package com.odolog.app;

import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 마이그레이션(db/migration)을 깨끗한 스키마에 돌리고 Hibernate 가 엔티티와 맞는지 확인(ddl-auto: validate)
 * validate 가 안 보는 nullable·유니크 제약은 SchemaDrift 가 대조
 * 다른 테스트는 create-drop 이라 마이그레이션 파일이 틀려도 통과한다. 여기가 그 구멍을 막는다
 * 엔티티를 바꾸고 마이그레이션을 안 더하면 이 테스트가 기동 단계에서 실패
 * 전용 스키마. clean 이 다른 테스트·띄워 둔 E2E 백엔드의 odolog_test 표를 지우지 않게
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:mariadb://localhost:3306/odolog_migration_test",
        "spring.flyway.enabled=true",
        "spring.flyway.clean-disabled=false",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.sql.init.mode=never"
})
class FlywayMigrationTest {

    @TestConfiguration
    static class CleanFirst {

        /** 매번 빈 스키마에서. 앞선 실행이 남긴 표와 섞이지 않게 */
        @Bean
        FlywayMigrationStrategy cleanThenMigrate() {
            return flyway -> {
                flyway.clean();
                flyway.migrate();
            };
        }
    }

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    @DisplayName("새 DB: V1 부터 전부 적용되고 엔티티와 맞는다(기동 = validate 통과)")
    void freshDatabaseMatchesEntities() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        Integer applied = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where success = 1", Integer.class);
        assertThat(applied).isGreaterThanOrEqualTo(3);
        // 세션 표도 마이그레이션이 만듦
        assertThat(jdbc.queryForObject("select count(*) from SPRING_SESSION", Integer.class)).isZero();
    }

    @Test
    @DisplayName("운영 DB 경로: ddl-auto 로 만든 스키마(값 목록 CHECK 포함)를 V1 기준점으로 표시하면 V2 부터 돌아 CHECK 가 사라진다")
    void existingDatabaseIsBaselinedAndUpgraded() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Flyway clean = Flyway.configure().dataSource(dataSource).cleanDisabled(false).load();
        clean.clean();

        // 운영과 같은 상태 만들기: V1 의 표 + ddl-auto 가 붙였던 CHECK, 기록 표는 없음
        Flyway.configure().dataSource(dataSource).target("1").load().migrate();
        jdbc.execute("drop table flyway_schema_history");
        // ddl-auto 가 만든 모양 그대로 컬럼 단위. 이름 붙은 표 단위 CHECK 와 달리 DROP CONSTRAINT 로 안 지워짐
        jdbc.execute("alter table service_intervals modify column `type` varchar(30) not null"
                + " check (`type` in ('ENGINE_OIL','OTHER'))");

        MigrateResult result = Flyway.configure().dataSource(dataSource)
                .baselineOnMigrate(true).baselineVersion("1").load().migrate();

        assertThat(result.migrationsExecuted).isEqualTo(3);
        String ddl = jdbc.queryForObject("show create table service_intervals",
                (rs, row) -> rs.getString(2));
        assertThat(ddl).doesNotContainIgnoringCase("check");
    }

    @Test
    @DisplayName("마이그레이션한 스키마의 nullable·유니크 제약이 엔티티와 같다")
    void migratedSchemaMatchesNullabilityAndUniqueConstraints() throws Exception {
        assertThat(SchemaDrift.find(entityManagerFactory, dataSource)).isEmpty();
    }

    @Test
    @DisplayName("nullable 이어야 할 컬럼이 NOT NULL 이면 고칠 SQL 까지 찍어 준다")
    void detectsNotNullDrift() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        // 운영 DB 에서 실제로 있었던 상태 재현
        jdbc.execute("ALTER TABLE fuel_records MODIFY COLUMN liters DECIMAL(6,2) NOT NULL");

        try {
            List<String> drifts = SchemaDrift.find(entityManagerFactory, dataSource);

            assertThat(drifts).hasSize(1);
            assertThat(drifts.get(0))
                    .contains("fuel_records.liters")
                    .contains("엔티티는 nullable, DB 는 NOT NULL")
                    .contains("ALTER TABLE fuel_records MODIFY COLUMN liters decimal(6,2) NULL;");
        } finally {
            jdbc.execute("ALTER TABLE fuel_records MODIFY COLUMN liters DECIMAL(6,2) NULL");
        }
    }

    @Test
    @DisplayName("반대 방향도 잡는다 — NOT NULL 이어야 할 컬럼이 nullable 인 경우")
    void detectsNullableDrift() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("ALTER TABLE fuel_records MODIFY COLUMN odometer INT NULL");

        try {
            assertThat(SchemaDrift.find(entityManagerFactory, dataSource))
                    .anySatisfy(drift -> assertThat(drift)
                            .contains("fuel_records.odometer")
                            .contains("엔티티는 NOT NULL, DB 는 nullable"));
        } finally {
            jdbc.execute("ALTER TABLE fuel_records MODIFY COLUMN odometer INT NOT NULL");
        }
    }

    @Test
    @DisplayName("엔티티의 유니크 제약이 DB 에 없으면 잡는다")
    void detectsMissingUniqueConstraint() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("ALTER TABLE vehicles DROP INDEX uk_vehicles_public_id");

        try {
            assertThat(SchemaDrift.find(entityManagerFactory, dataSource))
                    .anySatisfy(drift -> assertThat(drift)
                            .contains("uk_vehicles_public_id")
                            .contains("ALTER TABLE vehicles ADD CONSTRAINT uk_vehicles_public_id UNIQUE (public_id);"));
        } finally {
            jdbc.execute("ALTER TABLE vehicles ADD CONSTRAINT uk_vehicles_public_id UNIQUE (public_id)");
        }
    }

    @Test
    @DisplayName("엔티티에 없는 유니크 제약이 DB 에 남아 있으면 잡는다 — 옛 규칙이 계속 막는 경우")
    void detectsLeftoverUniqueConstraint() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("ALTER TABLE vehicles ADD CONSTRAINT uk_vehicles_legacy UNIQUE (public_id, plate_number)");

        try {
            assertThat(SchemaDrift.find(entityManagerFactory, dataSource))
                    .anySatisfy(drift -> assertThat(drift)
                            .contains("uk_vehicles_legacy")
                            .contains("ALTER TABLE vehicles DROP INDEX uk_vehicles_legacy;"));
        } finally {
            jdbc.execute("ALTER TABLE vehicles DROP INDEX uk_vehicles_legacy");
        }
    }
}
