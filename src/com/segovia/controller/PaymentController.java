package com.segovia.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {



    @PostMapping(value = "/callback", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void onCallback(String body){
        System.out.println(body);
    }


}
