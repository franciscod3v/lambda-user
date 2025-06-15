package com.pragma.app.createuser;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pragma.app.sqs.SqsUtils;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CreateUserHandler implements RequestHandler<Map<String, Object>, Object> {

    static class User {
        public String name;
        public String email;
    }

    @Override
    public Object handleRequest(Map<String, Object> input, Context context) {
        ObjectMapper objectMapper = new ObjectMapper();
        DynamoDbClient dynamoClient = DynamoDbClient.create();
        SqsClient sqsClient = SqsUtils.getSqsClient();

        String tableName = System.getenv("DYNAMO_TABLE");
        String queueUrl = "https://sqs.us-east-1.amazonaws.com/648078772415/queue_users";

        context.getLogger().log("DYNAMO_TABLE: " + tableName + "\n");
        context.getLogger().log("SQS_QUEUE_URL (hardcoded): " + queueUrl + "\n");

        try {
            String body = (String) input.get("body");
            if (body == null || body.isEmpty()) {
                return buildErrorResponse(400, "Missing request body");
            }

            User user = objectMapper.readValue(body, User.class);

            String userId = UUID.randomUUID().toString();
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("id", AttributeValue.builder().s(userId).build());
            item.put("name", AttributeValue.builder().s(user.name).build());
            item.put("email", AttributeValue.builder().s(user.email).build());

            dynamoClient.putItem(
                    PutItemRequest.builder()
                            .tableName(tableName)
                            .item(item)
                            .build()
            );

            Map<String, String> messageBody = Map.of(
                    "id", userId,
                    "name", user.name,
                    "email", user.email
            );

            sqsClient.sendMessage(
                    SendMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .messageBody(objectMapper.writeValueAsString(messageBody))
                            .build()
            );

            Map<String, Object> responseBody = Map.of(
                    "message", "User created successfully",
                    "id", userId
            );
            return buildResponse(201, responseBody);

        } catch (Exception e) {
            context.getLogger().log("Error: " + e.getMessage() + "\n");
            return buildErrorResponse(500, e.getMessage());
        }
    }

    private Map<String, Object> buildResponse(int statusCode, Object bodyObject) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return Map.of(
                    "statusCode", statusCode,
                    "headers", Map.of("Content-Type", "application/json"),
                    "body", mapper.writeValueAsString(bodyObject)
            );
        } catch (Exception ex) {
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
