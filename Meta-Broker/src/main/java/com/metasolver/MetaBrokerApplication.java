package com.metasolver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.metasolver")
public class MetaBrokerApplication {
    public static void main(String[] args) {
        SpringApplication.run(MetaBrokerApplication.class, args);
    }
} 