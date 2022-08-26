package com.segovia.controller;

import com.segovia.model.DetailedPaymentResponse;
import com.segovia.service.AsyncPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {

    @Autowired
    AsyncPaymentService paymentService;

    @PostMapping(value = "/callback", consumes = MediaType.ALL_VALUE)
    public void onCallback(DetailedPaymentResponse callbackResponse){
        paymentService.onResponseReceived(callbackResponse);
    }


}
