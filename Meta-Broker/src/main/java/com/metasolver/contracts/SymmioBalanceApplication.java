package com.metasolver.contracts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Arrays;
import java.util.List;

public class SymmioBalanceApplication {
    private static final Logger logger = LoggerFactory.getLogger(SymmioBalanceApplication.class);
    private final MarginAccountReader balanceReader;

    // List of test accounts to monitor
    private static final List<String> TEST_ACCOUNTS = Arrays.asList(
        "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266",  // Account 0
        "0x70997970C51812dc3A010C7d01b50e0d17dc79C8",  // Account 1
        "0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC"   // Account 2
    );

    public SymmioBalanceApplication() {
        try {
            logger.info("Starting application initialization...");
            
            // Use SymmioConfig instead of direct JSON parsing
            SymmioConfig config = new SymmioConfig();
            
            this.balanceReader = new MarginAccountReader(
                config.getRpcUrl(),
                config.getContractAddress(),
                config.getBrokerPrivateKey(),
                config.getBrokerRole()
            );
            
            logger.info("Application initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize application", e);
            throw new RuntimeException("Failed to initialize application", e);
        }
    }

    public void checkAllBalances() {
        logger.info("Checking balances for all test accounts...");
        for (String account : TEST_ACCOUNTS) {
            logger.info("\n=== Checking account {} ===", account);
            balanceReader.checkBalances(account);
        }
    }

    public static void main(String[] args) {
        try {
            logger.info("Starting Meta-Solver Exchange...");
            SymmioBalanceApplication app = new SymmioBalanceApplication();
            logger.info("Exchange started successfully!");
            
            // Check balances every 10 seconds
            while (true) {
                app.checkAllBalances();
                Thread.sleep(10000);  // Wait 10 seconds
            }
        } catch (Exception e) {
            logger.error("Failed to start exchange", e);
            System.exit(1);
        }
    }
} 