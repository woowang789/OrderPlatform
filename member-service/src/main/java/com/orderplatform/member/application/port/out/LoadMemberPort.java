package com.orderplatform.member.application.port.out;

import com.orderplatform.member.domain.model.Email;
import com.orderplatform.member.domain.model.Member;

import java.util.Optional;

public interface LoadMemberPort {

    Optional<Member> findById(Long id);

    Optional<Member> findByEmail(Email email);

    boolean existsByEmail(Email email);
}
