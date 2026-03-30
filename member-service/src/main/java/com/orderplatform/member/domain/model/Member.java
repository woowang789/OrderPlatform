package com.orderplatform.member.domain.model;

import java.time.LocalDateTime;

/**
 * Member 도메인 모델 — 순수 Java 객체 (JPA/Spring 의존 없음)
 */
public class Member {

    private final Long id;
    private final Email email;
    private final String password;
    private final String name;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private Member(Long id, Email email, String password, String name,
                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.name = name;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 신규 회원 생성 — id와 timestamp는 영속화 시 설정
     */
    public static Member create(Email email, String encodedPassword, String name) {
        return new Member(null, email, encodedPassword, name, null, null);
    }

    /**
     * DB에서 복원 — 모든 필드를 포함한 완전한 상태 복원
     */
    public static Member reconstitute(Long id, Email email, String password, String name,
                                      LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Member(id, email, password, name, createdAt, updatedAt);
    }

    public Long getId() {
        return id;
    }

    public Email getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
