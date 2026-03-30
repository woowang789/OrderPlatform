package com.orderplatform.member.adapter;

import com.orderplatform.common.AbstractIntegrationTest;
import com.orderplatform.member.adapter.out.persistence.MemberPersistenceAdapter;
import com.orderplatform.member.domain.model.Email;
import com.orderplatform.member.domain.model.Member;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MemberPersistenceAdapterTest extends AbstractIntegrationTest {

    @Autowired
    MemberPersistenceAdapter adapter;

    @Test
    void save_후_findById_조회() {
        Member member = Member.create(new Email("adapter-test@example.com"), "encodedPw", "테스트");

        Member saved = adapter.save(member);
        Optional<Member> found = adapter.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("테스트");
        assertThat(found.get().getEmail().value()).isEqualTo("adapter-test@example.com");
    }

    @Test
    void findByEmail_조회() {
        Email email = new Email("find-email@example.com");
        adapter.save(Member.create(email, "pw", "이메일테스트"));

        Optional<Member> found = adapter.findByEmail(email);

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("이메일테스트");
    }

    @Test
    void existsByEmail_존재하면_true() {
        Email email = new Email("exists@example.com");
        adapter.save(Member.create(email, "pw", "존재테스트"));

        assertThat(adapter.existsByEmail(email)).isTrue();
        assertThat(adapter.existsByEmail(new Email("notexists@example.com"))).isFalse();
    }
}
