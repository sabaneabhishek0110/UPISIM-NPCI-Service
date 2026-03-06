package com.example.npci.Repository;

import com.example.npci.model.PspRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PspRegistryRepository extends JpaRepository<PspRegistry,String> {
    Optional<PspRegistry> findByPspId(String pspId);
    
    @Query("SELECT p.callbackUrl FROM PspRegistry p WHERE p.pspId = :pspId")
    String findCallbackUrlByPspId(@Param("pspId") String pspId);
}
