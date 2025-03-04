package com.metasolver.model;

public class Trade {
    public enum Side {
        BUY,
        SELL
    }

    private String tradeId;
    private String timestamp;
    private String makerAddress;
    private String takerAddress;
    private String amount;
    private String price;
    private boolean isMatched;
    private boolean preventSelfTrade;

    public Trade(String tradeId, String timestamp, String makerAddress, String takerAddress, String amount, String price, boolean preventSelfTrade) {
        this.tradeId = tradeId;
        this.timestamp = timestamp;
        this.makerAddress = makerAddress;
        this.takerAddress = takerAddress;
        this.amount = amount;
        this.price = price;
        this.isMatched = false;
        this.preventSelfTrade = preventSelfTrade;
    }

    // Getters
    public String getTradeId() { return tradeId; }
    public String getTimestamp() { return timestamp; }
    public String getMakerAddress() { return makerAddress; }
    public String getTakerAddress() { return takerAddress; }
    public String getAmount() { return amount; }
    public String getPrice() { return price; }
    public boolean isMatched() { return isMatched; }
    public boolean isPreventSelfTrade() {
        return preventSelfTrade;
    }

    // Setters
    public void setTradeId(String tradeId) { this.tradeId = tradeId; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public void setMakerAddress(String makerAddress) { this.makerAddress = makerAddress; }
    public void setTakerAddress(String takerAddress) { this.takerAddress = takerAddress; }
    public void setAmount(String amount) { this.amount = amount; }
    public void setPrice(String price) { this.price = price; }
    public void setMatched(boolean matched) { isMatched = matched; }
    public void setPreventSelfTrade(boolean preventSelfTrade) {
        this.preventSelfTrade = preventSelfTrade;
    }

    @Override
    public String toString() {
        return String.format(
            "Trade{tradeId=%s, timestamp=%s, makerAddress=%s, takerAddress=%s, amount=%s, price=%s, matched=%s}",
            tradeId,
            timestamp,
            makerAddress.substring(0, 6) + "...",
            takerAddress.substring(0, 6) + "...",
            amount,
            price,
            isMatched
        );
    }
} 