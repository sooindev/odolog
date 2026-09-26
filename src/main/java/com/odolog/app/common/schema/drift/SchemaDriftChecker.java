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
 * 엔티티가 말하는 nullable·유니크 제약과 실제 DB 를 기동할 때 한 번 대조한다
 *
 * 왜 필요한가: ddl-auto: update 는 제약을 추가만 하고 지우지 않는다. 컬럼을 nullable 로
 * 바꿔도 이미 NOT NULL 인 컬럼은 그대로 남고, 그 사실이 아무 데도 드러나지 않다가
 * 저장하는 순간 500 으로 나타난다. 이 저장소는 같은 함정을 세 번 밟았다
 * (번호판 유니크 · 정비 종류 enum · 주유량 NOT NULL).
 *
 * 왜 ddl-auto: validate 로는 안 되는가: Hibernate 의 스키마 검증은 테이블과 컬럼의
 * 존재와 타입만 본다. nullability 는 보지 않는다 — 실제로 어긋난 스키마에 validate 를
 * 걸어도 앱이 그냥 뜨는 것을 확인했다
 *
 * 막지 않고 경고만 한다. 개발 중에 레거시 컬럼 하나로 앱이 안 뜨면 그게 더 큰 방해다.
 * 대신 고칠 SQL 을 그대로 찍어 준다 — 읽고 나서 무엇을 해야 할지 찾아다니지 않게
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

    /**
     * 어긋난 컬럼 목록. 로그로 찍는 것과 나눠 둔 이유는 테스트가 이 결과를 그대로 보기 위해서다 —
     * 로그를 가로채 문자열을 뒤지면 문구를 고칠 때마다 테스트가 깨진다
     */
    List<String> findDrifts() {
        List<String> drifts = new ArrayList<>();

        try (Connection connection = dataSource.getConnection()) {
            for (EntityType<?> entity : entityManagerFactory.getMetamodel().getEntities()) {
                String table = tableNameOf(entity.getJavaType());
                Map<String, DbColumn> actual = readColumns(connection, table);

                // 테이블이 통째로 없으면 ddl-auto 가 곧 만든다. 대조할 대상이 아니다
                if (actual.isEmpty()) {
                    continue;
                }

                collectDrifts(entity.getJavaType(), table, actual, drifts);
                collectUniqueDrifts(entity.getJavaType(), table, readUniqueConstraints(connection, table), drifts);
            }
        } catch (Exception e) {
            // 이 검사 때문에 앱이 못 뜨면 본말전도다
            log.debug("스키마 대조를 건너뛴다", e);
            return List.of();
        }

        return drifts;
    }

    /** 엔티티가 nullable 이라는데 DB 가 NOT NULL 이거나, 그 반대인 컬럼 */
    private void collectDrifts(Class<?> type, String table, Map<String, DbColumn> actual,
                               List<String> drifts) {

        for (Class<?> current = type; current != null && current != Object.class;
             current = current.getSuperclass()) {

            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }

                // @Column · @JoinColumn 이 붙은 필드만 본다. 애노테이션이 없으면 기본값을
                // 추측해야 하는데, 기본형(int)에 Hibernate 가 NOT NULL 을 붙이는 등 예외가 많아
                // 없는 어긋남을 보고하게 된다
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
     * 유니크 제약을 이름으로 대조. 두 방향 다 이 저장소가 실제로 밟았거나 밟을 뻔한 함정이다 —
     * DB 에 없음: 제약 생성이 조용히 실패(공개 id) / 엔티티에 없음: 옛 제약이 남음(번호판 유니크)
     * 이름으로만 보는 이유: 모든 유니크 제약에 이름을 붙인다(규칙 6). 이름 없는 것이 생기면
     * Hibernate 가 해시 이름을 붙여 "엔티티에 없음" 으로 잘못 보고된다
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

    /** modelYear → model_year. 이 저장소는 @Column(name=...) 을 다 적지만 기본값도 받아 둔다 */
    private String snake(String name) {
        return name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }

    /** SQL 로그 사이에 묻히지 않게 한 덩어리로 */
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
