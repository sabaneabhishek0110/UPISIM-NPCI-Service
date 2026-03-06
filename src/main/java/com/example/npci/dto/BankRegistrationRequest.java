package com.example.npci.dto;

import lombok.Data;

@Data
public class BankRegistrationRequest {
    private String bankCode;
    private String bankName;
    private String ifscPrefix;
    private String debitEndpoint;
    private String creditEndpoint;
    private String balanceEndpoint;
    private String reversalEndpoint;
}
