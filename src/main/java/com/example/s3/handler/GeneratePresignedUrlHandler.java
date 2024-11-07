package com.example.s3.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.example.s3.config.S3Config;
import com.example.s3.model.FileRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

public class GeneratePresignedUrlHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final S3Config s3Config = new S3Config();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        ObjectMapper objectMapper = new ObjectMapper();
        FileRequest fileRequest = null;

        context.getLogger().log("Request Body: " + request.getBody());
        if (request.getBody() == null) {
            context.getLogger().log("Request Body is null");
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("{\"error\": \"Request body is missing.\"}");
        }

        try {
            fileRequest = objectMapper.readValue(request.getBody(), FileRequest.class);
        } catch (Exception e) {
            context.getLogger().log("Error parsing request body: " + e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("{\"error\": \"Invalid input. Filename is required.\"}");
        }

        if (fileRequest == null || fileRequest.getFilename() == null || fileRequest.getFilename().isEmpty()) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("{\"error\": \"Filename is required.\"}");
        }

        String bucketName = System.getenv("BUCKET_NAME");
        context.getLogger().log("Bucket Name: " + bucketName);
        if (bucketName == null || bucketName.isEmpty()) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("{\"error\": \"Bucket name is not set in the environment variable.\"}");
        }

        Map<String, String> metadata = new HashMap<>();
        metadata.put("custom-metadata", "example");

        String presignedUrl = s3Config.createPresignedUrl(bucketName, fileRequest.getFilename(), metadata);
        if (presignedUrl == null) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("{\"error\": \"Failed to generate presigned URL.\"}");
        }

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(Map.of("Content-Type", "application/json"))
                .withBody("{\"upload_url\": \"" + presignedUrl + "\"}");

    }

}
