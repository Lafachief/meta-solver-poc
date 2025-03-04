package com.metasolver.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.metasolver")
public class SymmioBalanceApplication {
    private static final Logger logger = LoggerFactory.getLogger(SymmioBalanceApplication.class);

    public static void main(String[] args) {
        logger.info("Starting Meta-Solver Exchange...");
        SpringApplication.run(SymmioBalanceApplication.class, args);
    }
} 