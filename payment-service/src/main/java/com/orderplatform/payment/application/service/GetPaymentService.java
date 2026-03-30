package com.orderplatform.payment.application.service;

import com.orderplatform.payment.application.port.in.GetPaymentUseCase;
import com.orderplatform.payment.application.port.in.PaymentInfo;
import com.orderplatform.payment.application.port.out.LoadPaymentPort;
import com.orderplatform.payment.domain.exception.PaymentNotFoundException;
import com.orderplatform.payment.domain.model.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 결제 조회 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPaymentService implements GetPaymentUseCase {

    private final LoadPaymentPort loadPaymentPort;

    @Override
    public PaymentInfo getPayment(Long memberId, UUID paymentId) {
        Payment payment = loadPaymentPort.findByIdAndMemberId(paymentId, memberId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        return PaymentInfo.from(payment);
    }
}
