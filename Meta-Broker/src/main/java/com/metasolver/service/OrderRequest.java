package com.metasolver.service;

import com.metasolver.model.MatchingEngine;

public class OrderRequest {
    private String sessionId;
    private String traderId;
    private String makerAddress;
    private String takerAddress;
    private String amount;
    private double price;
    private boolean preventSelfTrade;
    private MatchingEngine.OrderType type;
    private MatchingEngine.OrderSide side;
    private double quantity;

    // Getters
    public String getSessionId() { return sessionId; }
    public String getTraderId() { return traderId; }
    public String getMakerAddress() { return makerAddress; }
    public String getTakerAddress() { return takerAddress; }
    public String getAmount() { return amount; }
    public double getPrice() { return price; }
    public boolean isPreventSelfTrade() { return preventSelfTrade; }
    public MatchingEngine.OrderType getType() { return type; }
    public MatchingEngine.OrderSide getSide() { return side; }
    public double getQuantity() { return quantity; }

    // Setters
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public void setTraderId(String traderId) { this.traderId = traderId; }
    public void setMakerAddress(String makerAddress) { this.makerAddress = makerAddress; }
    public void setTakerAddress(String takerAddress) { this.takerAddress = takerAddress; }
    public void setAmount(String amount) { this.amount = amount; }
    public void setPrice(double price) { this.price = price; }
    public void setPreventSelfTrade(boolean preventSelfTrade) { this.preventSelfTrade = preventSelfTrade; }
    public void setType(MatchingEngine.OrderType type) { this.type = type; }
    public void setSide(MatchingEngine.OrderSide side) { this.side = side; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
}