package com.example.npci.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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
    private String debitEndpoint;     // e.g., http://localhost:8086/api/bank/debit

    @Column(nullable = false)
    private String creditEndpoint;    // e.g., http://localhost:8086/api/bank/credit

    @Column(nullable = false)
    private String balanceEndpoint;   // e.g., http://localhost:8086/api/bank/balance?vpa

    private String reversalEndpoint;  // e.g., http://localhost:8086/api/bank/reversal

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
