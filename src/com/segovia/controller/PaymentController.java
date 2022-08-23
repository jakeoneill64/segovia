package com.segovia.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {

    @PostMapping(value = "/callback")
    public void onCallback(){

    }


}
