package com.example.npci.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vpa_registry")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VpaRegistry {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String vpa;              // rahul@icici

    @Column(nullable = false)
    private String bank_code;           // icici, ybl, oksbi

    @Column(nullable = false)
    private String pspId;            // PSP_ICICI, PSP_PHONEPE

    @Column(nullable = false)
    private String status;           // ACTIVE / BLOCKED

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
