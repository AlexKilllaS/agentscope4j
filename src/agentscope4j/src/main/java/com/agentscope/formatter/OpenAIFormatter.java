package com.agentscope.formatter;

import com.agentscope.message.Msg;
import com.agentscope.message.TextBlock;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * OpenAI API formatter for AgentScope4J.
 * Converts messages between AgentScope format and OpenAI API format.
 * 
 * @author Alex Huangfu
 * @version 1.0.0
 */
@Component
public class OpenAIFormatter extends FormatterBase {
    
    private int maxTokens = 4096; // Default for GPT-3.5-turbo
    
    /**
     * Default constructor
     */
    public OpenAIFormatter() {
        super();
    }
    
    /**
     * Constructor with custom max tokens
     * 
     * @param maxTokens Maximum number of tokens
     */
    public OpenAIFormatter(int maxTokens) {
        super();
        this.maxTokens = maxTokens;
    }
    
    @Override
    public Object format(List<Msg> messages) {
        validateMessages(messages);
        
        List<Msg> processedMessages = truncateMessages(messages);
        List<Map<String, Object>> openaiMessages = new ArrayList<>();
        
        for (Msg msg : processedMessages) {
            Map<String, Object> openaiMsg = convertToOpenAIFormat(msg);
            if (openaiMsg != null) {
                openaiMessages.add(openaiMsg);
            }
        }
        
        return openaiMessages;
    }
    
    @Override
    public Object format(List<Msg> messages, Map<String, Object> kwargs) {
        // Handle additional OpenAI-specific parameters
        Object formatted = format(messages);
        
        if (kwargs != null && !kwargs.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("messages", formatted);
            
            // Add OpenAI-specific parameters
            if (kwargs.containsKey("temperature")) {
                result.put("temperature", kwargs.get("temperature"));
            }
            if (kwargs.containsKey("max_tokens")) {
                result.put("max_tokens", kwargs.get("max_tokens"));
            }
            if (kwargs.containsKey("top_p")) {
                result.put("top_p", kwargs.get("top_p"));
            }
            if (kwargs.containsKey("frequency_penalty")) {
                result.put("frequency_penalty", kwargs.get("frequency_penalty"));
            }
            if (kwargs.containsKey("presence_penalty")) {
                result.put("presence_penalty", kwargs.get("presence_penalty"));
            }
            if (kwargs.containsKey("tools")) {
                result.put("tools", kwargs.get("tools"));
            }
            if (kwargs.containsKey("tool_choice")) {
                result.put("tool_choice", kwargs.get("tool_choice"));
            }
            
            return result;
        }
        
        return formatted;
    }
    
    @Override
    public Msg parseResponse(Object response) {
        if (response == null) {
            throw new IllegalArgumentException("Response cannot be null");
        }
        
        try {
            if (response instanceof Map) {
                Map<?, ?> responseMap = (Map<?, ?>) response;
                return parseOpenAIResponse(responseMap);
            } else {
                throw new IllegalArgumentException("Invalid response format: " + response.getClass());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse OpenAI response", e);
        }
    }
    
    @Override
    public int getMaxTokens() {
        return maxTokens;
    }
    
    @Override
    public boolean supportsStreaming() {
        return true;
    }
    
    @Override
    public boolean supportsToolCalls() {
        return true;
    }
    
    @Override
    public boolean supportsMultimodal() {
        return true; // OpenAI supports vision models
    }
    
    /**
     * Convert AgentScope message to OpenAI format
     * 
     * @param msg AgentScope message
     * @return OpenAI formatted message
     */
    private Map<String, Object> convertToOpenAIFormat(Msg msg) {
        Map<String, Object> openaiMsg = new HashMap<>();
        
        // Set role
        openaiMsg.put("role", msg.getRole());
        
        // Handle content
        if (msg.getContent() instanceof String) {
            openaiMsg.put("content", msg.getContent());
        } else if (msg.getContent() instanceof List) {
            List<?> contentList = (List<?>) msg.getContent();
            List<Map<String, Object>> openaiContent = new ArrayList<>();
            
            for (Object block : contentList) {
                if (block instanceof Map) {
                    Map<?, ?> blockMap = (Map<?, ?>) block;
                    Map<String, Object> openaiBlock = convertContentBlock(blockMap);
                    if (openaiBlock != null) {
                        openaiContent.add(openaiBlock);
                    }
                }
            }
            
            if (!openaiContent.isEmpty()) {
                openaiMsg.put("content", openaiContent);
            } else {
                // Fallback to text content
                String textContent = msg.getTextContent();
                if (textContent != null) {
                    openaiMsg.put("content", textContent);
                }
            }
        }
        
        // Add name if present and role is not system
        if (msg.getName() != null && !msg.getName().isEmpty() && !"system".equals(msg.getRole())) {
            openaiMsg.put("name", msg.getName());
        }
        
        // Handle tool calls
        List<Map<String, Object>> toolUseBlocks = msg.getContentBlocks("tool_use");
        if (!toolUseBlocks.isEmpty()) {
            List<Map<String, Object>> toolCalls = new ArrayList<>();
            for (Map<String, Object> toolUse : toolUseBlocks) {
                Map<String, Object> toolCall = new HashMap<>();
                toolCall.put("id", toolUse.get("id"));
                toolCall.put("type", "function");
                toolCall.put("function", Map.of(
                    "name", toolUse.get("name"),
                    "arguments", toolUse.get("input")
                ));
                toolCalls.add(toolCall);
            }
            openaiMsg.put("tool_calls", toolCalls);
        }
        
        // Handle tool results
        List<Map<String, Object>> toolResultBlocks = msg.getContentBlocks("tool_result");
        if (!toolResultBlocks.isEmpty() && "tool".equals(msg.getRole())) {
            // For tool result messages, set role to "tool" and add tool_call_id
            openaiMsg.put("role", "tool");
            if (!toolResultBlocks.isEmpty()) {
                Map<String, Object> firstResult = toolResultBlocks.get(0);
                openaiMsg.put("tool_call_id", firstResult.get("id"));
                openaiMsg.put("content", firstResult.get("output"));
            }
        }
        
        return openaiMsg;
    }
    
    /**
     * Convert content block to OpenAI format
     * 
     * @param block Content block
     * @return OpenAI formatted content block
     */
    private Map<String, Object> convertContentBlock(Map<?, ?> block) {
        String type = (String) block.get("type");
        
        switch (type) {
            case "text":
                return Map.of(
                    "type", "text",
                    "text", block.get("text")
                );
                
            case "image":
                Map<?, ?> source = (Map<?, ?>) block.get("source");
                if (source != null) {
                    String sourceType = (String) source.get("type");
                    if ("url".equals(sourceType)) {
                        return Map.of(
                            "type", "image_url",
                            "image_url", Map.of("url", source.get("url"))
                        );
                    } else if ("base64".equals(sourceType)) {
                        String dataUrl = String.format("data:%s;base64,%s",
                                                      source.get("media_type"),
                                                      source.get("data"));
                        return Map.of(
                            "type", "image_url",
                            "image_url", Map.of("url", dataUrl)
                        );
                    }
                }
                break;
                
            case "thinking":
                // OpenAI doesn't have a specific thinking block, convert to text
                return Map.of(
                    "type", "text",
                    "text", "(thinking) " + block.get("thinking")
                );
                
            default:
                // Unsupported block type, skip
                return null;
        }
        
        return null;
    }
    
    /**
     * Parse OpenAI API response to AgentScope message
     * 
     * @param response OpenAI response
     * @return AgentScope message
     */
    private Msg parseOpenAIResponse(Map<?, ?> response) {
        Object choices = response.get("choices");
        if (!(choices instanceof List) || ((List<?>) choices).isEmpty()) {
            throw new IllegalArgumentException("No choices in OpenAI response");
        }
        
        List<?> choicesList = (List<?>) choices;
        Map<?, ?> firstChoice = (Map<?, ?>) choicesList.get(0);
        Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");
        
        if (message == null) {
            throw new IllegalArgumentException("No message in OpenAI choice");
        }
        
        String role = (String) message.get("role");
        Object content = message.get("content");
        
        // Handle tool calls
        List<Map<String, Object>> contentBlocks = new ArrayList<>();
        
        if (content != null) {
            if (content instanceof String) {
                TextBlock textBlock = new TextBlock((String) content);
                contentBlocks.add(Map.of(
                    "type", "text",
                    "text", content
                ));
            }
        }
        
        // Handle tool calls in response
        Object toolCalls = message.get("tool_calls");
        if (toolCalls instanceof List) {
            List<?> toolCallsList = (List<?>) toolCalls;
            for (Object toolCallObj : toolCallsList) {
                if (toolCallObj instanceof Map) {
                    Map<?, ?> toolCall = (Map<?, ?>) toolCallObj;
                    Map<?, ?> function = (Map<?, ?>) toolCall.get("function");
                    
                    if (function != null) {
                        contentBlocks.add(Map.of(
                            "type", "tool_use",
                            "id", toolCall.get("id"),
                            "name", function.get("name"),
                            "input", function.get("arguments")
                        ));
                    }
                }
            }
        }
        
        Object messageContent = contentBlocks.isEmpty() ? content : contentBlocks;
        
        return new Msg("assistant", messageContent, role != null ? role : "assistant");
    }
    
    /**
     * Set the maximum number of tokens
     * 
     * @param maxTokens Maximum tokens
     */
    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }
    
    @Override
    public String getFormatterName() {
        return "OpenAIFormatter";
    }
}