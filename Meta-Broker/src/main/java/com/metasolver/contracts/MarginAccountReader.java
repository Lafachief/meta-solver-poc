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
import org.web3j.utils.Numeric;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.core.RemoteFunctionCall;

public class MarginAccountReader {
    private static final Logger logger = LoggerFactory.getLogger(MarginAccountReader.class);
    private final Web3j web3j;
    private final SymmioDeposit contract;
    private final Credentials brokerCredentials;
    private final byte[] BROKER_ROLE;

    public MarginAccountReader(String nodeUrl, String contractAddress, String brokerPrivateKey, String brokerRole) {
        try {
            // Add extensive debug logging
            logger.info("\nInitializing MarginAccountReader with:");
            logger.info("Node URL: {}", nodeUrl);
            logger.info("Contract Address: {}", contractAddress);
            logger.info("Broker Role: {}", brokerRole);

            // Validate inputs
            if (nodeUrl == null || nodeUrl.isEmpty()) {
                throw new IllegalArgumentException("Node URL cannot be null or empty");
            }
            if (contractAddress == null || contractAddress.isEmpty()) {
                throw new IllegalArgumentException("Contract address cannot be null or empty");
            }
            if (brokerPrivateKey == null || brokerPrivateKey.isEmpty()) {
                throw new IllegalArgumentException("Broker private key cannot be null or empty");
            }
            if (brokerRole == null || brokerRole.isEmpty()) {
                throw new IllegalArgumentException("Broker role cannot be null or empty");
            }

            // Ensure hex formatting
            String formattedContractAddress = contractAddress.startsWith("0x") ? contractAddress : "0x" + contractAddress;
            String formattedBrokerPrivateKey = brokerPrivateKey.startsWith("0x") ? brokerPrivateKey.substring(2) : brokerPrivateKey;
            String formattedBrokerRole = brokerRole.startsWith("0x") ? brokerRole : "0x" + brokerRole;

            // Create credentials and log the derived address
            this.brokerCredentials = Credentials.create(formattedBrokerPrivateKey);
            logger.info("Derived broker address: {}", brokerCredentials.getAddress());

            // Initialize web3j and contract
            this.web3j = Web3j.build(new HttpService(nodeUrl));
            this.BROKER_ROLE = Numeric.hexStringToByteArray(formattedBrokerRole);
            
            BigInteger gasPrice = BigInteger.valueOf(20_000_000_000L);
            BigInteger gasLimit = BigInteger.valueOf(6_721_975L);
            ContractGasProvider gasProvider = new StaticGasProvider(gasPrice, gasLimit);

            // Load contract
            this.contract = SymmioDeposit.load(formattedContractAddress, web3j, brokerCredentials, gasProvider);

            // Log role check details
            logger.info("\nChecking BROKER_ROLE:");
            logger.info("Broker address: {}", brokerCredentials.getAddress());
            logger.info("Contract address: {}", contract.getContractAddress());
            logger.info("BROKER_ROLE bytes length: {}", BROKER_ROLE.length);
            logger.info("BROKER_ROLE hex: 0x{}", Numeric.toHexString(BROKER_ROLE).substring(2));

            // Use AccessControl interface method directly
            try {
                byte[] role = new byte[32];
                System.arraycopy(BROKER_ROLE, 0, role, 0, Math.min(BROKER_ROLE.length, 32));
                
                // Log all parameters before making the call
                logger.info("\nPreparing contract call:");
                logger.info("From address: {}", brokerCredentials.getAddress());
                logger.info("To contract: {}", contract.getContractAddress());
                logger.info("Role bytes: {}", Numeric.toHexString(role));

                // Try using the contract's generated method first
                boolean hasRole = contract.hasBrokerRole(brokerCredentials.getAddress()).send();
                logger.info("Has role (using contract method): {}", hasRole);

                if (!hasRole) {
                    throw new RuntimeException("Broker address does not have BROKER_ROLE");
                }

            } catch (Exception e) {
                logger.error("Failed to check role membership: {}", e.getMessage());
                throw new RuntimeException("Failed to check role membership: " + e.getMessage(), e);
            }

            logger.info("MarginAccountReader initialized with broker role");
        } catch (Exception e) {
            logger.error("Failed to initialize MarginAccountReader", e);
            throw new RuntimeException("Failed to initialize MarginAccountReader", e);
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

            // Check if there are pending balances that need to be settled
            if (balances.get(1).compareTo(BigInteger.ZERO) > 0 || 
                balances.get(3).compareTo(BigInteger.ZERO) > 0) {
                settleBalances(account);
            }
        } catch (Exception e) {
            logger.error("Error checking balances", e);
            throw new RuntimeException("Failed to check balances", e);
        }
    }

    public void settleBalances(String account) {
        try {
            logger.info("Attempting to settle balances for account: {}", account);
            
            // Use the contract's generated method instead of manual encoding
            RemoteFunctionCall<TransactionReceipt> settleCall = contract.settleBalances(account);
            TransactionReceipt receipt = settleCall.send();
            
            logger.info("Settlement transaction hash: {}", receipt.getTransactionHash());
            
            // Check new balances
            List<BigInteger> newBalances = contract.getBalances(account).send();
            logger.info("Updated balances after settlement:");
            logger.info("ETH Balance: {}", newBalances.get(0));
            logger.info("ETH Margin Balance: {}", newBalances.get(1));
            logger.info("USDC Balance: {}", newBalances.get(2));
            logger.info("USDC Margin Balance: {}", newBalances.get(3));
            
        } catch (Exception e) {
            logger.error("Failed to settle balances for account: {}", account, e);
            throw new RuntimeException("Failed to settle balances", e);
        }
    }
}