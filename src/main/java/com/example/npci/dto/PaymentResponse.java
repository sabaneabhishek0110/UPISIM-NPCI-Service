package com.example.npci.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private String payer_vpa;
    private String payee_vpa;
    private Double amount;
    private String psp_txn_id;
    private String upi_txn_id;
    private String rrn;
    private String status;
    private String response_code;
    private String failureReason;
}
