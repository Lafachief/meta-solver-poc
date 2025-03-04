package com.metasolver.service;

import com.metasolver.model.MatchedTrade;
import com.metasolver.model.CompressedProof;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TradeProofService {
    private static final Logger logger = LoggerFactory.getLogger(TradeProofService.class);
    private final AIOZStorageClient storageClient;
    private final ProofCompressor compressor;
    private final TradeMatchValidator validator;
    
    public TradeProofService() {
        this.storageClient = new AIOZStorageClient();
        this.compressor = new ProofCompressor();
        this.validator = new TradeMatchValidator();
    }
    
    public void processTradeProof(MatchedTrade trade) {
        try {
            if (validator.validateTrade(trade)) {
                CompressedProof proof = compressor.compressProof(trade);
                storageClient.storeProof(proof);
                logger.info("Trade proof processed successfully: {}", trade.getTradeId());
            } else {
                logger.warn("Trade validation failed: {}", trade.getTradeId());
            }
        } catch (Exception e) {
            logger.error("Error processing trade proof", e);
            throw new RuntimeException("Failed to process trade proof", e);
        }
    }
}