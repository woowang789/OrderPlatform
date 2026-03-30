package com.orderplatform.payment.application.service;

import com.orderplatform.payment.application.port.in.CancelPaymentCommand;
import com.orderplatform.payment.application.port.in.CancelPaymentUseCase;
import com.orderplatform.payment.application.port.in.PaymentInfo;
import com.orderplatform.payment.application.port.out.LoadPaymentPort;
import com.orderplatform.payment.application.port.out.SavePaymentPort;
import com.orderplatform.payment.application.port.out.UpdateOrderStatusPort;
import com.orderplatform.payment.domain.exception.PaymentNotFoundException;
import com.orderplatform.payment.domain.model.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 취소 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CancelPaymentService implements CancelPaymentUseCase {

    private final LoadPaymentPort loadPaymentPort;
    private final SavePaymentPort savePaymentPort;
    private final UpdateOrderStatusPort updateOrderStatusPort;

    @Override
    @Transactional
    public PaymentInfo cancelPayment(CancelPaymentCommand command) {
        Payment payment = loadPaymentPort.findByIdAndMemberId(command.paymentId(), command.memberId())
                .orElseThrow(() -> new PaymentNotFoundException(command.paymentId()));

        payment.cancel();
        Payment saved = savePaymentPort.save(payment);

        updateOrderStatusPort.cancelOrder(command.memberId(), payment.getOrderId());

        return PaymentInfo.from(saved);
    }
}
