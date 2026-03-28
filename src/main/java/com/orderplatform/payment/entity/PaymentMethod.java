package com.orderplatform.payment.entity;

/**
 * 결제 수단 열거형
 */
public enum PaymentMethod {

    CARD,              // 카드 결제
    BANK_TRANSFER,     // 계좌이체
    VIRTUAL_ACCOUNT    // 가상계좌
}
