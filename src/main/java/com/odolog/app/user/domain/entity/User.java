package com.odolog.app.user.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_users_email",
                columnNames = "email"
        )
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 30)
    private String nickname;

    @Column(length = 20)
    private String phone;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected User() {
    }

    public User(String email, String password, String nickname, String phone) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.phone = phone;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getNickname() {
        return nickname;
    }

    public String getPhone() {
        return phone;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public void changePhone(String phone) {
        this.phone = phone;
    }

    /**
     * 이미 암호화된 문자열만 받는다.
     *
     * 엔티티가 PasswordEncoder 를 들고 평문을 받아 직접 인코딩하게 만들 수도 있지만,
     * 그러면 도메인 객체가 스프링 시큐리티에 묶이고 new User(...) 하나 하는 데 빈이 필요해진다.
     * "무엇을 저장할지"는 엔티티가, "어떻게 암호화할지"는 서비스가 안다.
     */
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}
