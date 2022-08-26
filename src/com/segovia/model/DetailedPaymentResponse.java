package com.segovia.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class DetailedPaymentResponse extends BasicPaymentResponse{

    private String timestamp;
    private String customerReference;
    private BigDecimal fee;

    public DetailedPaymentResponse(String customerReference, long status, String message){
        this.customerReference = customerReference;
        super.setStatus(status);
        super.setMessage(message);
    }

    //in case they send a string...
    public void setFee(String fee){
        this.fee = new BigDecimal(fee);
    }


    //this is an alias to customerId in the base class for jackson
    public void setReference(String reference){
        super.setConversationID(reference);
    }

    public String getReference(){
        return super.getConversationID();
    }







}
