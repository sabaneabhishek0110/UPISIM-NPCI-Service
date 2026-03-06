package com.example.npci.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PspCallbackRequest {
    private String upiTxnId;
    private String pspTxnId;
    private String rrn;
    private Double amount;
    private String status;
    private String responseCode;
    private String failureReason;
}
