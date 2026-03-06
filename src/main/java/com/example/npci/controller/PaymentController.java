package com.example.npci.controller;

import com.example.npci.dto.PaymentRequest;
import com.example.npci.dto.PaymentResponse;
import com.example.npci.model.NpciTransaction;
import com.example.npci.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
@Slf4j
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Async payment initiation using outbox pattern
     * Returns acknowledgment immediately, processing happens asynchronously
     */
    @PostMapping("/initiate")
    public ResponseEntity<?> initiatePayment(@RequestBody PaymentRequest requestBody) {
        try {
            String payerVpa = requestBody.getPayerVpa();
            String payeeVpa = requestBody.getPayeeVpa();
            Double amount = requestBody.getAmount();
            String pin = requestBody.getPin();
            String pspTxnId = requestBody.getPspTxnId();
            String callbackUrl = requestBody.getCallbackUrl();
            
            log.info("Payment initiation request received - Payer: {}, Payee: {}, Amount: {}, PSP TxnId: {}", 
                    payerVpa, payeeVpa, amount, pspTxnId);
            
            // Get bank codes
            String payerBankCode = paymentService.getBankNameByVpa(payerVpa);
            String payeeBankCode = paymentService.getBankNameByVpa(payeeVpa);
            
            log.info("Resolved banks - Payer Bank: {}, Payee Bank: {}", payerBankCode, payeeBankCode);
            
            // Create transaction and DEBIT_REQUEST outbox event (all in one transaction)
            NpciTransaction transaction = paymentService.createNpciTransactionWithDebitEvent(
                    payerVpa,
                    payeeVpa,
                    amount,
                    pspTxnId,
                    payerBankCode,
                    payeeBankCode,
                    pin,
                    callbackUrl
            );
            
            log.info("Transaction created with ID: {}, UPI TxnId: {}, RRN: {}", 
                    transaction.getId(), transaction.getUpiTxnId(), transaction.getRrn());
            
            // Return acknowledgment immediately
            PaymentResponse response = new PaymentResponse();
            response.setPayer_vpa(payerVpa);
            response.setPayee_vpa(payeeVpa);
            response.setAmount(amount);
            response.setPsp_txn_id(pspTxnId);
            response.setUpi_txn_id(transaction.getUpiTxnId());
            response.setRrn(transaction.getRrn());
            response.setStatus("PROCESSING");
            response.setResponse_code("ACKNOWLEDGED");
            response.setFailureReason(null);
            
            log.info("Returning acknowledgment for UPI TxnId: {}", transaction.getUpiTxnId());
            
            return ResponseEntity.accepted().body(response);
            
        } catch (Exception e) {
            log.error("Error initiating payment", e);
            
            PaymentResponse errorResponse = new PaymentResponse();
            errorResponse.setPayer_vpa(requestBody.getPayerVpa());
            errorResponse.setPayee_vpa(requestBody.getPayeeVpa());
            errorResponse.setAmount(requestBody.getAmount());
            errorResponse.setPsp_txn_id(requestBody.getPspTxnId());
            errorResponse.setStatus("ERROR");
            errorResponse.setResponse_code("ERROR");
            errorResponse.setFailureReason("Failed to initiate payment: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Legacy synchronous endpoint - kept for backward compatibility
     * Consider deprecating in favor of /initiate
     */
    @PostMapping("/process")
    @Deprecated
    public ResponseEntity<?> processPaymentSync(@RequestBody PaymentRequest requestBody) {
        // Redirect to async endpoint
        return initiatePayment(requestBody);
    }

    /**
     * Get transaction status by PSP transaction ID
     * Used by PSP/Frontend to poll for transaction status
     */
    @GetMapping("/status/{pspTxnId}")
    public ResponseEntity<?> getTransactionStatus(@PathVariable String pspTxnId) {
        try {
            log.info("Status request received for PSP TxnId: {}", pspTxnId);
            
            NpciTransaction transaction = paymentService.getTransactionByPspTxnId(pspTxnId);
            
            if (transaction == null) {
                log.warn("Transaction not found for PSP TxnId: {}", pspTxnId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Transaction not found");
            }
            
            PaymentResponse response = new PaymentResponse();
            response.setPayer_vpa(transaction.getPayerVpa());
            response.setPayee_vpa(transaction.getPayeeVpa());
            response.setAmount(transaction.getAmount());
            response.setPsp_txn_id(transaction.getPsp_txn_id());
            response.setUpi_txn_id(transaction.getUpiTxnId());
            response.setRrn(transaction.getRrn());
            response.setStatus(transaction.getStatus());
            response.setResponse_code(transaction.getResponseCode());
            response.setFailureReason(transaction.getFailureReason());
            
            log.info("Returning status for PSP TxnId: {}, Status: {}", pspTxnId, transaction.getStatus());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching transaction status for PSP TxnId: {}", pspTxnId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching transaction status: " + e.getMessage());
        }
    }

    /**
     * Get transaction status by UPI transaction ID
     */
    @GetMapping("/status/upi/{upiTxnId}")
    public ResponseEntity<?> getTransactionStatusByUpiTxnId(@PathVariable String upiTxnId) {
        try {
            log.info("Status request received for UPI TxnId: {}", upiTxnId);
            
            NpciTransaction transaction = paymentService.getTransactionByUpiTxnId(upiTxnId);
            
            if (transaction == null) {
                log.warn("Transaction not found for UPI TxnId: {}", upiTxnId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Transaction not found");
            }
            
            PaymentResponse response = new PaymentResponse();
            response.setPayer_vpa(transaction.getPayerVpa());
            response.setPayee_vpa(transaction.getPayeeVpa());
            response.setAmount(transaction.getAmount());
            response.setPsp_txn_id(transaction.getPsp_txn_id());
            response.setUpi_txn_id(transaction.getUpiTxnId());
            response.setRrn(transaction.getRrn());
            response.setStatus(transaction.getStatus());
            response.setResponse_code(transaction.getResponseCode());
            response.setFailureReason(transaction.getFailureReason());
            
            log.info("Returning status for UPI TxnId: {}, Status: {}", upiTxnId, transaction.getStatus());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching transaction status for UPI TxnId: {}", upiTxnId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching transaction status: " + e.getMessage());
        }
    }
}
