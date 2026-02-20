package com.example.npci.Repository;

import com.example.npci.model.NpciTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NpciTransactionsRepository extends JpaRepository<NpciTransaction, UUID> {
    public Optional<NpciTransaction> findById(UUID id);
}
