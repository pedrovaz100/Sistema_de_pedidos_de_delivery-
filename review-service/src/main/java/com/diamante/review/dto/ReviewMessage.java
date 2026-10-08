package com.diamante.review.dto;

public record ReviewMessage(Long dishId, String dishName, int rating, String comment) {
}
