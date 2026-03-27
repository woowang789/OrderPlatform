package com.orderplatform.member.service;

import com.orderplatform.common.exception.AuthenticationFailedException;
import com.orderplatform.common.exception.DuplicateEmailException;
import com.orderplatform.common.exception.MemberNotFoundException;
import com.orderplatform.config.jwt.JwtTokenProvider;
import com.orderplatform.member.dto.LoginRequest;
import com.orderplatform.member.dto.LoginResponse;
import com.orderplatform.member.dto.MemberResponse;
import com.orderplatform.member.dto.SignupRequest;
import com.orderplatform.member.entity.Member;
import com.orderplatform.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public MemberResponse signup(SignupRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        Member member = new Member(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.name()
        );
        Member saved = memberRepository.save(member);
        return MemberResponse.from(saved);
    }

    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(AuthenticationFailedException::new);

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new AuthenticationFailedException();
        }

        String token = jwtTokenProvider.generateToken(member.getId());
        return new LoginResponse(token);
    }

    public MemberResponse getMyInfo(@NonNull Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));
        return MemberResponse.from(member);
    }
}
