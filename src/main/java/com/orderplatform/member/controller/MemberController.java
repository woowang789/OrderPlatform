package com.orderplatform.member.controller;

import com.orderplatform.common.annotation.CurrentMemberId;
import com.orderplatform.member.dto.LoginRequest;
import com.orderplatform.member.dto.LoginResponse;
import com.orderplatform.member.dto.MemberResponse;
import com.orderplatform.member.dto.SignupRequest;
import com.orderplatform.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/signup")
    public ResponseEntity<MemberResponse> signup(@Valid @RequestBody SignupRequest request) {
        MemberResponse response = memberService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = memberService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<MemberResponse> getMyInfo(@CurrentMemberId Long memberId) {
        MemberResponse response = memberService.getMyInfo(memberId);
        return ResponseEntity.ok(response);
    }
}
