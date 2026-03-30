package com.orderplatform.member.application.service;

import com.orderplatform.member.application.port.in.GetMemberUseCase;
import com.orderplatform.member.application.port.in.MemberInfo;
import com.orderplatform.member.application.port.out.LoadMemberPort;
import com.orderplatform.member.domain.exception.MemberNotFoundException;
import com.orderplatform.member.domain.model.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMemberService implements GetMemberUseCase {

    private final LoadMemberPort loadMemberPort;

    @Override
    public MemberInfo getMember(Long memberId) {
        Member member = loadMemberPort.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));

        return new MemberInfo(
                member.getId(),
                member.getEmail().value(),
                member.getName(),
                member.getCreatedAt()
        );
    }
}
