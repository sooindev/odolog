package com.odolog.app.common.config.jpa;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * BaseTimeEntity 의 시간 필드를 채우는 스위치
 * OdoLogApplication 에 두면 WebMvcTest 전멸 — JPA 없이 메타모델을 요구해서
 * 대신 DataJpaTest 가 이 클래스를 안 집어 가므로 리포지토리 테스트에 Import 필요
 * 빠뜨리면 created_at null → NOT NULL 위반이라 조용히 넘어가지는 않음
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
