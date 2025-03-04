package com.metasolver.service;

import com.metasolver.model.CompressedProof;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AIOZStorageClient {
    private static final Logger logger = LoggerFactory.getLogger(AIOZStorageClient.class);
    private final HttpClient client;

    public AIOZStorageClient() {
        this.client = HttpClient.newHttpClient();
    }

    public void storeProof(CompressedProof proof) {
        try {
            URI uri = new URI("https://aioz.storage/api/v1/store");
            HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .POST(HttpRequest.BodyPublishers.ofString(proof.getData()))
                .build();
                
            HttpResponse<String> response = client.send(request, 
                HttpResponse.BodyHandlers.ofString());
                
            logger.info("Stored proof with status: {}", response.statusCode());
        } catch (Exception e) {
            logger.error("Error storing proof", e);
            throw new RuntimeException("Failed to store proof", e);
        }
    }
} 