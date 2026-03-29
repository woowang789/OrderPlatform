package com.orderplatform.member.adapter.out.persistence;

import com.orderplatform.member.domain.model.Email;
import com.orderplatform.member.domain.model.Member;

/**
 * Domain Member ↔ MemberJpaEntity 양방향 변환
 */
public final class MemberMapper {

    private MemberMapper() {
    }

    public static MemberJpaEntity toJpaEntity(Member member) {
        return new MemberJpaEntity(
                member.getEmail().value(),
                member.getPassword(),
                member.getName()
        );
    }

    public static Member toDomain(MemberJpaEntity entity) {
        return Member.reconstitute(
                entity.getId(),
                new Email(entity.getEmail()),
                entity.getPassword(),
                entity.getName(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
