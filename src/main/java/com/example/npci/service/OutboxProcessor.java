package com.example.npci.service;

import com.example.npci.Repository.NpciOutboxRepository;
import com.example.npci.Repository.NpciTransactionsRepository;
import com.example.npci.dto.*;
import com.example.npci.model.NpciOutbox;
import com.example.npci.model.NpciTransaction;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.security.PrivateKey;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {

    private final NpciOutboxRepository npciOutboxRepository;
    private final NpciTransactionsRepository npciTransactionsRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final NpciService npciService;
    private final PaymentService paymentService;

    private static final int MAX_RETRY_COUNT = 3;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processOutbox() {
        log.info("Processing outbox events...");
        
        List<NpciOutbox> pendingEvents = npciOutboxRepository.findPendingEvents();
        
        log.info("Found {} pending events", pendingEvents.size());
        
        for (NpciOutbox event : pendingEvents) {
            try {
                event.setStatus(NpciOutbox.EventStatus.PROCESSING);
                npciOutboxRepository.save(event);
                
                processEvent(event);
                
                event.setStatus(NpciOutbox.EventStatus.SUCCESS);
                event.setProcessedAt(LocalDateTime.now());
                npciOutboxRepository.save(event);
                
                log.info("Successfully processed event: {} for transaction: {}", 
                        event.getEventType(), event.getTransactionId());
                
            } catch (Exception e) {
                log.error("Error processing event: {} for transaction: {}", 
                        event.getEventType(), event.getTransactionId(), e);
                handleEventFailure(event, e.getMessage());
            }
        }
    }

    private void processEvent(NpciOutbox event) throws Exception {
        switch (event.getEventType()) {
            case "DEBIT_REQUEST" -> handleDebitRequest(event);
            case "CREDIT_REQUEST" -> handleCreditRequest(event);
            case "REVERSAL_REQUEST" -> handleReversalRequest(event);
            case "CALLBACK_PSP" -> handlePspCallback(event);
            default -> log.warn("Unknown event type: {}", event.getEventType());
        }
    }

    private void handleDebitRequest(NpciOutbox event) throws Exception {
        UUID transactionId = event.getTransactionId();
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = objectMapper.readValue(event.getPayload(), Map.class);
        
        NpciTransaction transaction = npciTransactionsRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));
        
        // Build debit request matching bank DTO field names
        DebitRequest debitRequest = new DebitRequest();
        debitRequest.setPayerVpa((String) payload.get("PayerVpa"));
        debitRequest.setPayeeVpa((String) payload.get("PayeeVpa"));
        debitRequest.setAmount((Double) payload.get("amount"));
        debitRequest.setPin((String) payload.get("pin"));
        debitRequest.setUpiTxnId((String) payload.get("upiTxnId"));
        debitRequest.setRrn((String) payload.get("rrn"));
        debitRequest.setPspTxnId((String) payload.get("pspTxnId"));
        
        String bankCode = (String) payload.get("bankCode");
        String debitUrl = paymentService.findDebitUrl(bankCode);
        
        ResponseEntity<BankResponse> response = callBankApi(debitUrl, debitRequest, BankResponse.class);
        BankResponse bankResponse = response.getBody();
        
        if (bankResponse != null && "SUCCESS".equals(bankResponse.getStatus())) {
            log.info("Debit successful for transaction: {}", transaction.getUpiTxnId());
            
            paymentService.updateTransactionStatus(transactionId, "DEBIT_SUCCESS", "U00", null);
            
            // Create CREDIT_REQUEST event
            String creditPayload = paymentService.createCreditPayload(
                    transaction.getPayeeVpa(),
                    transaction.getPayerVpa(),
                    transaction.getAmount(),
                    transaction.getRrn(),
                    transaction.getUpiTxnId(),
                    transaction.getPsp_txn_id(),
                    transaction.getPayeeBankCode()
            );
            paymentService.createOutboxEvent(transactionId, "CREDIT_REQUEST", creditPayload);
            
        } else {
            String failureReason = bankResponse != null ? bankResponse.getMessage() : "Unknown error";
            log.warn("Debit failed for transaction: {}. Reason: {}", transaction.getUpiTxnId(), failureReason);
            
            paymentService.updateTransactionStatus(transactionId, "FAILED", "FAILED", failureReason);
            
            // Create PSP callback for failure
            createCallbackEvent(transaction, "FAILED", "FAILED", failureReason);
        }
    }

    private void handleCreditRequest(NpciOutbox event) throws Exception {
        UUID transactionId = event.getTransactionId();
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = objectMapper.readValue(event.getPayload(), Map.class);
        
        NpciTransaction transaction = npciTransactionsRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));
        
        // Build credit request matching bank DTO field names
        CreditRequest creditRequest = new CreditRequest();
        creditRequest.setPayeeVpa((String) payload.get("PayeeVpa"));
        creditRequest.setPayerVpa((String) payload.get("PayerVpa"));
        creditRequest.setAmount((Double) payload.get("amount"));
        creditRequest.setUpiTxnId((String) payload.get("upiTxnId"));
        creditRequest.setRrn((String) payload.get("rrn"));
        creditRequest.setPspTxnId((String) payload.get("pspTxnId"));
        
        String bankCode = (String) payload.get("bankCode");
        String creditUrl = paymentService.findCreditUrl(bankCode);
        
        ResponseEntity<BankResponse> response = callBankApi(creditUrl, creditRequest, BankResponse.class);
        BankResponse bankResponse = response.getBody();
        
        if (bankResponse != null && "SUCCESS".equals(bankResponse.getStatus())) {
            log.info("Credit successful for transaction: {}", transaction.getUpiTxnId());
            
            paymentService.updateTransactionStatus(transactionId, "CREDIT_SUCCESS", "U00", null);
            
            // Create PSP callback for success
            createCallbackEvent(transaction, "SUCCESS", "U00", null);
            
        } else {
            log.warn("Credit failed for transaction: {}. Initiating reversal", transaction.getUpiTxnId());
            
            paymentService.updateTransactionStatus(transactionId, "CREDIT_FAILED", "FAILED",
                    bankResponse != null ? bankResponse.getMessage() : "Credit failed");
            
            // Create REVERSAL_REQUEST event to reverse the debit
            String reversalPayload = paymentService.createReversalPayload(
                    transaction.getPayerVpa(),
                    transaction.getPayeeVpa(),
                    transaction.getAmount(),
                    transaction.getRrn(),
                    transaction.getUpiTxnId(),
                    transaction.getPsp_txn_id(),
                    transaction.getPayerBankCode()
            );
            paymentService.createOutboxEvent(transactionId, "REVERSAL_REQUEST", reversalPayload);
        }
    }

    private void handleReversalRequest(NpciOutbox event) throws Exception {
        UUID transactionId = event.getTransactionId();
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = objectMapper.readValue(event.getPayload(), Map.class);
        
        NpciTransaction transaction = npciTransactionsRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));
        
        // Build reversal request matching bank DTO field names
        ReversalRequest reversalRequest = new ReversalRequest();
        reversalRequest.setUpiTxnId((String) payload.get("upiTxnId"));
        reversalRequest.setPayerVpa((String) payload.get("payerVpa"));
        reversalRequest.setPayeeVpa((String) payload.get("payeeVpa"));
        reversalRequest.setAmount((Double) payload.get("amount"));
        reversalRequest.setRrn((String) payload.get("rrn"));
        reversalRequest.setPspTxnId((String) payload.get("pspTxnId"));
        
        String bankCode = (String) payload.get("bankCode");
        String reversalUrl = paymentService.findReversalUrl(bankCode);
        
        callBankApi(reversalUrl, reversalRequest, BankResponse.class);
        
        log.info("Reversal completed for transaction: {}", transaction.getUpiTxnId());
        
        paymentService.updateTransactionStatus(transactionId, "REVERSED", "REVERSED",
                "Transaction reversed due to credit failure");
        
        // Create PSP callback for reversal
        createCallbackEvent(transaction, "FAILED", "REVERSED", "Transaction reversed due to credit failure");
    }

    private void handlePspCallback(NpciOutbox event) throws Exception {
        UUID transactionId = event.getTransactionId();
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = objectMapper.readValue(event.getPayload(), Map.class);
        
        // Get callback URL from the transaction
        NpciTransaction transaction = npciTransactionsRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));
        
        String callbackUrl = transaction.getCallbackUrl();
        if (callbackUrl == null || callbackUrl.isEmpty()) {
            log.warn("No callback URL for transaction: {}, skipping PSP callback", transaction.getUpiTxnId());
            return;
        }
        
        PspCallbackRequest callbackRequest = new PspCallbackRequest();
        callbackRequest.setUpiTxnId((String) payload.get("upiTxnId"));
        callbackRequest.setPspTxnId((String) payload.get("pspTxnId"));
        callbackRequest.setRrn((String) payload.get("rrn"));
        callbackRequest.setAmount((Double) payload.get("amount"));
        callbackRequest.setStatus((String) payload.get("status"));
        callbackRequest.setResponseCode((String) payload.get("responseCode"));
        callbackRequest.setFailureReason((String) payload.get("failureReason"));
        
        callPspApi(callbackUrl, callbackRequest);
        
        // Mark callback as sent on the transaction
        transaction.setCallbackSent(true);
        npciTransactionsRepository.save(transaction);
        
        log.info("PSP callback sent successfully for transaction: {}", callbackRequest.getUpiTxnId());
    }

    /**
     * Helper to create a CALLBACK_PSP outbox event using data from the transaction
     */
    private void createCallbackEvent(NpciTransaction transaction, String status, String responseCode, String failureReason) throws Exception {
        String callbackPayload = paymentService.createPspCallbackPayload(
                transaction.getUpiTxnId(),
                transaction.getPsp_txn_id(),
                transaction.getRrn(),
                transaction.getAmount(),
                status,
                responseCode,
                failureReason
        );
        paymentService.createOutboxEvent(transaction.getId(), "CALLBACK_PSP", callbackPayload);
    }

    private <T> ResponseEntity<T> callBankApi(String url, Object request, Class<T> responseType) throws Exception {
        String payload = objectMapper.writeValueAsString(request);
        PrivateKey privateKey = npciService.loadPrivateKey();
        String signature = npciService.signPayload(payload, privateKey);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-TIMESTAMP", Instant.now().toString());
        headers.add("X-NPCI-ID", "NPCI");
        headers.add("X-SIGNATURE", signature);
        
        HttpEntity<String> httpEntity = new HttpEntity<>(payload, headers);
        
        return restTemplate.postForEntity(url, httpEntity, responseType);
    }

    private ResponseEntity<String> callPspApi(String url, Object request) throws Exception {
        String payload = objectMapper.writeValueAsString(request);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-TIMESTAMP", Instant.now().toString());
        headers.add("X-NPCI-ID", "NPCI");
        
        HttpEntity<String> httpEntity = new HttpEntity<>(payload, headers);
        
        return restTemplate.postForEntity(url, httpEntity, String.class);
    }

    private void handleEventFailure(NpciOutbox event, String errorMessage) {
        event.setRetryCount(event.getRetryCount() + 1);
        event.setLastErrorMessage(errorMessage);
        
        if (event.getRetryCount() >= MAX_RETRY_COUNT) {
            event.setStatus(NpciOutbox.EventStatus.FAILED);
            log.error("Event failed after {} retries: {} for transaction: {}", 
                    MAX_RETRY_COUNT, event.getEventType(), event.getTransactionId());
        } else {
            event.setStatus(NpciOutbox.EventStatus.PENDING);
            log.warn("Event failed, will retry (attempt {}/{}): {} for transaction: {}", 
                    event.getRetryCount(), MAX_RETRY_COUNT, event.getEventType(), event.getTransactionId());
        }
        
        npciOutboxRepository.save(event);
    }
}