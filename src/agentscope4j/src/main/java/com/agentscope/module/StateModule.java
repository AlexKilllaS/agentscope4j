package com.agentscope.module;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for state management in AgentScope4J.
 * Provides state serialization and deserialization capabilities.
 * 
 * @author Alex Huangfu
 * @version 1.0.0
 */
@Component
public abstract class StateModule {
    
    private static final Logger logger = LoggerFactory.getLogger(StateModule.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Get the current state of the module as a dictionary.
     * 
     * @return Map containing the current state
     */
    public Map<String, Object> stateDict() {
        try {
            // Convert the object to a Map using Jackson
            return objectMapper.convertValue(this, Map.class);
        } catch (Exception e) {
            logger.error("Error serializing state for {}", getClass().getSimpleName(), e);
            return new HashMap<>();
        }
    }
    
    /**
     * Load state from a dictionary.
     * 
     * @param stateDict Map containing the state to load
     */
    public void loadStateDict(Map<String, Object> stateDict) {
        try {
            // Update the current object with values from the state dictionary
            Object updatedObject = objectMapper.convertValue(stateDict, this.getClass());
            copyStateFrom(updatedObject);
        } catch (Exception e) {
            logger.error("Error loading state for {}", getClass().getSimpleName(), e);
        }
    }
    
    /**
     * Copy state from another object of the same type.
     * This method should be overridden by subclasses to provide
     * specific state copying logic.
     * 
     * @param other The object to copy state from
     */
    protected void copyStateFrom(Object other) {
        // Default implementation - subclasses should override this
        logger.warn("Default copyStateFrom called for {}. Consider overriding this method.", 
                   getClass().getSimpleName());
    }
    
    /**
     * Create a deep copy of the current state.
     * 
     * @return Map containing a deep copy of the current state
     */
    public Map<String, Object> deepCopyState() {
        try {
            Map<String, Object> state = stateDict();
            // Create a deep copy by serializing and deserializing
            String json = objectMapper.writeValueAsString(state);
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            logger.error("Error creating deep copy of state for {}", getClass().getSimpleName(), e);
            return new HashMap<>();
        }
    }
    
    /**
     * Reset the module to its initial state.
     * This method should be overridden by subclasses.
     */
    public void reset() {
        logger.info("Reset called for {}. Override this method for custom reset logic.", 
                   getClass().getSimpleName());
    }
    
    /**
     * Check if the module has been initialized properly.
     * 
     * @return true if the module is properly initialized
     */
    public boolean isInitialized() {
        return true; // Default implementation
    }
    
    /**
     * Get a summary of the current state for logging/debugging.
     * 
     * @return String summary of the current state
     */
    public String getStateSummary() {
        try {
            Map<String, Object> state = stateDict();
            return String.format("%s{stateKeys=%s}", 
                               getClass().getSimpleName(), 
                               state.keySet());
        } catch (Exception e) {
            return String.format("%s{error=%s}", 
                               getClass().getSimpleName(), 
                               e.getMessage());
        }
    }
}