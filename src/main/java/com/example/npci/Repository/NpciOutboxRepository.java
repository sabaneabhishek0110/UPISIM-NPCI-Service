package com.example.npci.Repository;

import com.example.npci.model.NpciOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NpciOutboxRepository extends JpaRepository<NpciOutbox, UUID> {
    
    @Query("SELECT e FROM NpciOutbox e WHERE e.status = 'PENDING' ORDER BY e.createdAt ASC")
    List<NpciOutbox> findPendingEvents();
    
    @Query("SELECT e FROM NpciOutbox e WHERE e.transactionId = :transactionId")
    List<NpciOutbox> findByTransactionId(UUID transactionId);
}
