package com.diamante.review;

import com.diamante.review.dto.ReviewMessage;
import com.diamante.review.entity.ReviewSummary;
import com.diamante.review.listener.ReviewListener;
import com.diamante.review.repository.ReviewSummaryRepository;
import com.diamante.review.service.ReviewBuffer;
import com.diamante.review.service.ReviewFlushScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.datasource.url=jdbc:h2:mem:review-test;DB_CLOSE_DELAY=-1",
        "review.flush-interval-ms=3600000"
})
class ReviewFlushAndRankingTest {

    @Autowired
    private ReviewListener reviewListener;

    @Autowired
    private ReviewBuffer reviewBuffer;

    @Autowired
    private ReviewFlushScheduler flushScheduler;

    @Autowired
    private ReviewSummaryRepository repository;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void clean() {
        reviewBuffer.drain();
        repository.deleteAll();
    }

    @Test
    void shouldKeepReviewsInBufferUntilFlushAndThenPersistAndClear() {
        reviewListener.receive(new ReviewMessage(1L, "House Burger", 5, "Great"));
        reviewListener.receive(new ReviewMessage(1L, "House Burger", 4, "Good"));

        assertThat(repository.count()).isZero();
        assertThat(reviewBuffer.size()).isEqualTo(1);

        flushScheduler.flush();

        ReviewSummary summary = repository.findById(1L).orElseThrow();
        assertThat(summary.getRatingSum()).isEqualTo(9);
        assertThat(summary.getRatingCount()).isEqualTo(2);
        assertThat(summary.getDishName()).isEqualTo("House Burger");
        assertThat(reviewBuffer.size()).isZero();
    }

    @Test
    void shouldAccumulateAcrossMultipleFlushes() {
        reviewListener.receive(new ReviewMessage(1L, "House Burger", 5, "Great"));
        flushScheduler.flush();
        reviewListener.receive(new ReviewMessage(1L, "House Burger", 3, "Ok"));
        flushScheduler.flush();

        ReviewSummary summary = repository.findById(1L).orElseThrow();
        assertThat(summary.getRatingSum()).isEqualTo(8);
        assertThat(summary.getRatingCount()).isEqualTo(2);
    }

    @Test
    void shouldReturnRankingOrderedByAverageReadFromDatabase() throws Exception {
        reviewListener.receive(new ReviewMessage(1L, "House Burger", 5, "a"));
        reviewListener.receive(new ReviewMessage(1L, "House Burger", 4, "b"));
        reviewListener.receive(new ReviewMessage(2L, "Caesar Salad", 5, "c"));
        reviewListener.receive(new ReviewMessage(2L, "Caesar Salad", 5, "d"));
        reviewListener.receive(new ReviewMessage(3L, "Vegan Buddha Bowl", 2, "e"));
        reviewListener.receive(new ReviewMessage(3L, "Vegan Buddha Bowl", 3, "f"));
        reviewListener.receive(new ReviewMessage(3L, "Vegan Buddha Bowl", 3, "g"));

        mockMvc.perform(get("/reviews/ranking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        flushScheduler.flush();

        mockMvc.perform(get("/reviews/ranking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].dishId").value(2))
                .andExpect(jsonPath("$[0].dishName").value("Caesar Salad"))
                .andExpect(jsonPath("$[0].average").value(5.0))
                .andExpect(jsonPath("$[0].count").value(2))
                .andExpect(jsonPath("$[1].dishId").value(1))
                .andExpect(jsonPath("$[1].average").value(4.5))
                .andExpect(jsonPath("$[1].count").value(2))
                .andExpect(jsonPath("$[2].dishId").value(3))
                .andExpect(jsonPath("$[2].average").value(2.67))
                .andExpect(jsonPath("$[2].count").value(3));
    }
}
