package com.diamante.order.exception;

public class AssistantUnavailableException extends RuntimeException {

    public AssistantUnavailableException(Throwable cause) {
        super("Assistant is currently unavailable", cause);
    }
}
