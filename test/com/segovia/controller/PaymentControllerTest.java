package com.segovia.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PaymentControllerTest {

    @Autowired
    private PaymentController paymentController;

    @Test
    public void test(){
        System.out.println("Assacas");
    }

}