package com.orderplatform.member.domain.exception;

import com.orderplatform.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class AuthenticationFailedException extends BusinessException {

    public AuthenticationFailedException() {
        super("이메일 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED);
    }
}
