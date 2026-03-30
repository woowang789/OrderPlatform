package com.orderplatform.member.application;

import com.orderplatform.member.application.port.in.MemberInfo;
import com.orderplatform.member.application.port.in.SignUpCommand;
import com.orderplatform.member.application.port.out.LoadMemberPort;
import com.orderplatform.member.application.port.out.PasswordEncoderPort;
import com.orderplatform.member.application.port.out.SaveMemberPort;
import com.orderplatform.member.application.service.SignUpService;
import com.orderplatform.member.domain.exception.DuplicateEmailException;
import com.orderplatform.member.domain.model.Email;
import com.orderplatform.member.domain.model.Member;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SignUpServiceTest {

    @Mock
    LoadMemberPort loadMemberPort;
    @Mock
    SaveMemberPort saveMemberPort;
    @Mock
    PasswordEncoderPort passwordEncoderPort;
    @InjectMocks
    SignUpService signUpService;

    @Test
    void 정상_회원가입() {
        SignUpCommand command = new SignUpCommand("test@example.com", "password", "홍길동");
        Email email = new Email("test@example.com");
        LocalDateTime now = LocalDateTime.now();

        given(loadMemberPort.existsByEmail(email)).willReturn(false);
        given(passwordEncoderPort.encode("password")).willReturn("encodedPw");
        given(saveMemberPort.save(any(Member.class))).willReturn(
                Member.reconstitute(1L, email, "encodedPw", "홍길동", now, now)
        );

        MemberInfo result = signUpService.signUp(command);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.email()).isEqualTo("test@example.com");
        assertThat(result.name()).isEqualTo("홍길동");
        verify(saveMemberPort).save(any(Member.class));
    }

    @Test
    void 중복_이메일이면_예외() {
        SignUpCommand command = new SignUpCommand("dup@example.com", "password", "홍길동");
        Email email = new Email("dup@example.com");

        given(loadMemberPort.existsByEmail(email)).willReturn(true);

        assertThatThrownBy(() -> signUpService.signUp(command))
                .isInstanceOf(DuplicateEmailException.class);
    }
}
