package com.agentscope.tool;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Response from tool execution in AgentScope4J.
 * 
 * @author Alex Huangfu
 * @version 1.0.0
 */
@Component
public class ToolResponse {
    
    @JsonProperty("content")
    private Object content;
    
    @JsonProperty("is_final")
    private boolean isFinal;
    
    @JsonProperty("timestamp")
    private String timestamp;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    private static final DateTimeFormatter TIMESTAMP_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    /**
     * Default constructor
     */
    public ToolResponse() {
        this.timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        this.isFinal = true;
    }
    
    /**
     * Constructor with content
     * 
     * @param content The content of the tool response
     */
    public ToolResponse(Object content) {
        this();
        this.content = content;
    }
    
    /**
     * Constructor with content and final flag
     * 
     * @param content The content of the tool response
     * @param isFinal Whether this is the final response
     */
    public ToolResponse(Object content, boolean isFinal) {
        this();
        this.content = content;
        this.isFinal = isFinal;
    }
    
    /**
     * Full constructor
     * 
     * @param content The content of the tool response
     * @param isFinal Whether this is the final response
     * @param metadata Additional metadata
     */
    public ToolResponse(Object content, boolean isFinal, Map<String, Object> metadata) {
        this();
        this.content = content;
        this.isFinal = isFinal;
        this.metadata = metadata;
    }
    
    /**
     * Create a successful tool response
     * 
     * @param content The response content
     * @return ToolResponse instance
     */
    public static ToolResponse success(Object content) {
        return new ToolResponse(content, true);
    }
    
    /**
     * Create an error tool response
     * 
     * @param errorMessage The error message
     * @return ToolResponse instance
     */
    public static ToolResponse error(String errorMessage) {
        ToolResponse response = new ToolResponse("Error: " + errorMessage, true);
        response.metadata = Map.of("error", true, "error_message", errorMessage);
        return response;
    }
    
    /**
     * Create a streaming tool response (not final)
     * 
     * @param content The partial content
     * @return ToolResponse instance
     */
    public static ToolResponse streaming(Object content) {
        return new ToolResponse(content, false);
    }
    
    /**
     * Convert to dictionary for JSON serialization
     * 
     * @return Map representation of the response
     */
    public Map<String, Object> toDict() {
        return Map.of(
            "content", content,
            "is_final", isFinal,
            "timestamp", timestamp,
            "metadata", metadata != null ? metadata : Map.of()
        );
    }
    
    /**
     * Create ToolResponse from dictionary
     * 
     * @param dict Map containing response data
     * @return New ToolResponse object
     */
    @SuppressWarnings("unchecked")
    public static ToolResponse fromDict(Map<String, Object> dict) {
        ToolResponse response = new ToolResponse();
        
        response.content = dict.get("content");
        response.isFinal = (Boolean) dict.getOrDefault("is_final", true);
        response.timestamp = (String) dict.getOrDefault("timestamp", 
                                                        LocalDateTime.now().format(TIMESTAMP_FORMAT));
        response.metadata = (Map<String, Object>) dict.get("metadata");
        
        return response;
    }
    
    /**
     * Check if this response indicates an error
     * 
     * @return true if this is an error response
     */
    public boolean isError() {
        return metadata != null && Boolean.TRUE.equals(metadata.get("error"));
    }
    
    /**
     * Get the content as a string
     * 
     * @return String representation of the content
     */
    public String getContentAsString() {
        return content != null ? content.toString() : "";
    }
    
    /**
     * Check if the response has content
     * 
     * @return true if content is not null and not empty
     */
    public boolean hasContent() {
        if (content == null) {
            return false;
        }
        if (content instanceof String) {
            return !((String) content).isEmpty();
        }
        return true;
    }
    
    // Getters and Setters
    public Object getContent() {
        return content;
    }
    
    public void setContent(Object content) {
        this.content = content;
    }
    
    public boolean isFinal() {
        return isFinal;
    }
    
    public void setFinal(boolean isFinal) {
        this.isFinal = isFinal;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    @Override
    public String toString() {
        return String.format("ToolResponse{content=%s, isFinal=%s, timestamp='%s'}", 
                           content, isFinal, timestamp);
    }
}