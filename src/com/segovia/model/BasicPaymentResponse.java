package com.segovia.model;

import lombok.Data;

@Data
public class BasicPaymentResponse {

    private String conversationID;
    private String message;
    private long status;



}
