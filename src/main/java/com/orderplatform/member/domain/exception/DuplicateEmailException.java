package com.orderplatform.member.domain.exception;

import com.orderplatform.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class DuplicateEmailException extends BusinessException {

    public DuplicateEmailException(String email) {
        super("이미 사용 중인 이메일입니다: " + email, HttpStatus.CONFLICT);
    }
}
