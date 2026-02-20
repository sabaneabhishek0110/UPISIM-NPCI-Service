package com.example.npci.controller;

import com.example.npci.Repository.PspRegistryRepository;
import com.example.npci.dto.PspPublicKeyRequest;
import com.example.npci.model.PspRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/internal/npci/psp")
public class PspRegistryController {

    private final PspRegistryRepository repository;

    public PspRegistryController(PspRegistryRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/register-public-key")
    public ResponseEntity<String> registerPublicKey(
            @RequestBody PspPublicKeyRequest request
    ) {
        Optional<PspRegistry> existing =
                repository.findByPspId(request.getPspCode());

        if (existing.isPresent()) {
            PspRegistry psp = existing.get();
            psp.setPublic_key(request.getPublicKey());
            repository.save(psp);
            return ResponseEntity.ok("Public key updated");
        }

        PspRegistry psp = new PspRegistry();
        psp.setPspId(request.getPspCode());
        psp.setPublic_key(request.getPublicKey());
        psp.setStatus("ACTIVE");
        repository.save(psp);

        return ResponseEntity.ok("PSP public key registered");
    }
}
