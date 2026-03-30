package com.orderplatform.member.application;

import com.orderplatform.member.application.port.in.LoginCommand;
import com.orderplatform.member.application.port.out.LoadMemberPort;
import com.orderplatform.member.application.port.out.PasswordEncoderPort;
import com.orderplatform.member.application.port.out.TokenGeneratorPort;
import com.orderplatform.member.application.service.LoginService;
import com.orderplatform.member.domain.exception.AuthenticationFailedException;
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
class LoginServiceTest {

    @Mock
    LoadMemberPort loadMemberPort;
    @Mock
    PasswordEncoderPort passwordEncoderPort;
    @Mock
    TokenGeneratorPort tokenGeneratorPort;
    @InjectMocks
    LoginService loginService;

    private final Email email = new Email("test@example.com");
    private final LocalDateTime now = LocalDateTime.now();
    private final Member member = Member.reconstitute(1L, email, "encodedPw", "홍길동", now, now);

    @Test
    void 정상_로그인_토큰_반환() {
        given(loadMemberPort.findByEmail(email)).willReturn(Optional.of(member));
        given(passwordEncoderPort.matches("password", "encodedPw")).willReturn(true);
        given(tokenGeneratorPort.generateToken(1L)).willReturn("jwt-token");

        String token = loginService.login(new LoginCommand("test@example.com", "password"));

        assertThat(token).isEqualTo("jwt-token");
    }

    @Test
    void 이메일_미존재시_예외() {
        given(loadMemberPort.findByEmail(email)).willReturn(Optional.empty());

        assertThatThrownBy(() -> loginService.login(new LoginCommand("test@example.com", "password")))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void 비밀번호_불일치시_예외() {
        given(loadMemberPort.findByEmail(email)).willReturn(Optional.of(member));
        given(passwordEncoderPort.matches("wrong", "encodedPw")).willReturn(false);

        assertThatThrownBy(() -> loginService.login(new LoginCommand("test@example.com", "wrong")))
                .isInstanceOf(AuthenticationFailedException.class);
    }
}
