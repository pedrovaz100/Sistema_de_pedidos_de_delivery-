package com.diamante.order.exception;

public class DishNotFoundException extends RuntimeException {

    public DishNotFoundException() {
        super("Dish not found");
    }
}
