package com.example.npci.dto;

import lombok.*;

import java.util.UUID;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {
    private UUID id;
    private String name;
    private String phone;
    private String vpa;
    private String bankName;

}
