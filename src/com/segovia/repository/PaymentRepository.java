package com.segovia.repository;

import com.segovia.model.DetailedPaymentResponse;

public interface PaymentRepository {

    void insert(DetailedPaymentResponse response);

}
