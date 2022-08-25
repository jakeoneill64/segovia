package com.segovia.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;


@RestController
public class PaymentController {



    @PostMapping(value = "/callback", consumes = MediaType.ALL_VALUE)
    public void onCallback(HttpServletRequest request){

    }


}
