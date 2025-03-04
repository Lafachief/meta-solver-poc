package com.metasolver.tools;

import org.springframework.context.ApplicationContext;
import org.springframework.boot.SpringApplication;
import com.metasolver.MetaBrokerApplication;
import com.metasolver.service.OrderBookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BalanceChecker {
    private static final Logger logger = LoggerFactory.getLogger(BalanceChecker.class);

    public static void main(String[] args) {
        logger.info("Starting Internal Exchange Balance Checker...");
        
        // Get Spring context from main application
        ApplicationContext context = SpringApplication.run(MetaBrokerApplication.class, args);
        
        try {
            // Get OrderBookService from context
            OrderBookService orderBookService = context.getBean(OrderBookService.class);
            
            // Display balances
            logger.info("Checking internal exchange balances...");
            orderBookService.displayAllBalances();
            
        } catch (Exception e) {
            logger.error("Error checking balances: ", e);
        } finally {
            // Exit after displaying balances
            System.exit(0);
        }
    }
} 