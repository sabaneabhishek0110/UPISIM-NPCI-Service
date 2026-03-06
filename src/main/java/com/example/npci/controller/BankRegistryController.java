package com.example.npci.controller;

import com.example.npci.Repository.BankRegistryRepository;
import com.example.npci.dto.BankRegistrationRequest;
import com.example.npci.model.BankRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/internal/npci/bank")
public class BankRegistryController {

    private final BankRegistryRepository bankRegistryRepository;

    public BankRegistryController(BankRegistryRepository bankRegistryRepository) {
        this.bankRegistryRepository = bankRegistryRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerBank(@RequestBody BankRegistrationRequest request) {
        Optional<BankRegistry> existing = bankRegistryRepository.findById(request.getBankCode());

        BankRegistry bank;
        if (existing.isPresent()) {
            bank = existing.get();
        } else {
            bank = new BankRegistry();
            bank.setBank_code(request.getBankCode());
        }

        bank.setBankName(request.getBankName());
        bank.setIfscPrefix(request.getIfscPrefix());
        bank.setDebitEndpoint(request.getDebitEndpoint());
        bank.setCreditEndpoint(request.getCreditEndpoint());
        bank.setBalanceEndpoint(request.getBalanceEndpoint());
        bank.setReversalEndpoint(request.getReversalEndpoint());
        bank.setStatus("ACTIVE");

        bankRegistryRepository.save(bank);
        return ResponseEntity.ok("Bank registered: " + request.getBankCode());
    }

    @GetMapping("/list")
    public ResponseEntity<?> listBanks() {
        return ResponseEntity.ok(bankRegistryRepository.findAll());
    }
}
