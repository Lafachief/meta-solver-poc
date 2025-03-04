package com.metasolver.model;

import com.metasolver.contracts.SymmioDeposit;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.DefaultGasProvider;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SymmioBalanceReader {
    private final Web3j web3j;
    private final SymmioDeposit depositContract;
    private final MatchingEngine matchingEngine;
    private final ScheduledExecutorService scheduler;
    private static final Logger logger = LoggerFactory.getLogger(SymmioBalanceReader.class);
    
    public SymmioBalanceReader(String rpcUrl, String contractAddress, MatchingEngine matchingEngine) {
        this.web3j = Web3j.build(new HttpService(rpcUrl));
        this.depositContract = SymmioDeposit.load(
            contractAddress,
            web3j,
            Credentials.create("YOUR_PRIVATE_KEY"), // For read-only operations
            new DefaultGasProvider()
        );
        this.matchingEngine = matchingEngine;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }
    
    public void startBalanceMonitoring() {
        scheduler.scheduleAtFixedRate(
            this::updateBalances,
            0,
            30,  // Adjust polling interval as needed
            TimeUnit.SECONDS
        );
    }
    
    private void updateBalances() {
        try {
            List<String> activeUsers = matchingEngine.getActiveUsers();
            for (String userAddress : activeUsers) {
                BigInteger balance = getBalance(userAddress);
                matchingEngine.updateUserBalance(userAddress, balance);
            }
        } catch (Exception e) {
            // Log error appropriately
            e.printStackTrace();
        }
    }
    
    public BigInteger getBalance(String account) {
        try {
            List<BigInteger> balances = depositContract.getBalances(account).send();
            return balances.get(0);  // Return ETH balance
        } catch (Exception e) {
            logger.error("Error getting balance", e);
            throw new RuntimeException("Failed to get balance", e);
        }
    }
    
    public void shutdown() {
        scheduler.shutdown();
        web3j.shutdown();
    }
} 