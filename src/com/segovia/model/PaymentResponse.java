package com.segovia.model;

import java.math.BigDecimal;

public record PaymentResponse(String id, String apiId, String status, BigDecimal fee, String details) {
}
