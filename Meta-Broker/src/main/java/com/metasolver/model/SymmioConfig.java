package com.metasolver.model;
public class SymmioConfig {
    private String rpcUrl;
    private String contractAddress;
    
    public SymmioConfig() {
        // Load from environment variables or config file
        this.rpcUrl = System.getenv("SYMMIO_RPC_URL");
        this.contractAddress = System.getenv("SYMMIO_DEPOSIT_CONTRACT");
    }
    
    public String getRpcUrl() {
        return rpcUrl;
    }
    
    public String getContractAddress() {
        return contractAddress;
    }
} 