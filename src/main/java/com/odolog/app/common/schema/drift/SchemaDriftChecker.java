package com.odolog.app.common.schema.drift;

import jakarta.persistence.Column;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.metamodel.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 기동 시 엔티티의 nullable·유니크 제약과 실제 DB 대조
 * ddl-auto: update 는 제약을 지우지 않고, validate 는 nullability 를 보지 않음
 * 경고만. 고칠 SQL 까지 출력
 */
@Component
public class SchemaDriftChecker {

    private static final Logger log = LoggerFactory.getLogger(SchemaDriftChecker.class);

    private static final String COLUMN_QUERY = """
            SELECT COLUMN_NAME, IS_NULLABLE, COLUMN_TYPE
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
            """;

    private static final String UNIQUE_QUERY = """
            SELECT CONSTRAINT_NAME
            FROM information_schema.TABLE_CONSTRAINTS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND CONSTRAINT_TYPE = 'UNIQUE'
            """;

    private final EntityManagerFactory entityManagerFactory;
    private final DataSource dataSource;

    public SchemaDriftChecker(EntityManagerFactory entityManagerFactory, DataSource dataSource) {
        this.entityManagerFactory = entityManagerFactory;
        this.dataSource = dataSource;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void check() {
        List<String> drifts = findDrifts();

        if (!drifts.isEmpty()) {
            report(drifts);
        }
    }

    /** 어긋난 항목 목록. 테스트가 로그 대신 이 결과를 직접 검사 */
    List<String> findDrifts() {
        List<String> drifts = new ArrayList<>();

        try (Connection connection = dataSource.getConnection()) {
            for (EntityType<?> entity : entityManagerFactory.getMetamodel().getEntities()) {
                String table = tableNameOf(entity.getJavaType());
                Map<String, DbColumn> actual = readColumns(connection, table);

                // 테이블이 없으면 ddl-auto 가 생성 예정이라 제외
                if (actual.isEmpty()) {
                    continue;
                }

                collectDrifts(entity.getJavaType(), table, actual, drifts);
                collectUniqueDrifts(entity.getJavaType(), table, readUniqueConstraints(connection, table), drifts);
            }
        } catch (Exception e) {
            // 점검 실패로 기동이 막히지 않게
            log.debug("스키마 대조를 건너뛴다", e);
            return List.of();
        }

        return drifts;
    }

    /** 엔티티와 DB 의 nullable 이 서로 다른 컬럼 */
    private void collectDrifts(Class<?> type, String table, Map<String, DbColumn> actual,
                               List<String> drifts) {

        for (Class<?> current = type; current != null && current != Object.class;
             current = current.getSuperclass()) {

            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }

                // @Column·@JoinColumn 이 있는 필드만 대상. 기본값 추측 시 오탐
                ExpectedColumn expected = expectedOf(field);
                if (expected == null) {
                    continue;
                }

                DbColumn column = actual.get(expected.name().toLowerCase());
                if (column == null) {
                    continue;
                }

                if (expected.nullable() != column.nullable()) {
                    drifts.add("  %s.%s — 엔티티는 %s, DB 는 %s%n      ALTER TABLE %s MODIFY COLUMN %s %s %s;"
                            .formatted(
                                    table, expected.name(),
                                    expected.nullable() ? "nullable" : "NOT NULL",
                                    column.nullable() ? "nullable" : "NOT NULL",
                                    table, expected.name(), column.type(),
                                    expected.nullable() ? "NULL" : "NOT NULL"));
                }
            }
        }
    }

    /**
     * 유니크 제약 이름 양방향 대조. DB 에 없음(생성 실패) / 엔티티에 없음(옛 제약 잔존)
     * 모든 유니크에 이름이 있다는 전제(규칙 6)
     */
    private void collectUniqueDrifts(Class<?> type, String table, Set<String> actual, List<String> drifts) {
        Map<String, UniqueConstraint> expected = new LinkedHashMap<>();
        Table annotation = type.getAnnotation(Table.class);
        if (annotation != null) {
            for (UniqueConstraint constraint : annotation.uniqueConstraints()) {
                expected.put(constraint.name().toLowerCase(), constraint);
            }
        }

        expected.forEach((name, constraint) -> {
            if (!actual.contains(name)) {
                drifts.add("  %s — 유니크 제약 %s 가 DB 에 없음(중복이 그대로 들어간다)%n      ALTER TABLE %s ADD CONSTRAINT %s UNIQUE (%s);"
                        .formatted(table, constraint.name(), table, constraint.name(),
                                String.join(", ", constraint.columnNames())));
            }
        });
        for (String name : actual) {
            if (!expected.containsKey(name)) {
                drifts.add("  %s — 엔티티에 없는 유니크 제약 %s 가 DB 에 남아 있음(옛 규칙이 계속 막는다)%n      ALTER TABLE %s DROP INDEX %s;"
                        .formatted(table, name, table, name));
            }
        }
    }

    private Set<String> readUniqueConstraints(Connection connection, String table) throws Exception {
        Set<String> names = new LinkedHashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(UNIQUE_QUERY)) {
            statement.setString(1, table);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    names.add(rows.getString("CONSTRAINT_NAME").toLowerCase());
                }
            }
        }
        return names;
    }

    private ExpectedColumn expectedOf(Field field) {
        JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
        if (joinColumn != null) {
            String name = joinColumn.name().isBlank() ? snake(field.getName()) : joinColumn.name();
            return new ExpectedColumn(name, joinColumn.nullable());
        }

        Column column = field.getAnnotation(Column.class);
        if (column != null) {
            String name = column.name().isBlank() ? snake(field.getName()) : column.name();
            return new ExpectedColumn(name, column.nullable());
        }

        return null;
    }

    private Map<String, DbColumn> readColumns(Connection connection, String table) throws Exception {
        Map<String, DbColumn> columns = new LinkedHashMap<>();

        try (PreparedStatement statement = connection.prepareStatement(COLUMN_QUERY)) {
            statement.setString(1, table);

            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    columns.put(rows.getString("COLUMN_NAME").toLowerCase(),
                            new DbColumn("YES".equals(rows.getString("IS_NULLABLE")),
                                    rows.getString("COLUMN_TYPE")));
                }
            }
        }

        return columns;
    }

    private String tableNameOf(Class<?> type) {
        Table table = type.getAnnotation(Table.class);
        return (table == null || table.name().isBlank()) ? snake(type.getSimpleName()) : table.name();
    }

    /** modelYear → model_year. @Column(name) 미지정 대비 */
    private String snake(String name) {
        return name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }

    /** SQL 로그에 묻히지 않게 한 덩어리로 출력 */
    private void report(List<String> drifts) {
        log.warn("""

                ╔═══════════════════════════════════════════════════════════════════════
                ║ 스키마가 엔티티와 어긋나 있습니다 ({}곳)
                ║ ddl-auto: update 는 제약을 추가만 하고 지우지 않습니다.
                ║ 아래를 한 번 실행하세요. 그대로 두면 저장이 500 이 나거나 중복이 그대로 들어갑니다.
                ╚═══════════════════════════════════════════════════════════════════════
                {}
                """, drifts.size(), String.join(System.lineSeparator(), drifts));
    }

    private record ExpectedColumn(String name, boolean nullable) {
    }

    private record DbColumn(boolean nullable, String type) {
    }
}
