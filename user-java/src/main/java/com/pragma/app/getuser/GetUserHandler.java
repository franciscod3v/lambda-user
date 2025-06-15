package com.pragma.app.getuser;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;

import java.util.HashMap;
import java.util.Map;

public class GetUserHandler implements RequestHandler<Object, Object> {

    private final DynamoDbClient dynamoClient = DynamoDbClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String tableName = System.getenv("DYNAMO_TABLE");

    @Override
    public Object handleRequest(Object input, Context context) {
        System.out.println("Raw input: " + input);

        try {
            // Parse the event as JSON tree
            JsonNode event = objectMapper.readTree(input.toString());

            // Extract pathParameters.id
            JsonNode pathParams = event.path("pathParameters");
            String id = pathParams.has("id") ? pathParams.get("id").asText() : null;

            if (id == null || id.isBlank()) {
                return buildErrorResponse(400, "Missing path parameter 'id'");
            }
            System.out.println("Extracted id: " + id);

            // Prepare key and fetch from DynamoDB
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("id", AttributeValue.builder().s(id).build());

            var result = dynamoClient.getItem(GetItemRequest.builder()
                    .tableName(tableName)
                    .key(key)
                    .build());
            var item = result.item();

            if (item == null || item.isEmpty()) {
                return buildErrorResponse(404, "User not found for id=" + id);
            }

            // Map result to a simple user map
            Map<String, String> user = new HashMap<>();
            user.put("id", item.get("id").s());
            user.put("name", item.get("name").s());
            user.put("email", item.get("email").s());
            System.out.println("Fetched user: " + user);

            return buildResponse(200, user);

        } catch (Exception e) {
            System.out.println("Error getting user: " + e.getMessage());
            return buildErrorResponse(500, e.getMessage());
        }
    }

    private Map<String, Object> buildResponse(int statusCode, Object body) {
        try {
            return Map.of(
                "statusCode", statusCode,
                "headers", Map.of("Content-Type", "application/json"),
                "body", objectMapper.writeValueAsString(body)
            );
        } catch (Exception e) {
            return Map.of(
                "statusCode", 500,
                "headers", Map.of("Content-Type", "text/plain"),
                "body", "Error serializing response"
            );
        }
    }

    private Map<String, Object> buildErrorResponse(int statusCode, String message) {
        return buildResponse(statusCode, Map.of("error", message));
    }
}
