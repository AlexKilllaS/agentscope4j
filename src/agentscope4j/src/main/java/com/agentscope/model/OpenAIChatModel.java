package com.agentscope.model;

import com.agentscope.message.ContentBlock;
import com.agentscope.message.Msg;
import com.agentscope.message.TextBlock;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * OpenAI Chat Model implementation for AgentScope4J.
 * 
 * @author Alex Huangfu
 * @version 1.0.0
 */
@Component
public class OpenAIChatModel extends ChatModelBase {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenAIChatModel.class);
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    @Value("${agentscope.models.openai.api-key:}")
    private String apiKey;
    
    @Value("${agentscope.models.openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;
    
    @Value("${agentscope.models.openai.timeout:30000}")
    private long timeout;
    
    /**
     * Constructor for OpenAI Chat Model
     * 
     * @param modelName The name of the OpenAI model (e.g., "gpt-3.5-turbo", "gpt-4")
     * @param stream Whether to use streaming
     */
    public OpenAIChatModel(String modelName, boolean stream) {
        super(modelName, stream);
        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(timeout, TimeUnit.MILLISECONDS)
            .readTimeout(timeout, TimeUnit.MILLISECONDS)
            .writeTimeout(timeout, TimeUnit.MILLISECONDS)
            .build();
    }
    
    /**
     * Default constructor for Spring
     */
    public OpenAIChatModel() {
        super("gpt-3.5-turbo", false);
        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30000, TimeUnit.MILLISECONDS)
            .readTimeout(30000, TimeUnit.MILLISECONDS)
            .writeTimeout(30000, TimeUnit.MILLISECONDS)
            .build();
    }
    
    @Override
    public Object call(
            List<Msg> messages,
            List<Map<String, Object>> tools,
            String toolChoice,
            Map<String, Object> kwargs) {
        
        try {
            // Validate tool choice if provided
            if (toolChoice != null && tools != null) {
                validateToolChoice(toolChoice, tools);
            }
            
            // Build request payload
            Map<String, Object> payload = buildRequestPayload(messages, tools, toolChoice, kwargs);
            
            // Make HTTP request
            String jsonPayload = objectMapper.writeValueAsString(payload);
            
            RequestBody body = RequestBody.create(
                jsonPayload, 
                MediaType.get("application/json; charset=utf-8")
            );
            
            Request request = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(body)
                .build();
            
            long startTime = System.currentTimeMillis();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("OpenAI API call failed: " + response.code() + " " + response.message());
                }
                
                String responseBody = response.body().string();
                JsonNode responseJson = objectMapper.readTree(responseBody);
                
                long endTime = System.currentTimeMillis();
                double timeUsed = (endTime - startTime) / 1000.0;
                
                return parseResponse(responseJson, timeUsed);
            }
            
        } catch (Exception e) {
            logger.error("Error calling OpenAI API", e);
            throw new RuntimeException("Failed to call OpenAI API", e);
        }
    }
    
    /**
     * Build the request payload for OpenAI API
     */
    private Map<String, Object> buildRequestPayload(
            List<Msg> messages,
            List<Map<String, Object>> tools,
            String toolChoice,
            Map<String, Object> kwargs) {
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", modelName);
        payload.put("messages", convertMessagesToOpenAIFormat(messages));
        payload.put("stream", stream);
        
        // Add tools if provided
        if (tools != null && !tools.isEmpty()) {
            payload.put("tools", tools);
            if (toolChoice != null) {
                payload.put("tool_choice", toolChoice);
            }
        }
        
        // Add additional parameters from kwargs
        if (kwargs != null) {
            payload.putAll(kwargs);
        }
        
        return payload;
    }
    
    /**
     * Convert AgentScope messages to OpenAI format
     */
    private List<Map<String, Object>> convertMessagesToOpenAIFormat(List<Msg> messages) {
        List<Map<String, Object>> openaiMessages = new ArrayList<>();
        
        for (Msg msg : messages) {
            Map<String, Object> openaiMsg = new HashMap<>();
            openaiMsg.put("role", msg.getRole());
            
            // Handle content
            String textContent = msg.getTextContent();
            if (textContent != null) {
                openaiMsg.put("content", textContent);
            } else {
                openaiMsg.put("content", msg.getContent());
            }
            
            // Add name if present
            if (msg.getName() != null && !msg.getName().isEmpty()) {
                openaiMsg.put("name", msg.getName());
            }
            
            openaiMessages.add(openaiMsg);
        }
        
        return openaiMessages;
    }
    
    /**
     * Parse OpenAI API response
     */
    private ChatResponse parseResponse(JsonNode responseJson, double timeUsed) {
        try {
            JsonNode choices = responseJson.get("choices");
            if (choices == null || choices.size() == 0) {
                throw new RuntimeException("No choices in OpenAI response");
            }
            
            JsonNode firstChoice = choices.get(0);
            JsonNode message = firstChoice.get("message");
            
            // Extract content
            List<ContentBlock> content = new ArrayList<>();
            JsonNode contentNode = message.get("content");
            if (contentNode != null && !contentNode.isNull()) {
                TextBlock textBlock = new TextBlock(contentNode.asText());
                content.add(textBlock);
            }
            
            // Extract usage information
            ChatUsage usage = null;
            JsonNode usageNode = responseJson.get("usage");
            if (usageNode != null) {
                int inputTokens = usageNode.get("prompt_tokens").asInt();
                int outputTokens = usageNode.get("completion_tokens").asInt();
                usage = new ChatUsage(inputTokens, outputTokens, timeUsed);
            }
            
            // Create response
            String responseId = responseJson.get("id").asText();
            String createdAt = String.valueOf(responseJson.get("created").asLong());
            
            return new ChatResponse(content, responseId, createdAt, usage, null);
            
        } catch (Exception e) {
            logger.error("Error parsing OpenAI response", e);
            throw new RuntimeException("Failed to parse OpenAI response", e);
        }
    }
    
    // Getters and Setters for configuration
    public String getApiKey() {
        return apiKey;
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    public long getTimeout() {
        return timeout;
    }
    
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
}