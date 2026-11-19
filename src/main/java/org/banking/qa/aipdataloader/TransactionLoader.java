
package org.banking.qa.aipdataloader;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TransactionLoader {

    private static final String VERSION_A_URL =
            "http://localhost:8081/api/v1/transactions";

    private static final String VERSION_B_URL =
            "http://localhost:8083/api/v1/transactions";

    public static void main(String[] args) {

        ObjectMapper mapper = new ObjectMapper();
        HttpClient client = HttpClient.newHttpClient();

        try (InputStream input = TransactionLoader.class
                .getResourceAsStream("/transactiondata.json")) {

            if (input == null) {
                System.out.println("ERROR: transactiondata.json not found!");
                return;
            }

            List<JsonNode> transactions = mapper.readValue(
                    input,
                    new TypeReference<List<JsonNode>>() {}
            );

            System.out.println("Total transactions loaded: "
                    + transactions.size());

            System.out.println("\n========== VERSION A ==========");
            sendTransactions(client, mapper, transactions, VERSION_A_URL);

            System.out.println("\n========== VERSION B ==========");
            sendTransactions(client, mapper, transactions, VERSION_B_URL);

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void sendTransactions(
            HttpClient client,
            ObjectMapper mapper,
            List<JsonNode> transactions,
            String apiUrl) {

        int successCount = 0;
        int failureCount = 0;

        for (JsonNode transaction : transactions) {

            String transactionId =
                    transaction.get("transactionId").asText();

            try {
                String jsonBody =
                        mapper.writeValueAsString(transaction);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

                int statusCode = response.statusCode();

                System.out.println("Transaction: " + transactionId
                        + " | HTTP Status: " + statusCode);

                if (statusCode >= 200 && statusCode < 300) {
                    successCount++;
                } else {
                    failureCount++;
                    System.out.println("Response: " + response.body());
                }

            } catch (Exception e) {
                failureCount++;
                System.out.println("Failed: " + transactionId
                        + " | " + e.getMessage());
            }
        }

        System.out.println("\nLoad Summary for: " + apiUrl);
        System.out.println("Total records: " + transactions.size());
        System.out.println("Successful: " + successCount);
        System.out.println("Failed: " + failureCount);
    }
}
