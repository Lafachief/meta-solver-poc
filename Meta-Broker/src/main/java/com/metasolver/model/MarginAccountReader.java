package com.metasolver.model;

import com.metasolver.contracts.SymmioDeposit;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;
import java.io.FileReader;
import java.math.BigInteger;
import java.util.List;

public class MarginAccountReader {
    private static final Logger logger = LoggerFactory.getLogger(MarginAccountReader.class);
    private final SymmioDeposit contract;

    public MarginAccountReader(SymmioDeposit contract) {
        this.contract = contract;
    }

    public static void main(String[] args) {
        try {
            // Read config
            JSONParser parser = new JSONParser();
            JSONObject config = (JSONObject) parser.parse(new FileReader("../Users/config.json"));
            
            // Connect to local Hardhat node
            Web3j web3j = Web3j.build(new HttpService((String)config.get("rpcUrl")));

            // Get contract address from config
            String contractAddress = (String)config.get("contractAddress");

            // Gas settings (for Hardhat)
            BigInteger gasPrice = BigInteger.valueOf(20_000_000_000L); // 20 Gwei
            BigInteger gasLimit = BigInteger.valueOf(4_300_000L);
            ContractGasProvider gasProvider = new StaticGasProvider(gasPrice, gasLimit);

            // Load credentials (for read-only operations, we can use dummy credentials)
            Credentials credentials = Credentials.create("0x0000000000000000000000000000000000000000000000000000000000000000");

            // Load the contract
            SymmioDeposit contract = SymmioDeposit.load(contractAddress, web3j, credentials, gasProvider);

            // Check both test accounts
            String[] accountsToCheck = {
                (String)config.get("user_a"),
                (String)config.get("user_b")
            };

            for (String account : accountsToCheck) {
                System.out.println("\nChecking account: " + account);
                new MarginAccountReader(contract).checkBalances(account);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void checkBalances(String account) {
        try {
            List<BigInteger> balances = contract.getBalances(account).send();
            logger.info("Account: {}", account);
            logger.info("ETH Balance: {}", balances.get(0));
            logger.info("ETH Margin Balance: {}", balances.get(1));
            logger.info("USDC Balance: {}", balances.get(2));
            logger.info("USDC Margin Balance: {}", balances.get(3));
        } catch (Exception e) {
            logger.error("Error checking balances", e);
            throw new RuntimeException("Failed to check balances", e);
        }
    }
} 