package com.metasolver.service;

import com.metasolver.model.MatchingEngine;
import com.metasolver.model.MatchingEngine.OrderBookState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orderbook")
public class OrderBookService {
    private static final Logger logger = LoggerFactory.getLogger(OrderBookService.class);
    private final MatchingEngine matchingEngine;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserSessionService sessionService;

    @Autowired
    public OrderBookService(SimpMessagingTemplate messagingTemplate, UserSessionService sessionService) {
        this.matchingEngine = new MatchingEngine(messagingTemplate);
        this.messagingTemplate = messagingTemplate;
        this.sessionService = sessionService;
        logger.info("OrderBookService initialized");
    }

    @PostMapping("/session")
    public Map<String, String> createSession(@RequestBody SessionRequest request) {
        try {
            logger.info("Received session request: {}", request);
            
            if (request.getMessage() == null || request.getSignature() == null || request.getAddress() == null) {
                throw new IllegalArgumentException("Missing required fields in session request");
            }

            String sessionId = sessionService.createSession(
                request.getMessage(),
                request.getSignature(),
                request.getAddress()
            );
            
            logger.info("Created session {} for address {}", sessionId, request.getAddress());
            return Map.of("sessionId", sessionId);
        } catch (Exception e) {
            logger.error("Failed to create session", e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to create session: " + e.getMessage()
            );
        }
    }

    @PostMapping("/order")
    public long submitOrder(@RequestBody OrderRequest request) {
        if (!sessionService.verifySession(request.getSessionId(), request.getTraderId())) {
            throw new RuntimeException("Invalid session");
        }
        
        logger.info("Received order: {}", request);
        long orderId = matchingEngine.submitOrder(
            request.getTraderId(),
            request.getType(),
            request.getSide(),
            request.getPrice(),
            request.getQuantity()
        );
        
        broadcastOrderBookUpdate();
        return orderId;
    }

    private void broadcastOrderBookUpdate() {
        logger.info("‼️ Broadcasting orderbook update");
        try {
            OrderBookState state = matchingEngine.getOrderBookState();
            messagingTemplate.convertAndSend("/topic/orderbook", state);
            logger.info("Order book update broadcasted");
        } catch (Exception e) {
            logger.error("Failed to broadcast order book update", e);
        }
    }

    @DeleteMapping("/order/{orderId}")
    public void cancelOrder(@PathVariable long orderId, @RequestParam MatchingEngine.OrderSide side) {
        logger.info("Cancelling order: {} ({})", orderId, side);
        matchingEngine.cancelOrder(orderId, side);
    }

    @GetMapping("/status")
    public void getOrderBookStatus() {
        matchingEngine.displayOrderBook();
    }

    @GetMapping("/trades")
    public List<MatchingEngine.Trade> getAllTrades() {
        logger.info("Fetching all trades");
        return matchingEngine.getAllTrades();
    }

    public double getBalance(String sessionId, String traderId, String asset) {
        if (!sessionService.verifySession(sessionId, traderId)) {
            throw new RuntimeException("Invalid session");
        }
        
        Map<String, Double> userBalances = matchingEngine.getBalances().get(traderId);
        if (userBalances == null) {
            return 0.0;
        }
        return userBalances.getOrDefault(asset, 0.0);
    }

    public void displayAllBalances() {
        logger.info("=== Current Exchange Balances ===");
        Map<String, Map<String, Double>> allBalances = matchingEngine.getBalances();
        
        allBalances.forEach((trader, assets) -> {
            logger.info("Trader {}", trader);
            assets.forEach((asset, balance) -> 
                logger.info("  {} : {}", asset, balance));
        });
        
        logger.info("=== End Balances ===");
    }
} 