package com.segovia.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DetailedPaymentResponse extends BasicPaymentResponse{

    private String timestamp;
    private String customerReference;


    //this is an alias to customerId in the base class
    public void setReference(String reference){
        super.conversationID = reference;
    }

    public String getReference(){
        return super.conversationID;
    }







}
