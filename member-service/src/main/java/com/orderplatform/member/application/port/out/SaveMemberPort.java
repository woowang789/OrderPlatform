package com.orderplatform.member.application.port.out;

import com.orderplatform.member.domain.model.Member;

public interface SaveMemberPort {

    Member save(Member member);
}
