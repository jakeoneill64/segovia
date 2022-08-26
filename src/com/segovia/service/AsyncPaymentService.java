package com.segovia.service;

import com.segovia.model.DetailedPaymentResponse;
import com.segovia.model.PaymentRequest;

public interface AsyncPaymentService {

    void process(PaymentRequest request);
    void onResponseReceived(DetailedPaymentResponse detailedResponse);

}
