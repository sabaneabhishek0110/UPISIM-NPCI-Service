package com.example.npci.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bank_registry")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankRegistry {

    @Id
    @Column(nullable = false, unique = true)
    private String bank_code;           // icici, ybl, oksbi

    @Column(nullable = false)
    private String bankName;          // ICICI Bank

    @Column(nullable = false)
    private String ifscPrefix;        // ICICI, YESB, SBIN

    @Column(nullable = false)
    private String debitEndpoint;     // http://localhost:8086/bank/debit

    @Column(nullable = false)
    private String creditEndpoint;    // http://localhost:8086/bank/credit

    @Column(nullable = false)
    private String balanceEndpoint;   // http://localhost:8086/bank/balance?vpa

    @Column(nullable = false)
    private String status;            // ACTIVE / INACTIVE

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
