package com.workrh.sickness;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.workrh")
public class SicknessServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SicknessServiceApplication.class, args);
    }
}
