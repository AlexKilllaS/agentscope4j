package com.agentscope.formatter;

import com.agentscope.message.Msg;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Base class for message formatters in AgentScope4J.
 * Formatters are responsible for converting messages between AgentScope format
 * and the specific format required by different model APIs.
 * 
 * @author Alex Huangfu
 * @version 1.0.0
 */
@Component
public abstract class FormatterBase {
    
    /**
     * Format messages for the specific model API.
     * 
     * @param messages List of messages to format
     * @return Formatted messages ready for the model API
     */
    public abstract Object format(List<Msg> messages);
    
    /**
     * Format messages with additional parameters.
     * 
     * @param messages List of messages to format
     * @param kwargs Additional formatting parameters
     * @return Formatted messages ready for the model API
     */
    public Object format(List<Msg> messages, Map<String, Object> kwargs) {
        return format(messages);
    }
    
    /**
     * Parse response from the model API back to AgentScope format.
     * 
     * @param response The response from the model API
     * @return Parsed message in AgentScope format
     */
    public abstract Msg parseResponse(Object response);
    
    /**
     * Get the maximum number of tokens supported by this formatter.
     * 
     * @return Maximum token count, or -1 if unlimited
     */
    public int getMaxTokens() {
        return -1; // Unlimited by default
    }
    
    /**
     * Check if the formatter supports streaming responses.
     * 
     * @return true if streaming is supported
     */
    public boolean supportsStreaming() {
        return false; // Not supported by default
    }
    
    /**
     * Check if the formatter supports tool calls.
     * 
     * @return true if tool calls are supported
     */
    public boolean supportsToolCalls() {
        return false; // Not supported by default
    }
    
    /**
     * Check if the formatter supports multimodal content (images, audio, etc.).
     * 
     * @return true if multimodal content is supported
     */
    public boolean supportsMultimodal() {
        return false; // Not supported by default
    }
    
    /**
     * Validate that the messages are compatible with this formatter.
     * 
     * @param messages List of messages to validate
     * @throws IllegalArgumentException if messages are not compatible
     */
    public void validateMessages(List<Msg> messages) {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("Messages cannot be null or empty");
        }
        
        for (Msg msg : messages) {
            validateMessage(msg);
        }
    }
    
    /**
     * Validate a single message.
     * 
     * @param msg The message to validate
     * @throws IllegalArgumentException if the message is not compatible
     */
    protected void validateMessage(Msg msg) {
        if (msg == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        
        if (msg.getRole() == null || msg.getRole().isEmpty()) {
            throw new IllegalArgumentException("Message role cannot be null or empty");
        }
        
        if (msg.getContent() == null) {
            throw new IllegalArgumentException("Message content cannot be null");
        }
        
        // Check multimodal support
        if (!supportsMultimodal()) {
            List<Map<String, Object>> imageBlocks = msg.getContentBlocks("image");
            List<Map<String, Object>> audioBlocks = msg.getContentBlocks("audio");
            List<Map<String, Object>> videoBlocks = msg.getContentBlocks("video");
            
            if (!imageBlocks.isEmpty() || !audioBlocks.isEmpty() || !videoBlocks.isEmpty()) {
                throw new IllegalArgumentException(
                    "This formatter does not support multimodal content");
            }
        }
        
        // Check tool call support
        if (!supportsToolCalls()) {
            List<Map<String, Object>> toolUseBlocks = msg.getContentBlocks("tool_use");
            List<Map<String, Object>> toolResultBlocks = msg.getContentBlocks("tool_result");
            
            if (!toolUseBlocks.isEmpty() || !toolResultBlocks.isEmpty()) {
                throw new IllegalArgumentException(
                    "This formatter does not support tool calls");
            }
        }
    }
    
    /**
     * Truncate messages if they exceed the maximum token limit.
     * This is a simplified implementation - subclasses should override
     * for more sophisticated truncation strategies.
     * 
     * @param messages List of messages to truncate
     * @return Truncated list of messages
     */
    protected List<Msg> truncateMessages(List<Msg> messages) {
        int maxTokens = getMaxTokens();
        if (maxTokens <= 0) {
            return messages; // No truncation needed
        }
        
        // Simple truncation: remove oldest messages until under limit
        // This is a basic implementation - real tokenization would be more accurate
        int estimatedTokens = estimateTokenCount(messages);
        
        if (estimatedTokens <= maxTokens) {
            return messages;
        }
        
        // Remove messages from the beginning (keeping system messages)
        List<Msg> truncated = new java.util.ArrayList<>(messages);
        
        while (estimateTokenCount(truncated) > maxTokens && truncated.size() > 1) {
            // Keep system messages, remove others from the beginning
            boolean removed = false;
            for (int i = 0; i < truncated.size(); i++) {
                if (!"system".equals(truncated.get(i).getRole())) {
                    truncated.remove(i);
                    removed = true;
                    break;
                }
            }
            
            if (!removed) {
                break; // Only system messages left
            }
        }
        
        return truncated;
    }
    
    /**
     * Estimate the token count for a list of messages.
     * This is a rough estimation - subclasses should override for more accuracy.
     * 
     * @param messages List of messages
     * @return Estimated token count
     */
    protected int estimateTokenCount(List<Msg> messages) {
        int totalTokens = 0;
        
        for (Msg msg : messages) {
            String textContent = msg.getTextContent();
            if (textContent != null) {
                // Rough estimation: 1 token per 4 characters
                totalTokens += textContent.length() / 4;
            }
            
            // Add overhead for message structure
            totalTokens += 10;
        }
        
        return totalTokens;
    }
    
    /**
     * Get the name/identifier of this formatter.
     * 
     * @return Formatter name
     */
    public String getFormatterName() {
        return getClass().getSimpleName();
    }
    
    @Override
    public String toString() {
        return String.format("%s{maxTokens=%d, supportsStreaming=%s, supportsToolCalls=%s, supportsMultimodal=%s}",
                           getFormatterName(),
                           getMaxTokens(),
                           supportsStreaming(),
                           supportsToolCalls(),
                           supportsMultimodal());
    }
}