package com.orderplatform.member.application.port.in;

public interface GetMemberUseCase {

    MemberInfo getMember(Long memberId);
}
