package com.example.npci;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NpciApplication {

	public static void main(String[] args) {
		SpringApplication.run(NpciApplication.class, args);
	}

}
