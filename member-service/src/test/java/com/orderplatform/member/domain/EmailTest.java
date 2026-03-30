package com.orderplatform.member.domain;

import com.orderplatform.member.domain.model.Email;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Email Value Object 단위 테스트 — 순수 Java (Spring 없음)
 */
class EmailTest {

    @Test
    void 이메일은_null이면_예외() {
        assertThatThrownBy(() -> new Email(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 이메일은_빈값이면_예외() {
        assertThatThrownBy(() -> new Email(""))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new Email("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 이메일은_잘못된_형식이면_예외() {
        assertThatThrownBy(() -> new Email("invalid"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new Email("@domain.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 이메일은_정상_생성() {
        Email email = new Email("test@example.com");

        assertThat(email.value()).isEqualTo("test@example.com");
    }

    @Test
    void 동일한_값의_이메일은_동등하다() {
        Email email1 = new Email("test@example.com");
        Email email2 = new Email("test@example.com");

        assertThat(email1).isEqualTo(email2);
    }
}
