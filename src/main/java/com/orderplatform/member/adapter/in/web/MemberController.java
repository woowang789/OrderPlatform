package com.orderplatform.member.adapter.in.web;

import com.orderplatform.common.annotation.CurrentMemberId;
import com.orderplatform.member.adapter.in.web.dto.LoginRequest;
import com.orderplatform.member.adapter.in.web.dto.LoginResponse;
import com.orderplatform.member.adapter.in.web.dto.MemberResponse;
import com.orderplatform.member.adapter.in.web.dto.SignupRequest;
import com.orderplatform.member.application.port.in.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final SignUpUseCase signUpUseCase;
    private final LoginUseCase loginUseCase;
    private final GetMemberUseCase getMemberUseCase;

    @PostMapping("/signup")
    public ResponseEntity<MemberResponse> signup(@Valid @RequestBody SignupRequest request) {
        SignUpCommand command = new SignUpCommand(request.email(), request.password(), request.name());
        MemberInfo info = signUpUseCase.signUp(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(MemberResponse.from(info));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginCommand command = new LoginCommand(request.email(), request.password());
        String accessToken = loginUseCase.login(command);
        return ResponseEntity.ok(new LoginResponse(accessToken));
    }

    @GetMapping("/me")
    public ResponseEntity<MemberResponse> getMyInfo(@CurrentMemberId @NonNull Long memberId) {
        MemberInfo info = getMemberUseCase.getMember(memberId);
        return ResponseEntity.ok(MemberResponse.from(info));
    }
}
