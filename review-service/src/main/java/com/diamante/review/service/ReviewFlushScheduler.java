package com.diamante.review.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewFlushScheduler {

    private final ReviewBuffer reviewBuffer;
    private final ReviewSummaryService reviewSummaryService;

    @Scheduled(fixedRateString = "${review.flush-interval-ms:5000}")
    public void flush() {
        Map<Long, ReviewAccumulator> drained = reviewBuffer.drain();
        if (drained.isEmpty()) {
            return;
        }
        try {
            reviewSummaryService.persist(drained);
            log.info("Flushed review summaries for {} dishes", drained.size());
        } catch (RuntimeException exception) {
            log.error("Flush failed, restoring {} dishes to the buffer", drained.size(), exception);
            reviewBuffer.restore(drained);
        }
    }
}
