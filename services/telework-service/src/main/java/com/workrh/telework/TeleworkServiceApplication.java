package com.workrh.telework;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.workrh")
public class TeleworkServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TeleworkServiceApplication.class, args);
    }
}
