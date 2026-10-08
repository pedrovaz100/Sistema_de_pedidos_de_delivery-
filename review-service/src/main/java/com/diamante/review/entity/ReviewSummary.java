package com.diamante.review.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "review_summary")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSummary {

    @Id
    private Long dishId;

    @Column(nullable = false)
    private String dishName;

    @Column(nullable = false)
    private long ratingSum;

    @Column(nullable = false)
    private long ratingCount;

    public void accumulate(String dishName, long sum, long count) {
        this.dishName = dishName;
        this.ratingSum += sum;
        this.ratingCount += count;
    }

    public double average() {
        if (ratingCount == 0) {
            return 0.0;
        }
        return (double) ratingSum / ratingCount;
    }
}
