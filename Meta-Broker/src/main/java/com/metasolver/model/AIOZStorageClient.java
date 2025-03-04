package com.metasolver.model;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.net.URI;

public class AIOZStorageClient {
    private final HttpClient client;
    private final String aiozApiEndpoint;
    private final String apiKey;
    
    public AIOZStorageClient() {
        this.client = HttpClient.newBuilder().build();
        this.aiozApiEndpoint = System.getenv("AIOZ_API_ENDPOINT");
        this.apiKey = System.getenv("AIOZ_API_KEY");
    }
    
    public CompletableFuture<String> store(byte[] compressedProof) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(aiozApiEndpoint + "/store"))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/octet-stream")
            .POST(HttpRequest.BodyPublishers.ofByteArray(compressedProof))
            .build();
            
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() != 200) {
                    throw new RuntimeException("Failed to store proof: " + response.body());
                }
                return response.body(); // Returns the AIOZ hash
            });
    }
    
    public CompletableFuture<byte[]> retrieve(String proofHash) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(aiozApiEndpoint + "/retrieve/" + proofHash))
            .header("Authorization", "Bearer " + apiKey)
            .GET()
            .build();
            
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
            .thenApply(response -> {
                if (response.statusCode() != 200) {
                    throw new RuntimeException("Failed to retrieve proof: " + response.body());
                }
                return response.body();
            });
    }
} 