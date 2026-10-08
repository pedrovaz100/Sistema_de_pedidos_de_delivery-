package com.diamante.order;

import com.diamante.order.client.PaymentClient;
import com.diamante.order.dto.PaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringJUnitConfig(PaymentClientRetryTest.TestConfig.class)
@TestPropertySource(properties = {
        "payment.retry.max-retries=3",
        "payment.retry.delay=10",
        "payment.retry.multiplier=2.0",
        "payment.retry.jitter=5",
        "payment.retry.max-delay=40"
})
class PaymentClientRetryTest {

    private static final String PAYMENT_URL = "http://PAYMENT-SERVICE/payments";

    @Configuration
    @EnableResilientMethods
    static class TestConfig {

        @Bean
        RestTemplate restTemplate() {
            return new RestTemplate();
        }

        @Bean
        PaymentClient paymentClient(RestTemplate restTemplate) {
            return new PaymentClient(restTemplate);
        }
    }

    @Autowired
    private PaymentClient paymentClient;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        server = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void shouldApprovePaymentOnFirstAttempt() {
        server.expect(times(1), requestTo(PAYMENT_URL)).andExpect(method(POST))
                .andRespond(withSuccess("{\"status\":\"APPROVED\",\"instance\":8081}", APPLICATION_JSON));

        PaymentResponse response = paymentClient.charge(new BigDecimal("79.80"));

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.instance()).isEqualTo(8081);
        server.verify();
    }

    @Test
    void shouldRetryAfterFailuresUntilPaymentIsApproved() {
        server.expect(times(2), requestTo(PAYMENT_URL)).andRespond(withServerError());
        server.expect(times(1), requestTo(PAYMENT_URL))
                .andRespond(withSuccess("{\"status\":\"APPROVED\",\"instance\":8082}", APPLICATION_JSON));

        PaymentResponse response = paymentClient.charge(new BigDecimal("79.80"));

        assertThat(response.instance()).isEqualTo(8082);
        server.verify();
    }

    @Test
    void shouldGiveUpAfterAllAttemptsFail() {
        server.expect(times(4), requestTo(PAYMENT_URL)).andRespond(withServerError());

        assertThatThrownBy(() -> paymentClient.charge(new BigDecimal("79.80"))).isInstanceOf(RuntimeException.class);

        server.verify();
    }
}
