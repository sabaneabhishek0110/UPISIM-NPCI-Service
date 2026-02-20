package com.example.npci.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "npci_transactions")
public class NpciTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String payerVpa;
    private String payeeVpa;

    private Double amount;

    @Column(nullable = false, unique = true)
    private String upiTxnId;

    @Column(unique = true)
    private String rrn;

    @Column(unique = true,nullable = false)
    private String psp_txn_id;

    private String payerBankCode;
    private String payeeBankCode;

    private String payerPspCode;
    private String payeePspCode;

    private String status;
    private String responseCode;
    private String failureReason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
