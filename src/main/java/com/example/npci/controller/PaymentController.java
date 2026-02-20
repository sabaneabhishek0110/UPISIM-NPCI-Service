package com.example.npci.controller;

import com.example.npci.dto.*;
import com.example.npci.model.NpciTransaction;
import com.example.npci.service.NpciService;
import com.example.npci.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.security.PrivateKey;
import java.time.Instant;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {
    private final PaymentService paymentService;
    private RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final NpciService npciService;

    public PaymentController(
            PaymentService paymentService,
            NpciService npciService,
            ObjectMapper objectMapper,
            RestTemplate restTemplate) {
        this.paymentService = paymentService;
        this.npciService = npciService;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    @PostMapping("/process")
    public ResponseEntity<?> initiatePayment(@RequestBody PaymentRequest requestBody, HttpServletRequest request) {
        try{
            String PayerVpa = requestBody.getPayerVpa();
            String PayeeVpa = requestBody.getPayeeVpa();
            Double amount = requestBody.getAmount();
            String pin = requestBody.getPin();
            String psp_txn_id = requestBody.getPsp_txn_id();
            String debit_bank_code = paymentService.getBankNameByVpa(PayerVpa);
            String debitUrl = paymentService.findDebitUrl(debit_bank_code);
            System.out.println("Payer Vpa: " + PayerVpa);
            System.out.println("Payee Vpa: " + PayeeVpa);

            String credit_bank_code = paymentService.getBankNameByVpa(PayeeVpa);
            String creditUrl = paymentService.findCreditUrl(credit_bank_code);
            System.out.println("Debit Bank Code : "+debit_bank_code);
            System.out.println("Credit Bank Code : "+credit_bank_code);
            NpciTransaction npciTransaction = paymentService.createNpciTransaction(PayerVpa,PayeeVpa,amount,psp_txn_id);
            String upi_txn_id = npciTransaction.getUpiTxnId();
            String rrn = npciTransaction.getRrn();
            System.out.println("Debit URL: " + debitUrl);
            System.out.println("Credit URL: " + creditUrl);

            DebitRequest debitRequest = new DebitRequest(PayerVpa,amount,pin,rrn,upi_txn_id,psp_txn_id);

            String payload = objectMapper.writeValueAsString(debitRequest);
            PrivateKey privateKey = npciService.loadPrivateKey();
            String signature = npciService.signPayload(payload,privateKey);

            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("X-TIMESTAMP", Instant.now().toString());
            headers.add("X-NPCI-ID","NPCI");
            headers.add("X-SIGNATURE",signature);
            HttpEntity<String> req = new HttpEntity<>(payload,headers);

            ResponseEntity<DebitResponse> debitResponse = restTemplate.postForEntity(
                    debitUrl,
                    req,
                    DebitResponse.class
            );
            System.out.println("response :"+String.valueOf(debitResponse.getBody().getResponseCode()));
            String responseCode = String.valueOf(debitResponse.getBody().getResponseCode());
            System.out.println("ResponseCode of the debitResponse : "+responseCode);
            PaymentResponse response = new PaymentResponse();
            response.setPayer_vpa(PayerVpa);
            response.setPayee_vpa(PayeeVpa);
            response.setAmount(amount);
            response.setRrn(rrn);
            response.setUpi_txn_id(upi_txn_id);
            response.setPsp_txn_id(psp_txn_id);
            if(responseCode.equals("U00")){
                CreditRequest creditRequest = new CreditRequest(PayeeVpa,amount,upi_txn_id,rrn,psp_txn_id);

                String payload1 = objectMapper.writeValueAsString(creditRequest);
                PrivateKey privateKey1 = npciService.loadPrivateKey();
                String signature1 = npciService.signPayload(payload1,privateKey1);

                HttpHeaders headers1 = new HttpHeaders();
                headers1.setContentType(MediaType.APPLICATION_JSON);
                headers1.set("X-TIMESTAMP",Instant.now().toString());
                headers1.set("X-NPCI-ID","NPCI");
                headers1.set("X-SIGNATURE",signature1);

                HttpEntity<String> req1 = new HttpEntity<>(payload1,headers1);
                ResponseEntity<CreditResponse> creditResponse = restTemplate.postForEntity(
                        creditUrl,
                        req1,
                        CreditResponse.class
                );
                response.setStatus(creditResponse.getBody().getStatus());
                response.setResponse_code(creditResponse.getBody().getResponseCode());
                response.setFailureReason(creditResponse.getBody().getFailureReason());
                System.out.println("Credit completed");
                return ResponseEntity.ok(response);
            }
            else{
                String failureReason = debitResponse.getBody().getFailureReason();
                String status = debitResponse.getBody().getStatus();
                paymentService.handleDebitFailure(npciTransaction.getId(),failureReason);
                response.setStatus(status);
                response.setResponse_code(String.valueOf(debitResponse.getBody().getResponseCode()));
                response.setFailureReason(failureReason);
                return ResponseEntity.ok(response);
            }
        }
        catch(Exception e){
            System.out.println("Error in payment : "+e);
            throw new IllegalStateException("failed to send Payment Request from npci to bank");
        }

    }
}
