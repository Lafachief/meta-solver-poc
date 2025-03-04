package com.metasolver.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TestController {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @GetMapping("/test-broadcast")
    public String testBroadcast() {
        messagingTemplate.convertAndSend("/topic/matchedTrades", 
            Map.of("test", "data", "timestamp", System.currentTimeMillis()));
        return "Broadcast sent!";
    }
} 