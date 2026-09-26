package com.odolog.app.common.schema.drift;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 실제 DB 가 필요한 검사라 @SpringBootTest */
@SpringBootTest
class SchemaDriftCheckerTest {

    @Autowired
    private SchemaDriftChecker checker;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("스키마가 엔티티와 같으면 아무것도 보고하지 않는다")
    void noDriftOnFreshSchema() {
        // 테스트 스키마는 create-drop 이라 엔티티와 동일
        assertThat(checker.findDrifts()).isEmpty();
    }

    @Test
    @DisplayName("nullable 이어야 할 컬럼이 NOT NULL 이면 고칠 SQL 까지 찍어 준다")
    void detectsNotNullDrift() {
        // 운영 DB 에서 실제로 있었던 상태 재현
        jdbcTemplate.execute("ALTER TABLE fuel_records MODIFY COLUMN liters DECIMAL(6,2) NOT NULL");

        try {
            List<String> drifts = checker.findDrifts();

            assertThat(drifts).hasSize(1);
            assertThat(drifts.get(0))
                    .contains("fuel_records.liters")
                    .contains("엔티티는 nullable, DB 는 NOT NULL")
                    // 고칠 SQL 포함
                    .contains("ALTER TABLE fuel_records MODIFY COLUMN liters decimal(6,2) NULL;");
        } finally {
            jdbcTemplate.execute("ALTER TABLE fuel_records MODIFY COLUMN liters DECIMAL(6,2) NULL");
        }
    }

    @Test
    @DisplayName("반대 방향도 잡는다 — NOT NULL 이어야 할 컬럼이 nullable 인 경우")
    void detectsNullableDrift() {
        jdbcTemplate.execute("ALTER TABLE fuel_records MODIFY COLUMN odometer INT NULL");

        try {
            assertThat(checker.findDrifts())
                    .anySatisfy(drift -> assertThat(drift)
                            .contains("fuel_records.odometer")
                            .contains("엔티티는 NOT NULL, DB 는 nullable"));
        } finally {
            jdbcTemplate.execute("ALTER TABLE fuel_records MODIFY COLUMN odometer INT NOT NULL");
        }
    }

    @Test
    @DisplayName("엔티티의 유니크 제약이 DB 에 없으면 잡는다 — 제약 생성이 조용히 실패한 경우")
    void detectsMissingUniqueConstraint() {
        // SQL 없이 공개 id 를 띄운 경우 재현. 유니크 생성 실패 후 기동
        jdbcTemplate.execute("ALTER TABLE vehicles DROP INDEX uk_vehicles_public_id");

        try {
            assertThat(checker.findDrifts())
                    .anySatisfy(drift -> assertThat(drift)
                            .contains("uk_vehicles_public_id")
                            .contains("ALTER TABLE vehicles ADD CONSTRAINT uk_vehicles_public_id UNIQUE (public_id);"));
        } finally {
            jdbcTemplate.execute("ALTER TABLE vehicles ADD CONSTRAINT uk_vehicles_public_id UNIQUE (public_id)");
        }
    }

    @Test
    @DisplayName("엔티티에 없는 유니크 제약이 DB 에 남아 있으면 잡는다 — 옛 규칙이 계속 막는 경우")
    void detectsLeftoverUniqueConstraint() {
        // 옛 유니크 제약이 남은 경우 재현. 데이터와 무관하게 이미 유일한 조합에 설정
        jdbcTemplate.execute("ALTER TABLE vehicles ADD CONSTRAINT uk_vehicles_legacy UNIQUE (public_id, plate_number)");

        try {
            assertThat(checker.findDrifts())
                    .anySatisfy(drift -> assertThat(drift)
                            .contains("uk_vehicles_legacy")
                            .contains("ALTER TABLE vehicles DROP INDEX uk_vehicles_legacy;"));
        } finally {
            jdbcTemplate.execute("ALTER TABLE vehicles DROP INDEX uk_vehicles_legacy");
        }
    }
}
