package com.orderplatform.member.domain;

import com.orderplatform.member.domain.model.Email;
import com.orderplatform.member.domain.model.Member;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Member 도메인 모델 단위 테스트 — 순수 Java (Spring 없음)
 */
class MemberDomainTest {

    @Test
    void create_정적팩토리_id와_timestamp_null() {
        Email email = new Email("test@example.com");

        Member member = Member.create(email, "encodedPw", "홍길동");

        assertThat(member.getId()).isNull();
        assertThat(member.getEmail()).isEqualTo(email);
        assertThat(member.getPassword()).isEqualTo("encodedPw");
        assertThat(member.getName()).isEqualTo("홍길동");
        assertThat(member.getCreatedAt()).isNull();
        assertThat(member.getUpdatedAt()).isNull();
    }

    @Test
    void reconstitute_모든_필드_복원() {
        Email email = new Email("test@example.com");
        LocalDateTime now = LocalDateTime.of(2026, 3, 30, 12, 0);

        Member member = Member.reconstitute(1L, email, "encodedPw", "홍길동", now, now);

        assertThat(member.getId()).isEqualTo(1L);
        assertThat(member.getEmail()).isEqualTo(email);
        assertThat(member.getPassword()).isEqualTo("encodedPw");
        assertThat(member.getName()).isEqualTo("홍길동");
        assertThat(member.getCreatedAt()).isEqualTo(now);
        assertThat(member.getUpdatedAt()).isEqualTo(now);
    }
}
