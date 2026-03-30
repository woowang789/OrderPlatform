package com.orderplatform.member.application.service;

import com.orderplatform.member.application.port.in.LoginCommand;
import com.orderplatform.member.application.port.in.LoginUseCase;
import com.orderplatform.member.application.port.out.LoadMemberPort;
import com.orderplatform.member.application.port.out.PasswordEncoderPort;
import com.orderplatform.member.application.port.out.TokenGeneratorPort;
import com.orderplatform.member.domain.exception.AuthenticationFailedException;
import com.orderplatform.member.domain.model.Email;
import com.orderplatform.member.domain.model.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoginService implements LoginUseCase {

    private final LoadMemberPort loadMemberPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final TokenGeneratorPort tokenGeneratorPort;

    @Override
    public String login(LoginCommand command) {
        Email email = new Email(command.email());

        Member member = loadMemberPort.findByEmail(email)
                .orElseThrow(AuthenticationFailedException::new);

        if (!passwordEncoderPort.matches(command.password(), member.getPassword())) {
            throw new AuthenticationFailedException();
        }

        return tokenGeneratorPort.generateToken(member.getId());
    }
}
