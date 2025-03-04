package com.metasolver.contracts;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.FileReader;

public class SymmioConfig {
    private static final Logger logger = LoggerFactory.getLogger(SymmioConfig.class);
    private final String rpcUrl;
    private final String contractAddress;
    private final String usdcAddress;
    private final String brokerPrivateKey;
    private final String brokerRole;

    public SymmioConfig() {
        JSONObject config = loadConfig();
        this.rpcUrl = (String) config.get("rpcUrl");
        this.contractAddress = (String) config.get("contractAddress");
        this.usdcAddress = (String) config.get("usdcAddress");
        this.brokerPrivateKey = (String) config.get("brokerPrivateKey");
        this.brokerRole = (String) config.get("brokerRole");
        
        if (this.brokerPrivateKey == null || this.brokerPrivateKey.isEmpty()) {
            throw new RuntimeException("brokerPrivateKey not found in config.json");
        }
    }

    private JSONObject loadConfig() {
        try {
            File configFile = new File("config/config.json");
            if (!configFile.exists()) {
                throw new RuntimeException("Config file not found at: " + configFile.getAbsolutePath());
            }

            JSONParser parser = new JSONParser();
            try (FileReader reader = new FileReader(configFile)) {
                return (JSONObject) parser.parse(reader);
            }
        } catch (Exception e) {
            logger.error("Failed to load config: {}", e.getMessage());
            throw new RuntimeException("Failed to load config", e);
        }
    }

    public String getRpcUrl() { return rpcUrl; }
    public String getContractAddress() { return contractAddress; }
    public String getUsdcAddress() { return usdcAddress; }
    public String getBrokerPrivateKey() { return brokerPrivateKey; }
    public String getBrokerRole() { return brokerRole; }
} 