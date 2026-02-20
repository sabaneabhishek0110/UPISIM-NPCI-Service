package com.example.npci.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreditResponse {
    private String status;
    private String responseCode;
    private String upi_txn_id;
    private String rrn;
    private String bank_txn_id;
    private String failureReason;

}
