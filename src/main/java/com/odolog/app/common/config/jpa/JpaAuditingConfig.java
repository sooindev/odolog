package com.odolog.app.common.config.jpa;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * BaseTimeEntity 시간 필드 채우기 스위치
 * OdoLogApplication 에 두면 @WebMvcTest 실패. 대신 @DataJpaTest 에는 @Import 필요
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
