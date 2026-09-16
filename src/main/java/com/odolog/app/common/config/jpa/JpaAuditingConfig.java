package com.odolog.app.common.config.jpa;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * BaseTimeEntity 의 @CreatedDate / @LastModifiedDate 를 실제로 채워 주는 스위치.
 *
 * <p><b>OdoLogApplication 에 붙였다가 되돌린 자리다.</b> 거기 두면 @WebMvcTest 가 전부 깨진다 —
 * 웹 계층만 띄우느라 JPA 가 없는데 @EnableJpaAuditing 은 엔티티 메타모델을 요구해서
 * {@code IllegalArgumentException: JPA metamodel must not be empty} 로 터진다.
 * 실제로 밟고 나서 옮겼다.
 *
 * <p>대신 @DataJpaTest 가 이 클래스를 자동으로 집어 가지 못한다(JPA 와 무관한 @Configuration 을
 * 전부 걸러낸다). 그래서 리포지토리 테스트는 {@code @Import(JpaAuditingConfig.class)} 를 직접 붙인다.
 * 빠뜨리면 created_at 이 null 인 채로 INSERT 되어 NOT NULL 위반으로 터지므로 조용히 넘어가지 않는다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
