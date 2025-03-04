package com.metasolver.model;

public class MatchedTrade {
    private String tradeId;
    private String timestamp;
    private String makerAddress;
    private String takerAddress;
    private String amount;
    private String price;

    // Default constructor
    public MatchedTrade() {}

    // Full constructor
    public MatchedTrade(String tradeId, String timestamp, String makerAddress, 
                       String takerAddress, String amount, String price) {
        this.tradeId = tradeId;
        this.timestamp = timestamp;
        this.makerAddress = makerAddress;
        this.takerAddress = takerAddress;
        this.amount = amount;
        this.price = price;
    }

    // Getters
    public String getTradeId() { return tradeId; }
    public String getTimestamp() { return timestamp; }
    public String getMakerAddress() { return makerAddress; }
    public String getTakerAddress() { return takerAddress; }
    public String getAmount() { return amount; }
    public String getPrice() { return price; }

    // Setters
    public void setTradeId(String tradeId) { this.tradeId = tradeId; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public void setMakerAddress(String makerAddress) { this.makerAddress = makerAddress; }
    public void setTakerAddress(String takerAddress) { this.takerAddress = takerAddress; }
    public void setAmount(String amount) { this.amount = amount; }
    public void setPrice(String price) { this.price = price; }
} 