package com.example.npci.service;

import com.example.npci.Repository.BankRegistryRepository;
import com.example.npci.Repository.NpciOutboxRepository;
import com.example.npci.Repository.NpciTransactionsRepository;
import com.example.npci.Repository.VpaRegistryRepository;
import com.example.npci.model.NpciOutbox;
import com.example.npci.model.NpciTransaction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {
    private final VpaRegistryRepository vpaRegistryRepository;
    private final BankRegistryRepository bankRegistryRepository;
    private final NpciTransactionsRepository npciTransactionsRepository;
    private final NpciOutboxRepository npciOutboxRepository;
    private final ObjectMapper objectMapper;

    public PaymentService(
            VpaRegistryRepository vpaRegistryRepository,
            BankRegistryRepository bankRegistryRepository,
            NpciTransactionsRepository npciTransactionsRepository,
            NpciOutboxRepository npciOutboxRepository,
            ObjectMapper objectMapper
    ) {
        this.vpaRegistryRepository = vpaRegistryRepository;
        this.bankRegistryRepository = bankRegistryRepository;
        this.npciTransactionsRepository = npciTransactionsRepository;
        this.npciOutboxRepository = npciOutboxRepository;
        this.objectMapper = objectMapper;
    }

    public String getBankNameByVpa(String vpa) {
        String bank_code = vpaRegistryRepository.findBankCodeByVpa(vpa);
        return bank_code;
    }

    public String findDebitUrl(String bankCode) {
        String debitUrl = bankRegistryRepository.findDebitUrlByBankCode(bankCode);
        return debitUrl;
    }

    public String findCreditUrl(String bankCode) {
        String creditUrl = bankRegistryRepository.findCreditUrlByBankCode(bankCode);
        return creditUrl;
    }

    public String findBalanceUrlFromBankCode(String bankCode) {
        String balanceUrl = bankRegistryRepository.findBalanceUrlByBankCode(bankCode);
        return balanceUrl;
    }

    public String findReversalUrl(String bankCode) {
        String reversalUrl = bankRegistryRepository.findReversalUrlByBankCode(bankCode);
        if (reversalUrl == null || reversalUrl.isEmpty()) {
            // Fallback: try using debit endpoint for reversal if reversal endpoint not configured
            return bankRegistryRepository.findDebitUrlByBankCode(bankCode);
        }
        return reversalUrl;
    }

    @Transactional
    public NpciTransaction createNpciTransactionWithDebitEvent(
            String payerVpa,
            String payeeVpa,
            Double amount,
            String pspTxnId,
            String payerBankCode,
            String payeeBankCode,
            String pin,
            String callbackUrl
    ) throws JsonProcessingException {
        // Create transaction
        NpciTransaction npciTransaction = new NpciTransaction();
        npciTransaction.setPayerVpa(payerVpa);
        npciTransaction.setPayeeVpa(payeeVpa);
        npciTransaction.setAmount(amount);
        npciTransaction.setPsp_txn_id(pspTxnId);
        npciTransaction.setPayerBankCode(payerBankCode);
        npciTransaction.setPayeeBankCode(payeeBankCode);
        npciTransaction.setStatus("INITIATED");
        npciTransaction.setCallbackUrl(callbackUrl);
        
        String rrn = generateRRN();
        npciTransaction.setRrn(rrn);
        
        String upiTxnId = generateUpiTxnId();
        npciTransaction.setUpiTxnId(upiTxnId);
        
        npciTransaction = npciTransactionsRepository.save(npciTransaction);
        
        // Create DEBIT_REQUEST outbox event
        createOutboxEvent(
                npciTransaction.getId(),
                "DEBIT_REQUEST",
                createDebitPayload(payerVpa, payeeVpa, amount, pin, rrn, upiTxnId, pspTxnId, payerBankCode)
        );
        
        return npciTransaction;
    }

    @Transactional
    public void createOutboxEvent(UUID transactionId, String eventType, String payload) {
        NpciOutbox outboxEvent = NpciOutbox.builder()
                .transactionId(transactionId)
                .eventType(eventType)
                .payload(payload)
                .build();
        
        npciOutboxRepository.save(outboxEvent);
    }

    public String createDebitPayload(String payerVpa, String payeeVpa, Double amount, String pin, String rrn, String upiTxnId, String pspTxnId, String bankCode) throws JsonProcessingException {
        var payload = new java.util.HashMap<String, Object>();
        payload.put("PayerVpa", payerVpa);
        payload.put("PayeeVpa", payeeVpa);
        payload.put("amount", amount);
        payload.put("pin", pin);
        payload.put("rrn", rrn);
        payload.put("upiTxnId", upiTxnId);
        payload.put("pspTxnId", pspTxnId);
        payload.put("bankCode", bankCode);
        return objectMapper.writeValueAsString(payload);
    }

    public String createCreditPayload(String payeeVpa, String payerVpa, Double amount, String rrn, String upiTxnId, String pspTxnId, String bankCode) throws JsonProcessingException {
        var payload = new java.util.HashMap<String, Object>();
        payload.put("PayeeVpa", payeeVpa);
        payload.put("PayerVpa", payerVpa);
        payload.put("amount", amount);
        payload.put("rrn", rrn);
        payload.put("upiTxnId", upiTxnId);
        payload.put("pspTxnId", pspTxnId);
        payload.put("bankCode", bankCode);
        return objectMapper.writeValueAsString(payload);
    }

    public String createReversalPayload(String payerVpa, String payeeVpa, Double amount, String rrn, String upiTxnId, String pspTxnId, String bankCode) throws JsonProcessingException {
        var payload = new java.util.HashMap<String, Object>();
        payload.put("payerVpa", payerVpa);
        payload.put("payeeVpa", payeeVpa);
        payload.put("amount", amount);
        payload.put("rrn", rrn);
        payload.put("upiTxnId", upiTxnId);
        payload.put("pspTxnId", pspTxnId);
        payload.put("bankCode", bankCode);
        return objectMapper.writeValueAsString(payload);
    }

    public String createPspCallbackPayload(String upiTxnId, String pspTxnId, String rrn, Double amount, String status, String responseCode, String failureReason) throws JsonProcessingException {
        var payload = new java.util.HashMap<String, Object>();
        payload.put("upiTxnId", upiTxnId);
        payload.put("pspTxnId", pspTxnId);
        payload.put("rrn", rrn);
        payload.put("amount", amount);
        payload.put("status", status);
        payload.put("responseCode", responseCode);
        payload.put("failureReason", failureReason);
        return objectMapper.writeValueAsString(payload);
    }

    public static String generateRRN() {
        LocalDate now = LocalDate.now();

        String yy = String.format("%02d", now.getYear() % 100);
        String ddd = String.format("%03d", now.getDayOfYear());

        // Take last 7 digits of nanoTime
        long nano = System.nanoTime() % 10_000_000;
        String seq = String.format("%07d", nano);

        return yy + ddd + seq;
    }

    public static String generateUpiTxnId() {
        return "UPI" + UUID.randomUUID().toString().replace("-", "");
    }

    @Transactional
    public void updateTransactionStatus(UUID txnId, String status, String responseCode, String failureReason) {
        Optional<NpciTransaction> npciTransactionOpt = npciTransactionsRepository.findById(txnId);
        if (npciTransactionOpt.isPresent()) {
            NpciTransaction transaction = npciTransactionOpt.get();
            transaction.setStatus(status);
            transaction.setResponseCode(responseCode);
            if (failureReason != null) {
                transaction.setFailureReason(failureReason);
            }
            npciTransactionsRepository.save(transaction);
        }
    }

    @Transactional
    public void handleDebitFailure(UUID txn_id, String failureReason) {
        Optional<NpciTransaction> npciTransaction = npciTransactionsRepository.findById(txn_id);
        if (npciTransaction.isPresent()) {
            NpciTransaction transaction = npciTransaction.get();
            transaction.setRrn(null);
            transaction.setFailureReason(failureReason);
            transaction.setStatus("FAILED");
            npciTransactionsRepository.save(transaction);
        }
    }

    public Optional<NpciTransaction> getTransactionById(UUID id) {
        return npciTransactionsRepository.findById(id);
    }

    public NpciTransaction getTransactionByPspTxnId(String pspTxnId) {
        return npciTransactionsRepository.findByPspTxnId(pspTxnId).orElse(null);
    }

    public NpciTransaction getTransactionByUpiTxnId(String upiTxnId) {
        return npciTransactionsRepository.findByUpiTxnId(upiTxnId).orElse(null);
    }

    public NpciTransaction getTransactionByRrn(String rrn) {
        return npciTransactionsRepository.findByRrn(rrn).orElse(null);
    }
}
