package com.orderplatform.member.application;

import com.orderplatform.member.application.port.in.MemberInfo;
import com.orderplatform.member.application.port.out.LoadMemberPort;
import com.orderplatform.member.application.service.GetMemberService;
import com.orderplatform.member.domain.exception.MemberNotFoundException;
import com.orderplatform.member.domain.model.Email;
import com.orderplatform.member.domain.model.Member;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GetMemberServiceTest {

    @Mock
    LoadMemberPort loadMemberPort;
    @InjectMocks
    GetMemberService getMemberService;

    @Test
    void 정상_조회() {
        LocalDateTime now = LocalDateTime.now();
        Member member = Member.reconstitute(1L, new Email("test@example.com"), "pw", "홍길동", now, now);
        given(loadMemberPort.findById(1L)).willReturn(Optional.of(member));

        MemberInfo result = getMemberService.getMember(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.email()).isEqualTo("test@example.com");
        assertThat(result.name()).isEqualTo("홍길동");
    }

    @Test
    void 미존재시_예외() {
        given(loadMemberPort.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> getMemberService.getMember(999L))
                .isInstanceOf(MemberNotFoundException.class);
    }
}
