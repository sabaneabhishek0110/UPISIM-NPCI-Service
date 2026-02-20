package com.example.npci.service;

import com.example.npci.Repository.BankRegistryRepository;
import com.example.npci.Repository.NpciTransactionsRepository;
import com.example.npci.Repository.VpaRegistryRepository;
import com.example.npci.model.NpciTransaction;
import com.example.npci.model.VpaRegistry;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {
    VpaRegistryRepository vpaRegistryRepository;
    BankRegistryRepository bankRegistryRepository;
    NpciTransactionsRepository npciTransactionsRepository;

    public PaymentService(
            VpaRegistryRepository vpaRegistryRepository,
            BankRegistryRepository bankRegistryRepository,
            NpciTransactionsRepository npciTransactionsRepository
    ) {
        this.vpaRegistryRepository = vpaRegistryRepository;
        this.bankRegistryRepository = bankRegistryRepository;
        this.npciTransactionsRepository = npciTransactionsRepository;
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

    public NpciTransaction createNpciTransaction(String payerVpa,String payeeVpa,Double amount,String psp_txn_id){
        NpciTransaction npciTransaction = new NpciTransaction();
        npciTransaction.setPayerVpa(payerVpa);
        npciTransaction.setPayeeVpa(payeeVpa);
        npciTransaction.setAmount(amount);
        npciTransaction.setPsp_txn_id(psp_txn_id);
        String rrn = generateRRN();
        npciTransaction.setRrn(rrn);
        String upi_txn_id = generateUpiTxnId();
        npciTransaction.setUpiTxnId(upi_txn_id);
        npciTransactionsRepository.save(npciTransaction);
        return npciTransaction;
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

    public void handleDebitFailure(UUID txn_id,String failureReason) {
        Optional<NpciTransaction> npciTransaction = npciTransactionsRepository.findById(txn_id);
        NpciTransaction transaction = npciTransaction.get();
        transaction.setRrn(null);
        transaction.setFailureReason(failureReason);
        npciTransactionsRepository.save(transaction);
    }
}
