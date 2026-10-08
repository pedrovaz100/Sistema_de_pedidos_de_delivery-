package com.diamante.payment.service;

import com.diamante.payment.dto.PaymentRequest;
import com.diamante.payment.dto.PaymentResponse;
import com.diamante.payment.exception.PaymentProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Random;

@Slf4j
@Service
public class PaymentService {

    private final Random random = new Random();
    private final double failureRate;
    private final int port;

    public PaymentService(
            @Value("${payment.failure-rate:0.5}") double failureRate,
            @Value("${server.port}") int port) {
        this.failureRate = failureRate;
        this.port = port;
    }

    public PaymentResponse process(PaymentRequest request) {
        if (random.nextDouble() < failureRate) {
            log.warn("Instance {} simulated a payment failure for amount {}", port, request.amount());
            throw new PaymentProcessingException("Payment processing failed on instance " + port, port);
        }
        log.info("Instance {} approved a payment of {}", port, request.amount());
        return new PaymentResponse("APPROVED", port);
    }
}
