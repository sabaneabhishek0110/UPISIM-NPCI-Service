package com.example.npci.Repository;

import com.example.npci.model.BankRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BankRegistryRepository extends JpaRepository<BankRegistry, String> {
    @Query("SELECT b.debitEndpoint FROM BankRegistry b WHERE b.bank_code = :bank_code")
    String findDebitUrlByBankCode(@Param("bank_code") String bank_code);

    @Query("SELECT b.creditEndpoint FROM BankRegistry b WHERE b.bank_code = :bank_code")
    String findCreditUrlByBankCode(@Param("bank_code") String bank_code);

    @Query("SELECT b.balanceEndpoint FROM BankRegistry b WHERE b.bank_code = :bank_code")
    String findBalanceUrlByBankCode(@Param("bank_code") String bank_code);

    @Query("SELECT b.reversalEndpoint FROM BankRegistry b WHERE b.bank_code = :bank_code")
    String findReversalUrlByBankCode(@Param("bank_code") String bank_code);
}
