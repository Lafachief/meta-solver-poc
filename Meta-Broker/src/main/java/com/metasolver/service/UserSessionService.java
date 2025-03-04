package com.metasolver.service;

import org.web3j.crypto.Sign;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.math.BigInteger;
import java.util.UUID;

@Service
public class UserSessionService {
    private static final Logger logger = LoggerFactory.getLogger(UserSessionService.class);
    private final Map<String, String> sessions = new ConcurrentHashMap<>(); // sessionId -> address

    public String createSession(String message, String signature, String address) {
        try {
            logger.info("Creating session for address: {} with message: {}", address, message);
            logger.info("Signature received: {}", signature);
            
            // Convert signature from hex
            byte[] signatureBytes = Numeric.hexStringToByteArray(signature);
            logger.info("Signature bytes length: {}", signatureBytes.length);
            
            byte[] messageBytes = message.getBytes();
            logger.info("Message bytes: {}", Numeric.toHexString(messageBytes));
            
            // Split signature
            byte v = signatureBytes[64];
            byte[] r = new byte[32];
            byte[] s = new byte[32];
            System.arraycopy(signatureBytes, 0, r, 0, 32);
            System.arraycopy(signatureBytes, 32, s, 0, 32);
            
            logger.info("V: {}", v);
            logger.info("R: {}", Numeric.toHexString(r));
            logger.info("S: {}", Numeric.toHexString(s));
            
            Sign.SignatureData signatureData = new Sign.SignatureData(v, r, s);
            
            // Add Ethereum signed message prefix
            String prefix = "\u0019Ethereum Signed Message:\n" + messageBytes.length;
            byte[] prefixBytes = prefix.getBytes();
            byte[] prefixedMessage = new byte[prefixBytes.length + messageBytes.length];
            System.arraycopy(prefixBytes, 0, prefixedMessage, 0, prefixBytes.length);
            System.arraycopy(messageBytes, 0, prefixedMessage, prefixBytes.length, messageBytes.length);
            
            // Recover address
            BigInteger publicKey = Sign.signedMessageToKey(prefixedMessage, signatureData);
            String recoveredAddress = "0x" + Keys.getAddress(publicKey);
            
            logger.info("Recovered address: {}", recoveredAddress);
            logger.info("Expected address: {}", address);
            
            if (!recoveredAddress.equalsIgnoreCase(address)) {
                throw new IllegalArgumentException("Invalid signature for address");
            }
            
            String sessionId = UUID.randomUUID().toString();
            sessions.put(sessionId, address.toLowerCase());
            return sessionId;
            
        } catch (Exception e) {
            logger.error("Failed to create session", e);
            throw new RuntimeException("Failed to create session: " + e.getMessage(), e);
        }
    }

    public boolean verifySession(String sessionId, String address) {
        String sessionAddress = sessions.get(sessionId);
        return sessionAddress != null && sessionAddress.equalsIgnoreCase(address);
    }
} 