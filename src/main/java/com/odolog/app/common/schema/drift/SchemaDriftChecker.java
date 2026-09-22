package com.odolog.app.common.schema.drift;

import jakarta.persistence.Column;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
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
import java.util.List;
import java.util.Map;

/**
 * 엔티티가 말하는 nullable 과 실제 DB 컬럼을 기동할 때 한 번 대조한다
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
                ║ 아래를 한 번 실행하세요. 그대로 두면 저장할 때 500 이 납니다.
                ╚═══════════════════════════════════════════════════════════════════════
                {}
                """, drifts.size(), String.join(System.lineSeparator(), drifts));
    }

    private record ExpectedColumn(String name, boolean nullable) {
    }

    private record DbColumn(boolean nullable, String type) {
    }
}
