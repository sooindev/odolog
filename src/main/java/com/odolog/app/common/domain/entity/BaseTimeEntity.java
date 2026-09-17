package com.odolog.app.common.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 생성·수정 시각 담당 부모. 엔티티 4개째(FuelRecord)에 도입
 * MappedSuperclass 라 테이블을 만들지 않고 필드만 자식에 합쳐짐 — 컬럼 이름이 그대로라 ddl-auto 가 안 건드림
 * CreatedDate / LastModifiedDate 는 Spring Data 것이라 EnableJpaAuditing 이 켜져 있어야 동작
 * (스위치는 common/config/jpa/JpaAuditingConfig)
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
