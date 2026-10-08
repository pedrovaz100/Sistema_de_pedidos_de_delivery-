package com.diamante.order;

import com.diamante.order.client.PaymentClient;
import com.diamante.order.dto.PaymentResponse;
import com.diamante.order.messaging.ReviewPublisher;
import com.diamante.order.ratelimit.SlidingWindowRateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "rate-limit.orders.enabled=true",
        "rate-limit.orders.permits-per-second=3"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RateLimitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentClient paymentClient;

    @MockitoBean
    private ReviewPublisher reviewPublisher;

    @MockitoBean
    private ChatModel chatModel;

    @Test
    void shouldReturnTooManyRequestsWhenOrdersExceedLimit() throws Exception {
        when(paymentClient.charge(any())).thenReturn(new PaymentResponse("APPROVED", 8081));
        String body = "{\"dishId\": 999999, \"quantity\": 1}";

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isNotFound());
        }

        mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldNotLimitOtherEndpoints() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/dishes")).andExpect(status().isOk());
        }
    }

    @Test
    void shouldAllowRequestsAgainAfterWindowPasses() {
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(2, 1000);
        long start = System.nanoTime();

        assertThat(limiter.tryAcquire(start)).isTrue();
        assertThat(limiter.tryAcquire(start + 100_000_000L)).isTrue();
        assertThat(limiter.tryAcquire(start + 200_000_000L)).isFalse();
        assertThat(limiter.tryAcquire(start + 1_050_000_000L)).isTrue();
    }
}
