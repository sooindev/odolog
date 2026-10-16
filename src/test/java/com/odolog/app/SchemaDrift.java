package com.odolog.app;

import jakarta.persistence.Column;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.metamodel.EntityType;

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
import javax.sql.DataSource;

/**
 * 엔티티의 nullable·유니크 제약·문자열 길이·정렬 규칙·외래키·이름 붙은 인덱스와 실제 DB 대조. 마이그레이션 검증용
 * Hibernate validate 는 이것들을 보지 않음. 어긋난 곳마다 고칠 SQL 포함
 */
final class SchemaDrift {

    private static final String COLUMN_QUERY = """
            SELECT COLUMN_NAME, IS_NULLABLE, COLUMN_TYPE, CHARACTER_MAXIMUM_LENGTH, COLLATION_NAME
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
            """;

    private static final String UNIQUE_QUERY = """
            SELECT CONSTRAINT_NAME
            FROM information_schema.TABLE_CONSTRAINTS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND CONSTRAINT_TYPE = 'UNIQUE'
            """;

    private static final String FOREIGN_KEY_QUERY = """
            SELECT CONSTRAINT_NAME
            FROM information_schema.TABLE_CONSTRAINTS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND CONSTRAINT_TYPE = 'FOREIGN KEY'
            """;

    private static final String INDEX_QUERY = """
            SELECT DISTINCT INDEX_NAME
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
            """;

    private SchemaDrift() {
    }

    /** 어긋난 항목 목록. 비어 있으면 일치 */
    static List<String> find(EntityManagerFactory entityManagerFactory, DataSource dataSource)
            throws Exception {
        List<String> drifts = new ArrayList<>();

        try (Connection connection = dataSource.getConnection()) {
            for (EntityType<?> entity : entityManagerFactory.getMetamodel().getEntities()) {
                String table = tableNameOf(entity.getJavaType());
                Map<String, DbColumn> actual = readColumns(connection, table);

                if (actual.isEmpty()) {
                    drifts.add("  %s — 테이블이 DB 에 없음".formatted(table));
                    continue;
                }

                collectDrifts(entity.getJavaType(), table, actual, drifts);
                collectUniqueDrifts(entity.getJavaType(), table, readNames(connection, UNIQUE_QUERY, table), drifts);
                collectForeignKeyDrifts(entity.getJavaType(), table, readNames(connection, FOREIGN_KEY_QUERY, table), drifts);
                collectIndexDrifts(entity.getJavaType(), table, readNames(connection, INDEX_QUERY, table), drifts);
            }
        }

        return drifts;
    }

    /** 엔티티와 DB 의 nullable 이 서로 다른 컬럼 */
    private static void collectDrifts(Class<?> type, String table, Map<String, DbColumn> actual,
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

                // 문자열 길이. 엔티티보다 짧으면 저장이 잘려 실패, 길면 검증과 DB 가 다른 상한
                if (expected.length() != null && column.length() != null
                        && !expected.length().equals(column.length())) {
                    drifts.add("  %s.%s — 길이가 엔티티는 %d, DB 는 %d"
                            .formatted(table, expected.name(), expected.length(), column.length()));
                }

                // 정렬 규칙을 columnDefinition 에 적은 컬럼만(공개 id 의 대소문자 구분)
                if (expected.collation() != null && !expected.collation().equalsIgnoreCase(column.collation())) {
                    drifts.add("  %s.%s — 정렬 규칙이 엔티티는 %s, DB 는 %s"
                            .formatted(table, expected.name(), expected.collation(), column.collation()));
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
    private static void collectUniqueDrifts(Class<?> type, String table, Set<String> actual, List<String> drifts) {
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

    /** 외래키 이름 양방향 대조. 이름은 @JoinColumn(foreignKey) 에서(규칙 6) */
    private static void collectForeignKeyDrifts(Class<?> type, String table, Set<String> actual, List<String> drifts) {
        Set<String> expected = new LinkedHashSet<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
                if (joinColumn != null && !joinColumn.foreignKey().name().isBlank()) {
                    expected.add(joinColumn.foreignKey().name().toLowerCase());
                }
            }
        }

        for (String name : expected) {
            if (!actual.contains(name)) {
                drifts.add("  %s — 외래키 %s 가 DB 에 없음(지운 부모를 가리키는 행이 남는다)".formatted(table, name));
            }
        }
        for (String name : actual) {
            if (!expected.contains(name)) {
                drifts.add("  %s — 엔티티에 없는 외래키 %s 가 DB 에 남아 있음".formatted(table, name));
            }
        }
    }

    /** @Table(indexes) 에 적은 이름 붙은 인덱스가 DB 에 있는지. 유니크·외래키가 만든 인덱스는 위에서 봄 */
    private static void collectIndexDrifts(Class<?> type, String table, Set<String> actual, List<String> drifts) {
        Table annotation = type.getAnnotation(Table.class);
        if (annotation == null) {
            return;
        }
        for (Index index : annotation.indexes()) {
            if (!actual.contains(index.name().toLowerCase())) {
                drifts.add("  %s — 인덱스 %s 가 DB 에 없음%n      CREATE INDEX %s ON %s (%s);"
                        .formatted(table, index.name(), index.name(), table, index.columnList()));
            }
        }
    }

    private static Set<String> readNames(Connection connection, String query, String table) throws Exception {
        Set<String> names = new LinkedHashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, table);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    names.add(rows.getString(1).toLowerCase());
                }
            }
        }
        return names;
    }

    private static ExpectedColumn expectedOf(Field field) {
        JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
        if (joinColumn != null) {
            String name = joinColumn.name().isBlank() ? snake(field.getName()) : joinColumn.name();
            return new ExpectedColumn(name, joinColumn.nullable(), null, null);
        }

        Column column = field.getAnnotation(Column.class);
        if (column != null) {
            String name = column.name().isBlank() ? snake(field.getName()) : column.name();
            // 길이는 문자열만. 다른 타입의 length 는 기본값(255)이라 의미 없음
            Integer length = field.getType() == String.class ? column.length() : null;
            return new ExpectedColumn(name, column.nullable(), length, collationOf(column.columnDefinition()));
        }

        return null;
    }

    private static Map<String, DbColumn> readColumns(Connection connection, String table) throws Exception {
        Map<String, DbColumn> columns = new LinkedHashMap<>();

        try (PreparedStatement statement = connection.prepareStatement(COLUMN_QUERY)) {
            statement.setString(1, table);

            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    long maxLength = rows.getLong("CHARACTER_MAXIMUM_LENGTH");
                    columns.put(rows.getString("COLUMN_NAME").toLowerCase(),
                            new DbColumn("YES".equals(rows.getString("IS_NULLABLE")),
                                    rows.getString("COLUMN_TYPE"),
                                    rows.wasNull() ? null : (int) maxLength,
                                    rows.getString("COLLATION_NAME")));
                }
            }
        }

        return columns;
    }

    private static String tableNameOf(Class<?> type) {
        Table table = type.getAnnotation(Table.class);
        return (table == null || table.name().isBlank()) ? snake(type.getSimpleName()) : table.name();
    }

    /** modelYear → model_year. @Column(name) 미지정 대비 */
    private static String snake(String name) {
        return name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }

    /** columnDefinition 의 "collate X". 없으면 null */
    private static String collationOf(String columnDefinition) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("collate\\s+(\\w+)", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(columnDefinition);
        return matcher.find() ? matcher.group(1) : null;
    }

    private record ExpectedColumn(String name, boolean nullable, Integer length, String collation) {
    }

    private record DbColumn(boolean nullable, String type, Integer length, String collation) {
    }
}
