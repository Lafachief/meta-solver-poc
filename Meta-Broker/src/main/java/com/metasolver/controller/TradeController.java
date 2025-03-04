package com.metasolver.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.metasolver.model.MatchingEngine;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TradeController {
    
    private final MatchingEngine matchingEngine;
    
    @Autowired
    public TradeController(MatchingEngine matchingEngine) {
        this.matchingEngine = matchingEngine;
    }
    
    @GetMapping("/trades")
    public List<MatchingEngine.Trade> getAllTrades() {
        return matchingEngine.getAllTrades();
    }
} 