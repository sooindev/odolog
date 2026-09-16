package com.odolog.app.common.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 생성·수정 시각을 담당하는 부모.
 *
 * <p>엔티티가 4개째(FuelRecord)가 되면서 도입했다. 그전까지는 @PrePersist/@PreUpdate 를
 * 엔티티마다 복사해 뒀고, 3개까지는 "상속이 없어서 파일 하나만 열면 모든 필드가 보인다"는
 * 장점이 복사의 비용보다 컸다. 4개째에서 뒤집혔다 — 같은 8줄이 네 벌이 된다.
 *
 * <p><b>@MappedSuperclass 는 테이블을 만들지 않는다.</b> @Entity 와 달리 이 클래스 자체는
 * DB에 없고, 필드만 자식 테이블에 합쳐진다. 그래서 users·vehicles·maintenance_records 의
 * created_at / updated_at 컬럼은 전과 **똑같은 이름으로 같은 테이블에** 남는다 —
 * ddl-auto: update 가 컬럼을 새로 만들거나 지우지 않는다는 뜻이다.
 *
 * <p>@CreatedDate / @LastModifiedDate 는 JPA 가 아니라 <b>Spring Data</b> 의 것이라
 * AuditingEntityListener 가 붙어 있어야 동작하고, 그 리스너는 @EnableJpaAuditing 이
 * 켜져 있어야 등록된다(OdoLogApplication 에 있다).
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
