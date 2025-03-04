package com.metasolver.model;

import java.nio.charset.StandardCharsets;

public class CompressedProof {
    private final String timestamp;
    private final String makerAddress;
    private final String takerAddress;
    private final String data;
    private String amount;
    private String price;

    public CompressedProof(String timestamp, String makerAddress, String takerAddress, String data) {
        this.timestamp = timestamp;
        this.makerAddress = makerAddress;
        this.takerAddress = takerAddress;
        this.data = data;
    }

    // Getters
    public String getTimestamp() { return timestamp; }
    public String getMakerAddress() { return makerAddress; }
    public String getTakerAddress() { return takerAddress; }
    public String getData() { return data; }
    public String getAmount() { return amount; }
    public String getPrice() { return price; }

    // Setters
    public void setAmount(String amount) { this.amount = amount; }
    public void setPrice(String price) { this.price = price; }

    public byte[] getBytes() {
        return data.getBytes(StandardCharsets.UTF_8);
    }
} 