package com.orderplatform.member.domain.exception;

import com.orderplatform.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class MemberNotFoundException extends BusinessException {

    public MemberNotFoundException(Long memberId) {
        super("회원을 찾을 수 없습니다. ID: " + memberId, HttpStatus.NOT_FOUND);
    }
}
