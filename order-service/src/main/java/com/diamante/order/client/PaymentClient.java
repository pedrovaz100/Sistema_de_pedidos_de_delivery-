package com.diamante.order.client;

import com.diamante.order.dto.PaymentRequest;
import com.diamante.order.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentClient {

    private static final String PAYMENT_URL = "http://PAYMENT-SERVICE/payments";

    private final RestTemplate restTemplate;

    @Retryable(
            maxRetriesString = "${payment.retry.max-retries:5}",
            delayString = "${payment.retry.delay:200}",
            multiplierString = "${payment.retry.multiplier:2.0}",
            jitterString = "${payment.retry.jitter:100}",
            maxDelayString = "${payment.retry.max-delay:2000}")
    public PaymentResponse charge(BigDecimal amount) {
        try {
            PaymentResponse response = restTemplate.postForObject(PAYMENT_URL, new PaymentRequest(amount), PaymentResponse.class);
            if (response == null || !"APPROVED".equals(response.status())) {
                throw new IllegalStateException("Payment was not approved");
            }
            log.info("Payment of {} approved by payment-service instance {}", amount, response.instance());
            return response;
        } catch (RestClientException | IllegalStateException exception) {
            log.warn("Payment attempt for {} failed: {}", amount, exception.getMessage());
            throw exception;
        }
    }
}
