package com.example.npci.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReversalRequest {
    private String upiTxnId;
    private String payerVpa;
    private String payeeVpa;
    private Double amount;
    private String rrn;
    private String pspTxnId;
}
