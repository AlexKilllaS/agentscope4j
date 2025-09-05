package com.agentscope.model;

import com.agentscope.message.ContentBlock;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The response of chat models in AgentScope4J.
 * 
 * @author Alex Huangfu
 * @version 1.0.0
 */
@Component
public class ChatResponse {
    
    @JsonProperty("content")
    private List<ContentBlock> content;
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("created_at")
    private String createdAt;
    
    @JsonProperty("type")
    private String type = "chat";
    
    @JsonProperty("usage")
    private ChatUsage usage;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    private static final DateTimeFormatter TIMESTAMP_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    /**
     * Default constructor
     */
    public ChatResponse() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        this.type = "chat";
    }
    
    /**
     * Constructor with content
     * 
     * @param content The content of the chat response
     */
    public ChatResponse(List<ContentBlock> content) {
        this();
        this.content = content;
    }
    
    /**
     * Full constructor
     * 
     * @param content The content of the chat response
     * @param id The unique identifier
     * @param createdAt When the response was created
     * @param usage The usage information
     * @param metadata The metadata of the response
     */
    public ChatResponse(
            List<ContentBlock> content,
            String id,
            String createdAt,
            ChatUsage usage,
            Map<String, Object> metadata) {
        
        this.content = content;
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now().format(TIMESTAMP_FORMAT);
        this.type = "chat";
        this.usage = usage;
        this.metadata = metadata;
    }
    
    /**
     * Convert to dictionary for JSON serialization
     * 
     * @return Map representation of the response
     */
    public Map<String, Object> toDict() {
        return Map.of(
            "content", content,
            "id", id,
            "created_at", createdAt,
            "type", type,
            "usage", usage != null ? usage.toDict() : null,
            "metadata", metadata
        );
    }
    
    /**
     * Create ChatResponse from dictionary
     * 
     * @param dict Map containing response data
     * @return New ChatResponse object
     */
    @SuppressWarnings("unchecked")
    public static ChatResponse fromDict(Map<String, Object> dict) {
        ChatResponse response = new ChatResponse();
        
        response.content = (List<ContentBlock>) dict.get("content");
        response.id = (String) dict.get("id");
        response.createdAt = (String) dict.get("created_at");
        response.type = (String) dict.getOrDefault("type", "chat");
        
        if (dict.containsKey("usage") && dict.get("usage") != null) {
            response.usage = ChatUsage.fromDict((Map<String, Object>) dict.get("usage"));
        }
        
        response.metadata = (Map<String, Object>) dict.get("metadata");
        
        return response;
    }
    
    // Getters and Setters
    public List<ContentBlock> getContent() {
        return content;
    }
    
    public void setContent(List<ContentBlock> content) {
        this.content = content;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public ChatUsage getUsage() {
        return usage;
    }
    
    public void setUsage(ChatUsage usage) {
        this.usage = usage;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    @Override
    public String toString() {
        return String.format("ChatResponse{id='%s', type='%s', createdAt='%s'}", 
                           id, type, createdAt);
    }
}