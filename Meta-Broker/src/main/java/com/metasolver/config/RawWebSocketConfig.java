package com.metasolver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableWebSocket
public class RawWebSocketConfig implements WebSocketConfigurer {

    @Bean
    public RawWebSocketHandler rawWebSocketHandler() {
        return new RawWebSocketHandler();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(rawWebSocketHandler(), "/ws-raw")
                .setAllowedOrigins("*")
                .setAllowedOriginPatterns("*");
    }
}

@Component
class RawWebSocketHandler extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(RawWebSocketHandler.class);
    
    private static final String LOG_FORMAT = "Raw WebSocket - {}";

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        logger.info(LOG_FORMAT, "Connection established: " + session.getId());
        try {
            session.sendMessage(new TextMessage("Connected to raw WebSocket"));
        } catch (Exception e) {
            logger.error(LOG_FORMAT, "Error sending welcome message: " + e.getMessage());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        logger.info(LOG_FORMAT, "Received message: " + message.getPayload());
        try {
            session.sendMessage(new TextMessage("Server received: " + message.getPayload()));
        } catch (Exception e) {
            logger.error(LOG_FORMAT, "Error sending response: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        logger.info(LOG_FORMAT, "Connection closed: " + session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        logger.error(LOG_FORMAT, "Transport error: " + exception.getMessage());
    }
}