package com.agentscope.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * The usage of a chat model API invocation in AgentScope4J.
 * 
 * @author Alex Huangfu
 * @version 1.0.0
 */
@Component
public class ChatUsage {
    
    @JsonProperty("input_tokens")
    private int inputTokens;
    
    @JsonProperty("output_tokens")
    private int outputTokens;
    
    @JsonProperty("time")
    private double time;
    
    @JsonProperty("type")
    private String type = "chat";
    
    /**
     * Default constructor
     */
    public ChatUsage() {
        this.type = "chat";
    }
    
    /**
     * Constructor with token counts and time
     * 
     * @param inputTokens The number of input tokens
     * @param outputTokens The number of output tokens
     * @param time The time used in seconds
     */
    public ChatUsage(int inputTokens, int outputTokens, double time) {
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.time = time;
        this.type = "chat";
    }
    
    /**
     * Get total tokens used
     * 
     * @return Total number of tokens (input + output)
     */
    public int getTotalTokens() {
        return inputTokens + outputTokens;
    }
    
    /**
     * Convert to dictionary for JSON serialization
     * 
     * @return Map representation of the usage
     */
    public Map<String, Object> toDict() {
        return Map.of(
            "input_tokens", inputTokens,
            "output_tokens", outputTokens,
            "time", time,
            "type", type
        );
    }
    
    /**
     * Create ChatUsage from dictionary
     * 
     * @param dict Map containing usage data
     * @return New ChatUsage object
     */
    public static ChatUsage fromDict(Map<String, Object> dict) {
        ChatUsage usage = new ChatUsage();
        
        if (dict.containsKey("input_tokens")) {
            usage.inputTokens = ((Number) dict.get("input_tokens")).intValue();
        }
        
        if (dict.containsKey("output_tokens")) {
            usage.outputTokens = ((Number) dict.get("output_tokens")).intValue();
        }
        
        if (dict.containsKey("time")) {
            usage.time = ((Number) dict.get("time")).doubleValue();
        }
        
        if (dict.containsKey("type")) {
            usage.type = (String) dict.get("type");
        }
        
        return usage;
    }
    
    /**
     * Add usage from another ChatUsage object
     * 
     * @param other Another ChatUsage to add
     * @return New ChatUsage with combined values
     */
    public ChatUsage add(ChatUsage other) {
        if (other == null) {
            return new ChatUsage(this.inputTokens, this.outputTokens, this.time);
        }
        
        return new ChatUsage(
            this.inputTokens + other.inputTokens,
            this.outputTokens + other.outputTokens,
            this.time + other.time
        );
    }
    
    // Getters and Setters
    public int getInputTokens() {
        return inputTokens;
    }
    
    public void setInputTokens(int inputTokens) {
        this.inputTokens = inputTokens;
    }
    
    public int getOutputTokens() {
        return outputTokens;
    }
    
    public void setOutputTokens(int outputTokens) {
        this.outputTokens = outputTokens;
    }
    
    public double getTime() {
        return time;
    }
    
    public void setTime(double time) {
        this.time = time;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    @Override
    public String toString() {
        return String.format("ChatUsage{inputTokens=%d, outputTokens=%d, time=%.3f, type='%s'}", 
                           inputTokens, outputTokens, time, type);
    }
}