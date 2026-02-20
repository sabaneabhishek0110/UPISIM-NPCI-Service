package com.example.npci.controller;

import com.example.npci.dto.HealthRequest;
import com.example.npci.service.NpciService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.security.PrivateKey;
import java.time.Instant;

@RestController
@RequestMapping
public class HealthController {

    @Value("${ICICI_BANK_URL}")
    private String ICICI_BANK_URL;

    @Value("${HDFC_BANK_URL}")
    private String HDFC_BANK_URL;

    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;
    private NpciService npciService;

    public HealthController(RestTemplate restTemplate,ObjectMapper objectMapper,NpciService npciService) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.npciService = npciService;
    }

    @PostMapping("/health")
    public String health() throws Exception{
        HealthRequest Req = new HealthRequest("NPCI");

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        String payload = objectMapper.writeValueAsString(Req);
        PrivateKey privateKey = npciService.loadPrivateKey();
        String signature = npciService.signPayload(payload,privateKey);
        headers.set("X-NPCI-ID","NPCI");
        headers.set("X-SIGNATURE",signature);


        headers.add("X-TIMESTAMP", Instant.now().toString());
        HttpEntity<HealthRequest> entity = new HttpEntity<>(Req, headers);
        String url = HDFC_BANK_URL + "/health";
        String response = restTemplate.postForObject(
                url,
                entity,
                String.class
        );
        System.out.println("Response from HDFC : "+response);

        String url1 = ICICI_BANK_URL + "/health";
        String response1 = restTemplate.postForObject(
                url1,
                entity,
                String.class
        );
        System.out.println("Response from ICICI : "+response1);

        return "NPCI Service Is Now Active!!!";
    }
}
