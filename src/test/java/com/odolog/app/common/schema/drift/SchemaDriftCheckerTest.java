package com.odolog.app.common.schema.drift;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 진짜 DB 가 있어야 하는 검사라 @SpringBootTest
 * 이 장치 자체가 "아무도 안 보는 사이 어긋나는 것"을 잡는 물건이라, 이게 조용히 고장 나면
 * 그때부터 아무것도 못 잡는다
 */
@SpringBootTest
class SchemaDriftCheckerTest {

    @Autowired
    private SchemaDriftChecker checker;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("스키마가 엔티티와 같으면 아무것도 보고하지 않는다")
    void noDriftOnFreshSchema() {
        // 테스트 스키마는 create-drop 이라 언제나 엔티티 그대로 만들어진다
        assertThat(checker.findDrifts()).isEmpty();
    }

    @Test
    @DisplayName("nullable 이어야 할 컬럼이 NOT NULL 이면 고칠 SQL 까지 찍어 준다")
    void detectsNotNullDrift() {
        // 운영 DB 가 실제로 빠져 있던 상태를 그대로 만든다 (2026-09-23)
        jdbcTemplate.execute("ALTER TABLE fuel_records MODIFY COLUMN liters DECIMAL(6,2) NOT NULL");

        try {
            List<String> drifts = checker.findDrifts();

            assertThat(drifts).hasSize(1);
            assertThat(drifts.get(0))
                    .contains("fuel_records.liters")
                    .contains("엔티티는 nullable, DB 는 NOT NULL")
                    // 읽고 나서 무엇을 해야 할지 찾아다니지 않게 명령까지 담는다
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
}
