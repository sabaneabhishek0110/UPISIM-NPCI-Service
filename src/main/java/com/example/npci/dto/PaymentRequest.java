package com.example.npci.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    private String PayerVpa;
    private String PayeeVpa;
    private Double amount;
    private String pin;
    private String psp_txn_id;
}
