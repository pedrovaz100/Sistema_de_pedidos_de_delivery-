package com.diamante.review.service;

public record ReviewAccumulator(String dishName, long ratingSum, long ratingCount) {

    public static ReviewAccumulator of(String dishName, int rating) {
        return new ReviewAccumulator(dishName, rating, 1);
    }

    public ReviewAccumulator merge(ReviewAccumulator other) {
        return new ReviewAccumulator(other.dishName, ratingSum + other.ratingSum, ratingCount + other.ratingCount);
    }
}
