package com.diamante.payment;

import com.diamante.payment.dto.PaymentRequest;
import com.diamante.payment.dto.PaymentResponse;
import com.diamante.payment.exception.PaymentProcessingException;
import com.diamante.payment.service.PaymentService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentServiceTest {

    private static final PaymentRequest REQUEST = new PaymentRequest(new BigDecimal("79.80"));

    @Test
    void shouldApproveAndReportInstancePortWhenFailureRateIsZero() {
        PaymentService service = new PaymentService(0.0, 8081);

        PaymentResponse response = service.process(REQUEST);

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.instance()).isEqualTo(8081);
    }

    @Test
    void shouldFailWhenFailureRateIsOne() {
        PaymentService service = new PaymentService(1.0, 8082);

        assertThatThrownBy(() -> service.process(REQUEST))
                .isInstanceOf(PaymentProcessingException.class)
                .hasMessageContaining("8082");
    }

    @Test
    void shouldFailRoughlyHalfOfTheCallsWithDefaultRate() {
        PaymentService service = new PaymentService(0.5, 8081);
        int failures = 0;
        int attempts = 2000;

        for (int i = 0; i < attempts; i++) {
            try {
                service.process(REQUEST);
            } catch (PaymentProcessingException exception) {
                failures++;
            }
        }

        assertThat(failures).isBetween(attempts * 40 / 100, attempts * 60 / 100);
    }
}
