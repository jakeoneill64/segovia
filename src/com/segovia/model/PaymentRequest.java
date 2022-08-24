package com.segovia.model;

import java.math.BigDecimal;

public record PaymentRequest(
    String msisdn,
    BigDecimal amount,  //better than double for currency values
    String currency,
    String reference,
    String url
) {}
