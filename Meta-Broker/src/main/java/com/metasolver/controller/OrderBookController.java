package com.metasolver.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.metasolver.service.OrderBookService;
import com.metasolver.service.OrderRequest;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;

@RestController
@RequestMapping("/order-book")
public class OrderBookController {

    private static final Logger logger = LoggerFactory.getLogger(OrderBookController.class);

    private final OrderBookService orderBookService;

    @Autowired
    public OrderBookController(OrderBookService orderBookService) {
        this.orderBookService = orderBookService;
    }

    @PostMapping("/order")
    public ResponseEntity<String> submitOrder(@RequestBody OrderRequest request) {
        try {
            return ResponseEntity.ok(String.valueOf(
                orderBookService.submitOrder(request)
            ));
        } catch (Exception e) {
            logger.error("Error submitting order", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                               .body("Error submitting order: " + e.getMessage());
        }
    }

    @GetMapping("/balance")
    public ResponseEntity<?> getBalance(
            @RequestParam String sessionId,
            @RequestParam String traderId,
            @RequestParam String asset) {
        try {
            double balance = orderBookService.getBalance(sessionId, traderId, asset);
            return ResponseEntity.ok().body(new BalanceResponse(balance));
        } catch (Exception e) {
            logger.error("Error getting balance", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                               .body("Error getting balance: " + e.getMessage());
        }
    }

    @PostMapping("/session")
    public ResponseEntity<?> createSession(@RequestBody SessionRequest request) {
        try {
            Map<String, String> response = orderBookService.createSession(request);
            return ResponseEntity.ok().body(new SessionResponse(response.get("sessionId")));
        } catch (Exception e) {
            logger.error("Error creating session", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                               .body("Error creating session: " + e.getMessage());
        }
    }

    public static class SessionRequest extends com.metasolver.service.SessionRequest {
        // Inherits all fields and methods from OrderBookService.SessionRequest
    }

    private static class SessionResponse {
        private final String sessionId;

        public SessionResponse(String sessionId) {
            this.sessionId = sessionId;
        }
    }

    // Inner class for balance response
    private static class BalanceResponse {
        private final double balance;

        public BalanceResponse(double balance) {
            this.balance = balance;
        }

        public double getBalance() {
            return balance;
        }
    }
} 