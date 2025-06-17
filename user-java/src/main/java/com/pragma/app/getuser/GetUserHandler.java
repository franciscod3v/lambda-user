package com.pragma.app.getuser;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class GetUserHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final DynamoDbClient dynamoClient = DynamoDbClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String tableName = System.getenv("DYNAMO_TABLE");

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        // Log raw event
        context.getLogger().log("Raw input: " + input);

        // Extraer pathParameters de forma segura
        Map<String, String> pathParams = new HashMap<>();
        Object pp = input.get("pathParameters");
        if (pp instanceof Map<?, ?> raw) {
            for (Map.Entry<?, ?> entry : raw.entrySet()) {
                Object k = entry.getKey();
                Object v = entry.getValue();
                if (k instanceof String key && v instanceof String val) {
                    pathParams.put(key, val);
                }
            }
        }

        String id = pathParams.get("id");
        if (id == null || id.isBlank()) {
            return buildErrorResponse(400, "Missing path parameter 'id'");
        }
        context.getLogger().log("Extracted id: " + id);

        try {
            // Consultar DynamoDB
            Map<String, AttributeValue> key = Map.of(
                    "id", AttributeValue.builder().s(id).build()
            );
            var result = dynamoClient.getItem(GetItemRequest.builder()
                    .tableName(tableName)
                    .key(key)
                    .build());

            var item = result.item();
            if (item == null || item.isEmpty()) {
                return buildErrorResponse(404, "User not found for id=" + id);
            }

            // Mapear resultado
            Map<String, String> user = Map.of(
                    "id",    item.get("id").s(),
                    "name",  item.get("name").s(),
                    "email", item.get("email").s()
            );
            context.getLogger().log("Fetched user: " + user);

            return buildResponse(200, user);

        } catch (Exception e) {
            context.getLogger().log("Error getting user: " + e.getMessage());
            return buildErrorResponse(500, e.getMessage());
        }
    }

    private Map<String, Object> buildResponse(int statusCode, Object body) {
        try {
            String json = objectMapper.writeValueAsString(body);
            return Map.of(
                    "statusCode",         statusCode,
                    "headers",            Map.of("Content-Type", "application/json"),
                    "body",               json,
                    "isBase64Encoded",    false
            );
        } catch (Exception e) {
            return Map.of(
                    "statusCode",      500,
                    "headers",         Map.of("Content-Type", "text/plain"),
                    "body",            "Error serializing response",
                    "isBase64Encoded", false
            );
        }
    }

    private Map<String, Object> buildErrorResponse(int statusCode, String message) {
        return buildResponse(statusCode, Map.of("error", message));
    }
}
