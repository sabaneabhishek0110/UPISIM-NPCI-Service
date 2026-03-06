package com.example.npci.Repository;

import com.example.npci.model.NpciTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface NpciTransactionsRepository extends JpaRepository<NpciTransaction, UUID> {
    @Override
    Optional<NpciTransaction> findById(UUID id);
    
    @Query("SELECT t FROM NpciTransaction t WHERE t.psp_txn_id = :pspTxnId")
    Optional<NpciTransaction> findByPspTxnId(@Param("pspTxnId") String pspTxnId);
    
    @Query("SELECT t FROM NpciTransaction t WHERE t.upiTxnId = :upiTxnId")
    Optional<NpciTransaction> findByUpiTxnId(@Param("upiTxnId") String upiTxnId);
    
    @Query("SELECT t FROM NpciTransaction t WHERE t.rrn = :rrn")
    Optional<NpciTransaction> findByRrn(@Param("rrn") String rrn);
}
