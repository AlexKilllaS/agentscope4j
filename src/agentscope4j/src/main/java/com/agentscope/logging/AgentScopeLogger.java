package com.agentscope.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced logging utility for AgentScope4J.
 * Provides structured logging with context and tracing capabilities.
 * 
 * @author Alex Huangfu
 * @version 1.0.0
 */
@Component
public class AgentScopeLogger {
    
    private static final DateTimeFormatter TIMESTAMP_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    // Context keys for MDC
    public static final String AGENT_ID = "agentId";
    public static final String AGENT_NAME = "agentName";
    public static final String SESSION_ID = "sessionId";
    public static final String REQUEST_ID = "requestId";
    public static final String OPERATION = "operation";
    public static final String MODEL_NAME = "modelName";
    public static final String TOOL_NAME = "toolName";
    
    // Performance tracking
    private final Map<String, Long> operationStartTimes = new ConcurrentHashMap<>();
    
    /**
     * Get logger for a specific class
     * 
     * @param clazz The class to get logger for
     * @return SLF4J Logger instance
     */
    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }
    
    /**
     * Get logger for a specific name
     * 
     * @param name The logger name
     * @return SLF4J Logger instance
     */
    public static Logger getLogger(String name) {
        return LoggerFactory.getLogger(name);
    }
    
    /**
     * Set agent context for logging
     * 
     * @param agentId The agent ID
     * @param agentName The agent name
     */
    public static void setAgentContext(String agentId, String agentName) {
        MDC.put(AGENT_ID, agentId);
        MDC.put(AGENT_NAME, agentName);
    }
    
    /**
     * Set session context for logging
     * 
     * @param sessionId The session ID
     */
    public static void setSessionContext(String sessionId) {
        MDC.put(SESSION_ID, sessionId);
    }
    
    /**
     * Set request context for logging
     * 
     * @param requestId The request ID
     */
    public static void setRequestContext(String requestId) {
        MDC.put(REQUEST_ID, requestId);
    }
    
    /**
     * Set operation context for logging
     * 
     * @param operation The operation name
     */
    public static void setOperationContext(String operation) {
        MDC.put(OPERATION, operation);
    }
    
    /**
     * Set model context for logging
     * 
     * @param modelName The model name
     */
    public static void setModelContext(String modelName) {
        MDC.put(MODEL_NAME, modelName);
    }
    
    /**
     * Set tool context for logging
     * 
     * @param toolName The tool name
     */
    public static void setToolContext(String toolName) {
        MDC.put(TOOL_NAME, toolName);
    }
    
    /**
     * Clear all MDC context
     */
    public static void clearContext() {
        MDC.clear();
    }
    
    /**
     * Clear specific context key
     * 
     * @param key The context key to clear
     */
    public static void clearContext(String key) {
        MDC.remove(key);
    }
    
    /**
     * Get current context as a map
     * 
     * @return Current MDC context
     */
    public static Map<String, String> getContext() {
        return MDC.getCopyOfContextMap();
    }
    
    /**
     * Set multiple context values at once
     * 
     * @param context Map of context key-value pairs
     */
    public static void setContext(Map<String, String> context) {
        if (context != null) {
            context.forEach(MDC::put);
        }
    }
    
    /**
     * Start timing an operation
     * 
     * @param operationId Unique identifier for the operation
     */
    public void startTiming(String operationId) {
        operationStartTimes.put(operationId, System.currentTimeMillis());
    }
    
    /**
     * End timing an operation and log the duration
     * 
     * @param operationId The operation identifier
     * @param logger The logger to use
     * @param message Additional message to log
     */
    public void endTiming(String operationId, Logger logger, String message) {
        Long startTime = operationStartTimes.remove(operationId);
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            logger.info("{} completed in {}ms", message, duration);
        } else {
            logger.warn("No start time found for operation: {}", operationId);
        }
    }
    
    /**
     * Log agent action with structured format
     * 
     * @param logger The logger to use
     * @param agentName The agent name
     * @param action The action being performed
     * @param details Additional details
     */
    public static void logAgentAction(Logger logger, String agentName, String action, String details) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        logger.info("[{}] Agent '{}' performing action: {} - {}", 
                   timestamp, agentName, action, details);
    }
    
    /**
     * Log model interaction
     * 
     * @param logger The logger to use
     * @param modelName The model name
     * @param operation The operation (call, stream, etc.)
     * @param inputTokens Number of input tokens
     * @param outputTokens Number of output tokens
     * @param duration Duration in milliseconds
     */
    public static void logModelInteraction(
            Logger logger, 
            String modelName, 
            String operation, 
            int inputTokens, 
            int outputTokens, 
            long duration) {
        
        setModelContext(modelName);
        setOperationContext(operation);
        
        logger.info("Model interaction: {} {} - Input: {} tokens, Output: {} tokens, Duration: {}ms",
                   modelName, operation, inputTokens, outputTokens, duration);
        
        clearContext(MODEL_NAME);
        clearContext(OPERATION);
    }
    
    /**
     * Log tool execution
     * 
     * @param logger The logger to use
     * @param toolName The tool name
     * @param success Whether execution was successful
     * @param duration Duration in milliseconds
     * @param error Error message if failed
     */
    public static void logToolExecution(
            Logger logger, 
            String toolName, 
            boolean success, 
            long duration, 
            String error) {
        
        setToolContext(toolName);
        
        if (success) {
            logger.info("Tool execution successful: {} completed in {}ms", toolName, duration);
        } else {
            logger.error("Tool execution failed: {} failed after {}ms - Error: {}", 
                        toolName, duration, error);
        }
        
        clearContext(TOOL_NAME);
    }
    
    /**
     * Log memory operation
     * 
     * @param logger The logger to use
     * @param operation The memory operation
     * @param messageCount Number of messages involved
     * @param memoryType Type of memory (short-term, long-term)
     */
    public static void logMemoryOperation(
            Logger logger, 
            String operation, 
            int messageCount, 
            String memoryType) {
        
        setOperationContext(operation);
        
        logger.debug("Memory operation: {} on {} - {} messages processed", 
                    operation, memoryType, messageCount);
        
        clearContext(OPERATION);
    }
    
    /**
     * Log error with context
     * 
     * @param logger The logger to use
     * @param message Error message
     * @param throwable The exception
     * @param context Additional context information
     */
    public static void logError(
            Logger logger, 
            String message, 
            Throwable throwable, 
            Map<String, String> context) {
        
        // Temporarily set context
        Map<String, String> originalContext = getContext();
        if (context != null) {
            setContext(context);
        }
        
        logger.error(message, throwable);
        
        // Restore original context
        clearContext();
        if (originalContext != null) {
            setContext(originalContext);
        }
    }
    
    /**
     * Log performance metrics
     * 
     * @param logger The logger to use
     * @param component The component name
     * @param metrics Map of metric name to value
     */
    public static void logPerformanceMetrics(
            Logger logger, 
            String component, 
            Map<String, Object> metrics) {
        
        StringBuilder metricsStr = new StringBuilder();
        metrics.forEach((key, value) -> 
            metricsStr.append(key).append("=").append(value).append(", "));
        
        if (metricsStr.length() > 0) {
            metricsStr.setLength(metricsStr.length() - 2); // Remove trailing ", "
        }
        
        logger.info("Performance metrics for {}: {}", component, metricsStr.toString());
    }
    
    /**
     * Create a structured log entry
     * 
     * @param level Log level
     * @param component Component name
     * @param operation Operation name
     * @param message Message
     * @param data Additional data
     * @return Formatted log entry
     */
    public static String createStructuredLogEntry(
            String level, 
            String component, 
            String operation, 
            String message, 
            Map<String, Object> data) {
        
        StringBuilder entry = new StringBuilder();
        entry.append("[").append(LocalDateTime.now().format(TIMESTAMP_FORMAT)).append("] ");
        entry.append("[").append(level.toUpperCase()).append("] ");
        entry.append("[").append(component).append("]");
        
        if (operation != null) {
            entry.append(" [").append(operation).append("]");
        }
        
        entry.append(" - ").append(message);
        
        if (data != null && !data.isEmpty()) {
            entry.append(" | Data: ").append(data);
        }
        
        return entry.toString();
    }
    
    /**
     * Log system startup information
     * 
     * @param logger The logger to use
     * @param version AgentScope version
     * @param config Configuration summary
     */
    public static void logSystemStartup(Logger logger, String version, Map<String, Object> config) {
        logger.info("=== AgentScope4J System Starting ===");
        logger.info("Version: {}", version);
        logger.info("Configuration: {}", config);
        logger.info("Startup time: {}", LocalDateTime.now().format(TIMESTAMP_FORMAT));
        logger.info("========================================");
    }
    
    /**
     * Log system shutdown information
     * 
     * @param logger The logger to use
     * @param uptime System uptime in milliseconds
     */
    public static void logSystemShutdown(Logger logger, long uptime) {
        logger.info("=== AgentScope4J System Shutting Down ===");
        logger.info("Uptime: {}ms", uptime);
        logger.info("Shutdown time: {}", LocalDateTime.now().format(TIMESTAMP_FORMAT));
        logger.info("===========================================");
    }
}