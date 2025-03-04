package com.metasolver.service;

import com.metasolver.model.CompressedProof;
import com.metasolver.model.MatchedTrade;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProofCompressor {
    private static final Logger logger = LoggerFactory.getLogger(ProofCompressor.class);

    public CompressedProof compressProof(MatchedTrade trade) {
        try {
            // Basic compression logic
            String compressedData = Base64.encodeBase64String(
                String.format("%s:%s:%s", 
                    trade.getAmount(), 
                    trade.getPrice(),
                    trade.getTradeId()
                ).getBytes()
            );
            
            CompressedProof proof = new CompressedProof(
                trade.getTimestamp(),
                trade.getMakerAddress(),
                trade.getTakerAddress(),
                compressedData
            );
            
            proof.setAmount(trade.getAmount());
            proof.setPrice(trade.getPrice());
            
            logger.info("Compressed proof created for trade: {}", trade.getTradeId());
            return proof;
        } catch (Exception e) {
            logger.error("Failed to compress proof", e);
            throw new RuntimeException("Failed to compress proof", e);
        }
    }
}