package com.diamante.order.exception;

public class PaymentFailedException extends RuntimeException {

    public PaymentFailedException(Throwable cause) {
        super("Payment could not be processed", cause);
    }
}
