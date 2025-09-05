package com.agentscope.model;

import com.agentscope.message.Msg;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Base class for chat models in AgentScope4J.
 * 
 * @author Alex Huangfu
 * @version 1.0.0
 */
@Component
public abstract class ChatModelBase {
    
    protected String modelName;
    protected boolean stream;
    
    private static final List<String> TOOL_CHOICE_MODES = 
        Arrays.asList("auto", "none", "any", "required");
    
    /**
     * Initialize the chat model base class.
     * 
     * @param modelName The name of the model
     * @param stream Whether the model output is streaming or not
     */
    public ChatModelBase(String modelName, boolean stream) {
        this.modelName = modelName;
        this.stream = stream;
    }
    
    /**
     * Default constructor for Spring
     */
    public ChatModelBase() {
        this.stream = false;
    }
    
    /**
     * Abstract method to be implemented by concrete model classes.
     * 
     * @param messages List of messages to send to the model
     * @param tools Optional list of tools available to the model
     * @param toolChoice Tool choice mode or function name
     * @param kwargs Additional parameters
     * @return ChatResponse or Stream of ChatResponse for streaming
     */
    public abstract Object call(
        List<Msg> messages,
        List<Map<String, Object>> tools,
        String toolChoice,
        Map<String, Object> kwargs
    );
    
    /**
     * Async version of the call method.
     * 
     * @param messages List of messages to send to the model
     * @param tools Optional list of tools available to the model
     * @param toolChoice Tool choice mode or function name
     * @param kwargs Additional parameters
     * @return CompletableFuture of ChatResponse or Stream
     */
    public CompletableFuture<Object> callAsync(
        List<Msg> messages,
        List<Map<String, Object>> tools,
        String toolChoice,
        Map<String, Object> kwargs
    ) {
        return CompletableFuture.supplyAsync(() -> 
            call(messages, tools, toolChoice, kwargs)
        );
    }
    
    /**
     * Validate tool_choice parameter.
     * 
     * @param toolChoice Tool choice mode or function name
     * @param tools Available tools list
     * @throws IllegalArgumentException If tool_choice is invalid
     */
    protected void validateToolChoice(String toolChoice, List<Map<String, Object>> tools) {
        if (toolChoice == null) {
            return;
        }
        
        if (!(toolChoice instanceof String)) {
            throw new IllegalArgumentException(
                String.format("tool_choice must be String, got %s", 
                             toolChoice.getClass().getSimpleName())
            );
        }
        
        if (TOOL_CHOICE_MODES.contains(toolChoice)) {
            return;
        }
        
        if (tools != null && !tools.isEmpty()) {
            List<String> availableFunctions = tools.stream()
                .filter(tool -> tool.containsKey("function"))
                .map(tool -> (Map<String, Object>) tool.get("function"))
                .filter(function -> function.containsKey("name"))
                .map(function -> (String) function.get("name"))
                .toList();
            
            if (!availableFunctions.contains(toolChoice)) {
                throw new IllegalArgumentException(
                    String.format("Invalid tool_choice '%s'. Available options: %s", 
                                 toolChoice, 
                                 String.join(", ", TOOL_CHOICE_MODES) + ", " + 
                                 String.join(", ", availableFunctions))
                );
            }
        } else {
            throw new IllegalArgumentException(
                String.format("Invalid tool_choice '%s'. Available options: %s", 
                             toolChoice, String.join(", ", TOOL_CHOICE_MODES))
            );
        }
    }
    
    // Getters and Setters
    public String getModelName() {
        return modelName;
    }
    
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
    
    public boolean isStream() {
        return stream;
    }
    
    public void setStream(boolean stream) {
        this.stream = stream;
    }
    
    @Override
    public String toString() {
        return String.format("%s{modelName='%s', stream=%s}", 
                           getClass().getSimpleName(), modelName, stream);
    }
}