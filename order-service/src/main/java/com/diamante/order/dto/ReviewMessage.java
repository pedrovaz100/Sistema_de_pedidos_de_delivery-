package com.diamante.order.dto;

public record ReviewMessage(Long dishId, String dishName, int rating, String comment) {
}
