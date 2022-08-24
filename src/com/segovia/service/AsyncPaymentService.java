package com.segovia.service;

import com.segovia.model.ApiCredentials;
import com.segovia.model.PaymentRequest;
import com.segovia.model.PaymentResponse;

import java.util.concurrent.CompletableFuture;

public interface AsyncPaymentService {

    CompletableFuture<PaymentResponse> process(PaymentRequest request, ApiCredentials credentials);

}
