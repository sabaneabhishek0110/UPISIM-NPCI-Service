package com.example.npci.controller;
//THIS IS THE CONTROLLER FOR THE PUBLIC KEY REGISTRATION OF NPCI TO BANK

import com.example.npci.dto.NpciPublicKeyRegistryRequest;
import com.example.npci.dto.NpciRegistryRequestToNpci;
import com.example.npci.service.NpciRegistryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.security.PublicKey;
import java.util.Optional;

@RestController
@RequestMapping("/internal/npci/")
public class NpciRegistryController {
    private final NpciRegistryService npciRegistryService;
    private final RestTemplate restTemplate;

    @Value("${ICICI_BANK_URL}")
    private String ICICI_BANK_URL;

    @Value("${HDFC_BANK_URL}")
    private String HDFC_BANK_URL;

    public NpciRegistryController(NpciRegistryService npciRegistryService,RestTemplate restTemplate) {
        this.npciRegistryService = npciRegistryService;
        this.restTemplate = restTemplate;
    }
    @PostMapping("/icici/register-public-key")
    public ResponseEntity<String> NpciRegistryIcici(@RequestBody NpciRegistryRequestToNpci requestBody) throws Exception{

        PublicKey publicKey = npciRegistryService.loadPublicKey();
        String publicKeyBase64 = npciRegistryService.publicKeyToBase64(publicKey);
        NpciPublicKeyRegistryRequest request = new NpciPublicKeyRegistryRequest();
        request.setNpciId(requestBody.getNpciId());
        request.setPublicKey(publicKeyBase64);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<NpciPublicKeyRegistryRequest> entity =
                new HttpEntity<>(request, headers);
        System.out.println("NPCI Code : "+requestBody.getNpciId());

        String url = ICICI_BANK_URL + "/internal/icici/npci/register-public-key";

        String response = restTemplate.postForObject(
                url,
                entity,
                String.class
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/hdfc/register-public-key")
    public ResponseEntity<String> NpciRegistryHdfc(@RequestBody NpciRegistryRequestToNpci requestBody) throws Exception{
        RestTemplate restTemplate = new RestTemplate();

        PublicKey publicKey = npciRegistryService.loadPublicKey();
        String publicKeyBase64 = npciRegistryService.publicKeyToBase64(publicKey);
        NpciPublicKeyRegistryRequest request = new NpciPublicKeyRegistryRequest();
        request.setNpciId(requestBody.getNpciId());
        request.setPublicKey(publicKeyBase64);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<NpciPublicKeyRegistryRequest> entity =
                new HttpEntity<>(request, headers);
        System.out.println("NPCI Code : "+requestBody.getNpciId());

        String url = HDFC_BANK_URL + "/internal/hdfc/npci/register-public-key";
        String response = restTemplate.postForObject(
                url,
                entity,
                String.class
        );
        return ResponseEntity.ok(response);
    }
}
