package com.diamante.order.dto;

import java.math.BigDecimal;

public record PaymentRequest(BigDecimal amount) {
}
