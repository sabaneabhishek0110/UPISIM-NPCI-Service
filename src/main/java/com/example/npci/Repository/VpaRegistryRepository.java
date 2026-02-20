package com.example.npci.Repository;

import com.example.npci.model.VpaRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface VpaRegistryRepository extends JpaRepository<VpaRegistry, UUID> {
    @Query("SELECT v.bank_code FROM VpaRegistry v WHERE v.vpa = :vpa")
    String findBankCodeByVpa(@Param("vpa") String vpa);

}
