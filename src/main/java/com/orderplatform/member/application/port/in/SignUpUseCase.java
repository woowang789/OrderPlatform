package com.orderplatform.member.application.port.in;

public interface SignUpUseCase {

    MemberInfo signUp(SignUpCommand command);
}
