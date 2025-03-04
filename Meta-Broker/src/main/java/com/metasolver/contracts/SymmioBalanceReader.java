package com.metasolver.contracts;

import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;
import java.math.BigInteger;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SymmioBalanceReader {
    private static final Logger logger = LoggerFactory.getLogger(SymmioBalanceReader.class);
    private final SymmioDeposit depositContract;
    private final Web3j web3j;
    private final Credentials credentials;

    public SymmioBalanceReader(String nodeUrl, String contractAddress, String privateKey) {
        try {
            this.web3j = Web3j.build(new HttpService(nodeUrl));
            this.credentials = Credentials.create(privateKey);

            BigInteger gasPrice = BigInteger.valueOf(20_000_000_000L);
            BigInteger gasLimit = BigInteger.valueOf(6_721_975L);
            ContractGasProvider gasProvider = new StaticGasProvider(gasPrice, gasLimit);

            this.depositContract = SymmioDeposit.load(
                contractAddress,
                web3j,
                credentials,
                gasProvider
            );

            logger.info("SymmioBalanceReader initialized");
        } catch (Exception e) {
            logger.error("Failed to initialize SymmioBalanceReader", e);
            throw new RuntimeException("Failed to initialize SymmioBalanceReader", e);
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
} 