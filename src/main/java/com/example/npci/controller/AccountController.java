package com.example.npci.controller;

import com.example.npci.dto.BalanceRequest;
import com.example.npci.dto.BalanceResponse;
import com.example.npci.service.NpciService;
import com.example.npci.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.security.PrivateKey;
import java.security.Signature;
import java.time.Instant;

@RestController
@RequestMapping("/api/account")
public class AccountController {
    private final NpciService npciService;
    private final PaymentService paymentService;
    private ObjectMapper objectMapper;
    private RestTemplate restTemplate;

    public AccountController(
            NpciService npciService,
            PaymentService paymentService,
            ObjectMapper objectMapper,
            RestTemplate restTemplate) {
        this.npciService = npciService;
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    @PostMapping("/balance")
    public ResponseEntity<BalanceResponse> checkBalance(@RequestBody BalanceRequest balanceRequest) {
        try{
            String vpa = balanceRequest.getVpa();
            String bank_code = paymentService.getBankNameByVpa(vpa);
            String balance_url = paymentService.findBalanceUrlFromBankCode(bank_code);

            String payload = objectMapper.writeValueAsString(balanceRequest);
            PrivateKey privateKey = npciService.loadPrivateKey();
            String signature = npciService.signPayload(payload,privateKey);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-TIMESTAMP", Instant.now().toString());
            headers.set("X-NPCI-ID","NPCI");
            headers.set("X-SIGNATURE",signature);

            HttpEntity<BalanceRequest> entity = new HttpEntity<>(balanceRequest,headers);

            System.out.println("Balance Url : "+balance_url);
            BalanceResponse response = restTemplate.postForObject(
                    balance_url,
                    entity,
                    BalanceResponse.class
            );
            return ResponseEntity.ok(response);
        }
        catch(Exception e){
            return ResponseEntity.internalServerError().body(new BalanceResponse());
        }
    }
}
