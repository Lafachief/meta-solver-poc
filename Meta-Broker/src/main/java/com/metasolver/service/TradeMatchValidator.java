package com.metasolver.service;

import com.metasolver.model.MatchedTrade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TradeMatchValidator {
    private static final Logger logger = LoggerFactory.getLogger(TradeMatchValidator.class);

    public boolean validateTrade(MatchedTrade trade) {
        try {
            if (trade == null) {
                logger.warn("Trade validation failed: trade is null");
                return false;
            }

            if (trade.getMakerAddress() == null || trade.getMakerAddress().isEmpty()) {
                logger.warn("Trade validation failed: maker address is missing");
                return false;
            }

            if (trade.getTakerAddress() == null || trade.getTakerAddress().isEmpty()) {
                logger.warn("Trade validation failed: taker address is missing");
                return false;
            }

            if (trade.getAmount() == null || trade.getAmount().isEmpty()) {
                logger.warn("Trade validation failed: amount is missing");
                return false;
            }

            logger.info("Trade validated successfully: {}", trade.getTradeId());
            return true;
        } catch (Exception e) {
            logger.error("Error validating trade", e);
            return false;
        }
    }
} 