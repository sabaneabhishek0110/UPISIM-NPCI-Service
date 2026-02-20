package com.example.npci.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {//mainly used to share the data at the time of login
    private UUID id;
    private String phone;
    private String name;
    private String vpa;

}
