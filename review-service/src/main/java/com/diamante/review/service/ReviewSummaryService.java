package com.diamante.review.service;

import com.diamante.review.dto.RankingEntry;
import com.diamante.review.entity.ReviewSummary;
import com.diamante.review.repository.ReviewSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewSummaryService {

    private final ReviewSummaryRepository repository;

    @Transactional
    public void persist(Map<Long, ReviewAccumulator> accumulators) {
        accumulators.forEach((dishId, accumulator) -> {
            ReviewSummary summary = repository.findById(dishId)
                    .orElseGet(() -> ReviewSummary.builder()
                            .dishId(dishId)
                            .dishName(accumulator.dishName())
                            .build());
            summary.accumulate(accumulator.dishName(), accumulator.ratingSum(), accumulator.ratingCount());
            repository.save(summary);
        });
    }

    @Transactional(readOnly = true)
    public List<RankingEntry> ranking() {
        return repository.findAll().stream()
                .sorted(Comparator.comparingDouble(ReviewSummary::average).reversed()
                        .thenComparing(Comparator.comparingLong(ReviewSummary::getRatingCount).reversed()))
                .map(summary -> new RankingEntry(
                        summary.getDishId(),
                        summary.getDishName(),
                        BigDecimal.valueOf(summary.average()).setScale(2, RoundingMode.HALF_UP).doubleValue(),
                        summary.getRatingCount()))
                .toList();
    }
}
