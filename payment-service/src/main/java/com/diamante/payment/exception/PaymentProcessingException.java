package com.diamante.payment.exception;

import lombok.Getter;

@Getter
public class PaymentProcessingException extends RuntimeException {

    private final int instance;

    public PaymentProcessingException(String message, int instance) {
        super(message);
        this.instance = instance;
    }
}
