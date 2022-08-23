package com.segovia.model;

import java.math.BigDecimal;

public record PaymentRequest(
    String id,
    String recipient,
    BigDecimal amount,  //better than double for currency values
    String currency
) {}
