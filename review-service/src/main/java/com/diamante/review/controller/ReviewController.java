package com.diamante.review.controller;

import com.diamante.review.dto.RankingEntry;
import com.diamante.review.service.ReviewSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewSummaryService reviewSummaryService;

    @GetMapping("/ranking")
    public List<RankingEntry> ranking() {
        return reviewSummaryService.ranking();
    }
}
