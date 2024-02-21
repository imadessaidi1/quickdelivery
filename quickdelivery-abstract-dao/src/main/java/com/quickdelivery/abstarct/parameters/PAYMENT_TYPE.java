package com.quickdelivery.abstarct.parameters;

public enum PAYMENT_TYPE {
    CREDIT_CARD("CREDIT_CARD"),
    BANK_ID("BANK_ID"),

    PAYPAL("PAYPAL");
    private String paymentType;

    PAYMENT_TYPE(String paymentType){
        this.paymentType = paymentType;
    }
}
