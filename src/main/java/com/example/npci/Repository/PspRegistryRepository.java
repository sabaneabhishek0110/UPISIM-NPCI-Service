package com.example.npci.Repository;

import com.example.npci.model.PspRegistry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PspRegistryRepository extends JpaRepository<PspRegistry,String> {
    Optional<PspRegistry> findByPspId(String pspId);
}
