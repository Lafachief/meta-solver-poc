package com.metasolver.model;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.charset.StandardCharsets;

public class ProofCompressor {
    private static final Logger logger = LoggerFactory.getLogger(ProofCompressor.class);

    public CompressedProof compressProof(MatchedTrade matchedTrade) {
        try {
            // Create compressed data string
            String compressedData = Base64.encodeBase64String(
                String.format("%s:%s:%s", 
                    matchedTrade.getAmount(), 
                    matchedTrade.getPrice(),
                    matchedTrade.getTradeId()
                ).getBytes(StandardCharsets.UTF_8)
            );
            
            // Create proof with required fields only (timestamp, maker, taker, data)
            CompressedProof proof = new CompressedProof(
                matchedTrade.getTimestamp(),
                matchedTrade.getMakerAddress(),
                matchedTrade.getTakerAddress(),
                compressedData
            );
            
            // Set optional fields after construction
            proof.setAmount(matchedTrade.getAmount());
            proof.setPrice(matchedTrade.getPrice());
            
            logger.info("Created compressed proof for trade ID: {}", matchedTrade.getTradeId());
            return proof;
        } catch (Exception e) {
            logger.error("Failed to compress proof", e);
            throw new RuntimeException("Failed to compress proof", e);
        }
    }
}