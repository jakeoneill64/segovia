package com.segovia.service;

import com.segovia.model.ApiCredentials;
import com.segovia.model.PaymentRequest;

public interface AsyncPaymentService {

    void process(PaymentRequest request, ApiCredentials credentials);

}
