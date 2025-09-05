package com.agentscope.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Toolkit class for managing and executing tool functions in AgentScope4J.
 * 
 * @author Alex Huangfu
 * @version 1.0.0
 */
@Component
public class Toolkit {
    
    private static final Logger logger = LoggerFactory.getLogger(Toolkit.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // Registered tool functions
    private final Map<String, ToolFunction> tools = new ConcurrentHashMap<>();
    
    // Tool execution timeout in milliseconds
    private long executionTimeout = 30000;
    
    // Whether to enable async execution
    private boolean enableAsync = true;
    
    /**
     * Default constructor
     */
    public Toolkit() {
        // Register some built-in tools
        registerBuiltinTools();
    }
    
    /**
     * Register a tool function
     * 
     * @param name The name of the tool
     * @param function The function to execute
     * @param description Description of the tool
     * @param parameters Parameter schema for the tool
     */
    public void registerTool(
            String name, 
            Function<Map<String, Object>, ToolResponse> function,
            String description,
            Map<String, Object> parameters) {
        
        ToolFunction toolFunction = new ToolFunction(name, function, description, parameters);
        tools.put(name, toolFunction);
        logger.info("Registered tool: {}", name);
    }
    
    /**
     * Register a tool function with automatic parameter detection
     * 
     * @param name The name of the tool
     * @param function The function to execute
     * @param description Description of the tool
     */
    public void registerTool(
            String name,
            Function<Map<String, Object>, ToolResponse> function,
            String description) {
        
        // Auto-generate parameter schema (simplified)
        Map<String, Object> parameters = generateParameterSchema(function);
        registerTool(name, function, description, parameters);
    }
    
    /**
     * Register a tool from a method using reflection
     * 
     * @param object The object containing the method
     * @param methodName The name of the method
     * @param toolName The name to register the tool as (optional)
     */
    public void registerToolFromMethod(Object object, String methodName, String toolName) {
        try {
            Method[] methods = object.getClass().getMethods();
            Method targetMethod = null;
            
            for (Method method : methods) {
                if (method.getName().equals(methodName)) {
                    targetMethod = method;
                    break;
                }
            }
            
            if (targetMethod == null) {
                throw new IllegalArgumentException("Method not found: " + methodName);
            }
            
            String name = toolName != null ? toolName : methodName;
            String description = "Tool function: " + methodName;
            
            // Create parameter schema from method parameters
            Map<String, Object> parameters = createParameterSchemaFromMethod(targetMethod);
            
            // Create function wrapper
            Function<Map<String, Object>, ToolResponse> function = createMethodWrapper(object, targetMethod);
            
            registerTool(name, function, description, parameters);
            
        } catch (Exception e) {
            logger.error("Error registering tool from method: {}", methodName, e);
            throw new RuntimeException("Failed to register tool from method", e);
        }
    }
    
    /**
     * Execute a tool function
     * 
     * @param toolName The name of the tool to execute
     * @param input The input parameters for the tool
     * @return ToolResponse containing the result
     */
    public ToolResponse executeTool(String toolName, Map<String, Object> input) {
        ToolFunction toolFunction = tools.get(toolName);
        if (toolFunction == null) {
            return ToolResponse.error("Tool not found: " + toolName);
        }
        
        try {
            logger.debug("Executing tool: {} with input: {}", toolName, input);
            
            if (enableAsync) {
                CompletableFuture<ToolResponse> future = CompletableFuture
                    .supplyAsync(() -> toolFunction.getFunction().apply(input))
                    .orTimeout(executionTimeout, java.util.concurrent.TimeUnit.MILLISECONDS);
                
                return future.get();
            } else {
                return toolFunction.getFunction().apply(input);
            }
            
        } catch (Exception e) {
            logger.error("Error executing tool: {}", toolName, e);
            return ToolResponse.error("Tool execution failed: " + e.getMessage());
        }
    }
    
    /**
     * Get the schema for a specific tool
     * 
     * @param toolName The name of the tool
     * @return Tool schema as a Map
     */
    public Map<String, Object> getToolSchema(String toolName) {
        ToolFunction toolFunction = tools.get(toolName);
        if (toolFunction == null) {
            return null;
        }
        
        return Map.of(
            "type", "function",
            "function", Map.of(
                "name", toolFunction.getName(),
                "description", toolFunction.getDescription(),
                "parameters", toolFunction.getParameters()
            )
        );
    }
    
    /**
     * Get schemas for all registered tools
     * 
     * @return List of tool schemas
     */
    public List<Map<String, Object>> getToolSchemas() {
        List<Map<String, Object>> schemas = new ArrayList<>();
        for (String toolName : tools.keySet()) {
            schemas.add(getToolSchema(toolName));
        }
        return schemas;
    }
    
    /**
     * Remove a tool from the toolkit
     * 
     * @param toolName The name of the tool to remove
     * @return true if the tool was removed, false if it didn't exist
     */
    public boolean removeTool(String toolName) {
        ToolFunction removed = tools.remove(toolName);
        if (removed != null) {
            logger.info("Removed tool: {}", toolName);
            return true;
        }
        return false;
    }
    
    /**
     * Get all registered tool names
     * 
     * @return Set of tool names
     */
    public Set<String> getToolNames() {
        return new HashSet<>(tools.keySet());
    }
    
    /**
     * Check if a tool is registered
     * 
     * @param toolName The name of the tool
     * @return true if the tool is registered
     */
    public boolean hasTool(String toolName) {
        return tools.containsKey(toolName);
    }
    
    /**
     * Clear all registered tools
     */
    public void clearTools() {
        tools.clear();
        logger.info("Cleared all tools from toolkit");
    }
    
    /**
     * Register built-in tools
     */
    private void registerBuiltinTools() {
        // Echo tool for testing
        registerTool("echo", 
            input -> ToolResponse.success(input.get("message")),
            "Echo the input message",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "message", Map.of(
                        "type", "string",
                        "description", "The message to echo"
                    )
                ),
                "required", List.of("message")
            )
        );
        
        // Current time tool
        registerTool("get_current_time",
            input -> ToolResponse.success(new Date().toString()),
            "Get the current date and time",
            Map.of(
                "type", "object",
                "properties", Map.of(),
                "required", List.of()
            )
        );
    }
    
    /**
     * Generate parameter schema for a function (simplified)
     */
    private Map<String, Object> generateParameterSchema(Function<Map<String, Object>, ToolResponse> function) {
        // Simplified implementation - in a real scenario, this would use reflection
        // or annotations to determine parameter types
        return Map.of(
            "type", "object",
            "properties", Map.of(),
            "required", List.of()
        );
    }
    
    /**
     * Create parameter schema from method reflection
     */
    private Map<String, Object> createParameterSchemaFromMethod(Method method) {
        Map<String, Object> properties = new HashMap<>();
        List<String> required = new ArrayList<>();
        
        Parameter[] parameters = method.getParameters();
        for (Parameter param : parameters) {
            String paramName = param.getName();
            String paramType = getJsonType(param.getType());
            
            properties.put(paramName, Map.of(
                "type", paramType,
                "description", "Parameter: " + paramName
            ));
            
            // Assume all parameters are required for simplicity
            required.add(paramName);
        }
        
        return Map.of(
            "type", "object",
            "properties", properties,
            "required", required
        );
    }
    
    /**
     * Create a function wrapper for a method
     */
    private Function<Map<String, Object>, ToolResponse> createMethodWrapper(Object object, Method method) {
        return input -> {
            try {
                Parameter[] parameters = method.getParameters();
                Object[] args = new Object[parameters.length];
                
                for (int i = 0; i < parameters.length; i++) {
                    String paramName = parameters[i].getName();
                    Object value = input.get(paramName);
                    
                    // Convert value to the expected type
                    args[i] = convertValue(value, parameters[i].getType());
                }
                
                Object result = method.invoke(object, args);
                
                if (result instanceof ToolResponse) {
                    return (ToolResponse) result;
                } else {
                    return ToolResponse.success(result);
                }
                
            } catch (Exception e) {
                logger.error("Error invoking method: {}", method.getName(), e);
                return ToolResponse.error("Method invocation failed: " + e.getMessage());
            }
        };
    }
    
    /**
     * Convert Java type to JSON schema type
     */
    private String getJsonType(Class<?> clazz) {
        if (clazz == String.class) {
            return "string";
        } else if (clazz == Integer.class || clazz == int.class || 
                   clazz == Long.class || clazz == long.class) {
            return "integer";
        } else if (clazz == Double.class || clazz == double.class ||
                   clazz == Float.class || clazz == float.class) {
            return "number";
        } else if (clazz == Boolean.class || clazz == boolean.class) {
            return "boolean";
        } else if (clazz.isArray() || List.class.isAssignableFrom(clazz)) {
            return "array";
        } else {
            return "object";
        }
    }
    
    /**
     * Convert value to the expected type
     */
    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }
        
        // Use Jackson for type conversion
        try {
            return objectMapper.convertValue(value, targetType);
        } catch (Exception e) {
            logger.warn("Failed to convert value {} to type {}", value, targetType.getSimpleName());
            return value;
        }
    }
    
    // Getters and Setters
    public long getExecutionTimeout() {
        return executionTimeout;
    }
    
    public void setExecutionTimeout(long executionTimeout) {
        this.executionTimeout = executionTimeout;
    }
    
    public boolean isEnableAsync() {
        return enableAsync;
    }
    
    public void setEnableAsync(boolean enableAsync) {
        this.enableAsync = enableAsync;
    }
    
    /**
     * Inner class representing a tool function
     */
    private static class ToolFunction {
        private final String name;
        private final Function<Map<String, Object>, ToolResponse> function;
        private final String description;
        private final Map<String, Object> parameters;
        
        public ToolFunction(
                String name,
                Function<Map<String, Object>, ToolResponse> function,
                String description,
                Map<String, Object> parameters) {
            this.name = name;
            this.function = function;
            this.description = description;
            this.parameters = parameters;
        }
        
        public String getName() {
            return name;
        }
        
        public Function<Map<String, Object>, ToolResponse> getFunction() {
            return function;
        }
        
        public String getDescription() {
            return description;
        }
        
        public Map<String, Object> getParameters() {
            return parameters;
        }
    }
}