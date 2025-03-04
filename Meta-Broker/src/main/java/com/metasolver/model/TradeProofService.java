package com.metasolver.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.CompletableFuture;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class TradeProofService {
    private static final Logger logger = LoggerFactory.getLogger(TradeProofService.class);
    private final ProofCompressor compressor;
    private final AIOZStorageClient storageClient;

    public TradeProofService(ProofCompressor compressor, AIOZStorageClient storageClient) {
        this.compressor = compressor;
        this.storageClient = storageClient;
    }

    public CompletableFuture<String> createProof(MatchedTrade matchedTrade) {
        try {
            // Get compressed proof object
            CompressedProof compressedProof = compressor.compressProof(matchedTrade);
            
            // Convert to bytes for storage
            byte[] proofBytes = compressedProof.getData().getBytes();
            
            // Store on AIOZ and return the proof hash
            return storageClient.store(proofBytes)
                .thenApply(hash -> {
                    logger.info("Stored proof with hash: {}", hash);
                    return hash;
                });
        } catch (Exception e) {
            logger.error("Failed to create proof", e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    public CompletableFuture<MatchedTrade> retrieveProof(String proofHash) {
        return storageClient.retrieve(proofHash)
            .thenApply(bytes -> {
                try {
                    // Convert bytes back to string
                    String compressedData = new String(bytes);
                    logger.info("Retrieved compressed data: {}", compressedData);

                    // Decode base64 string
                    byte[] decodedBytes = Base64.getDecoder().decode(compressedData);
                    String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);
                    
                    // Split the decoded string into components (amount:price:tradeId)
                    String[] components = decodedString.split(":");
                    if (components.length != 3) {
                        throw new RuntimeException("Invalid proof format");
                    }

                    String amount = components[0];
                    String price = components[1];
                    String tradeId = components[2];

                    // Create new MatchedTrade with decoded data
                    MatchedTrade trade = new MatchedTrade();
                    trade.setTradeId(tradeId);
                    trade.setAmount(amount);
                    trade.setPrice(price);
                    trade.setTimestamp(Instant.now().toString()); // Set current time for retrieved trade
                    
                    logger.info("Successfully decompressed proof for trade ID: {}", tradeId);
                    return trade;
                } catch (Exception e) {
                    logger.error("Failed to decompress proof", e);
                    throw new RuntimeException("Failed to decompress proof", e);
                }
            });
    }
} 