package com.orderplatform.member.application.service;

import com.orderplatform.member.application.port.in.MemberInfo;
import com.orderplatform.member.application.port.in.SignUpCommand;
import com.orderplatform.member.application.port.in.SignUpUseCase;
import com.orderplatform.member.application.port.out.LoadMemberPort;
import com.orderplatform.member.application.port.out.PasswordEncoderPort;
import com.orderplatform.member.application.port.out.SaveMemberPort;
import com.orderplatform.member.domain.exception.DuplicateEmailException;
import com.orderplatform.member.domain.model.Email;
import com.orderplatform.member.domain.model.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SignUpService implements SignUpUseCase {

    private final LoadMemberPort loadMemberPort;
    private final SaveMemberPort saveMemberPort;
    private final PasswordEncoderPort passwordEncoderPort;

    @Override
    public MemberInfo signUp(SignUpCommand command) {
        Email email = new Email(command.email());

        if (loadMemberPort.existsByEmail(email)) {
            throw new DuplicateEmailException(command.email());
        }

        String encodedPassword = passwordEncoderPort.encode(command.password());
        Member member = Member.create(email, encodedPassword, command.name());
        Member saved = saveMemberPort.save(member);

        return new MemberInfo(
                saved.getId(),
                saved.getEmail().value(),
                saved.getName(),
                saved.getCreatedAt()
        );
    }
}
