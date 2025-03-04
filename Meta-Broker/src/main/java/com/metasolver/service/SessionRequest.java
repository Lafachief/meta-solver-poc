package com.metasolver.service;

public class SessionRequest {
    private String message;
    private String signature;
    private String address;

    // Constructor
    public SessionRequest() {}

    // Getters and setters
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    @Override
    public String toString() {
        return "SessionRequest{" +
            "message='" + message + '\'' +
            ", signature='" + signature + '\'' +
            ", address='" + address + '\'' +
            '}';
    }
} 