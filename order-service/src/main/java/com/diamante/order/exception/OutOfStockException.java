package com.diamante.order.exception;

public class OutOfStockException extends RuntimeException {

    public OutOfStockException() {
        super("Dish out of stock");
    }
}
