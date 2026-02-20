package com.example.npci.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DebitRequest {
    private String PayerVpa;
    private Double amount;
    private String pin;
    private String rrn;
    private String upi_txn_id;
    private String psp_txn_id;
}

