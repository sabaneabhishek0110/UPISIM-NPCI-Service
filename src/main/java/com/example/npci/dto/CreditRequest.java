package com.example.npci.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreditRequest {
    private String PayeeVpa;
    private String PayerVpa;
    private Double amount;
    private String upiTxnId;
    private String rrn;
    private String pspTxnId;
}
