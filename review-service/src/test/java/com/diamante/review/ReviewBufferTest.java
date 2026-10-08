package com.diamante.review;

import com.diamante.review.dto.ReviewMessage;
import com.diamante.review.service.ReviewAccumulator;
import com.diamante.review.service.ReviewBuffer;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewBufferTest {

    @Test
    void shouldAccumulateRatingSumAndCountPerDish() {
        ReviewBuffer buffer = new ReviewBuffer();

        buffer.add(new ReviewMessage(1L, "House Burger", 5, "Great"));
        buffer.add(new ReviewMessage(1L, "House Burger", 4, "Good"));
        buffer.add(new ReviewMessage(2L, "Caesar Salad", 3, "Fine"));

        Map<Long, ReviewAccumulator> drained = buffer.drain();

        assertThat(drained).hasSize(2);
        assertThat(drained.get(1L).ratingSum()).isEqualTo(9);
        assertThat(drained.get(1L).ratingCount()).isEqualTo(2);
        assertThat(drained.get(2L).ratingSum()).isEqualTo(3);
        assertThat(drained.get(2L).ratingCount()).isEqualTo(1);
    }

    @Test
    void shouldBeEmptyAfterDrain() {
        ReviewBuffer buffer = new ReviewBuffer();
        buffer.add(new ReviewMessage(1L, "House Burger", 5, "Great"));

        buffer.drain();

        assertThat(buffer.size()).isZero();
        assertThat(buffer.drain()).isEmpty();
    }

    @Test
    void shouldRestoreDrainedEntriesWithoutLosingNewOnes() {
        ReviewBuffer buffer = new ReviewBuffer();
        buffer.add(new ReviewMessage(1L, "House Burger", 5, "Great"));
        Map<Long, ReviewAccumulator> drained = buffer.drain();
        buffer.add(new ReviewMessage(1L, "House Burger", 3, "Ok"));

        buffer.restore(drained);

        ReviewAccumulator merged = buffer.drain().get(1L);
        assertThat(merged.ratingSum()).isEqualTo(8);
        assertThat(merged.ratingCount()).isEqualTo(2);
    }

    @Test
    void shouldNotLoseReviewsUnderConcurrentWrites() throws Exception {
        ReviewBuffer buffer = new ReviewBuffer();
        int threads = 16;
        int perThread = 500;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch done = new CountDownLatch(threads);

        for (int t = 0; t < threads; t++) {
            executor.submit(() -> {
                for (int i = 0; i < perThread; i++) {
                    buffer.add(new ReviewMessage(1L, "House Burger", 4, "Concurrent"));
                }
                done.countDown();
            });
        }
        done.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        ReviewAccumulator accumulator = buffer.drain().get(1L);
        assertThat(accumulator.ratingCount()).isEqualTo((long) threads * perThread);
        assertThat(accumulator.ratingSum()).isEqualTo((long) threads * perThread * 4);
    }
}
