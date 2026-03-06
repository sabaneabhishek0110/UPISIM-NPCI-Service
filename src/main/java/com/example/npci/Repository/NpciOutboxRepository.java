package com.example.npci.Repository;

import com.example.npci.model.NpciOutbox;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NpciOutboxRepository extends JpaRepository<NpciOutbox, UUID> {
    
//    @Query("SELECT e FROM NpciOutbox e WHERE e.status = 'PENDING' ORDER BY e.createdAt ASC")
//    List<NpciOutbox> findPendingEvents();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT o FROM NpciOutbox o
        WHERE o.status IN ('PENDING','FAILED')
        AND (o.nextRetryAt IS NULL OR o.nextRetryAt <= CURRENT_TIMESTAMP)
        ORDER BY o.createdAt
    """)
    List<NpciOutbox> findPendingEvents(Pageable pageable);
    
    @Query("SELECT e FROM NpciOutbox e WHERE e.transactionId = :transactionId")
    List<NpciOutbox> findByTransactionId(UUID transactionId);
}
