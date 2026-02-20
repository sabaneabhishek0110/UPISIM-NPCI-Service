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
    private Double amount;
    private String upi_txn_id;
    private String rrn;
    private String psp_txn_id;

}
