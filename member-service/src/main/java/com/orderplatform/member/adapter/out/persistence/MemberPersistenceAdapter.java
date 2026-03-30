package com.orderplatform.member.adapter.out.persistence;

import com.orderplatform.member.application.port.out.LoadMemberPort;
import com.orderplatform.member.application.port.out.SaveMemberPort;
import com.orderplatform.member.domain.model.Email;
import com.orderplatform.member.domain.model.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@SuppressWarnings("null")
@Component
@RequiredArgsConstructor
public class MemberPersistenceAdapter implements LoadMemberPort, SaveMemberPort {

    private final MemberJpaRepository memberJpaRepository;

    @Override
    public Optional<Member> findById(Long id) {
        return memberJpaRepository.findById(id)
                .map(MemberMapper::toDomain);
    }

    @Override
    public Optional<Member> findByEmail(Email email) {
        return memberJpaRepository.findByEmail(email.value())
                .map(MemberMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return memberJpaRepository.existsByEmail(email.value());
    }

    @Override
    public Member save(Member member) {
        MemberJpaEntity entity = MemberMapper.toJpaEntity(member);
        MemberJpaEntity saved = memberJpaRepository.save(entity);
        return MemberMapper.toDomain(saved);
    }
}
