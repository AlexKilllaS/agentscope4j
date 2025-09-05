package com.agentscope.example;

import com.agentscope.agent.ReActAgent;
import com.agentscope.config.AgentScopeConfig;
import com.agentscope.formatter.OpenAIFormatter;
import com.agentscope.logging.AgentScopeLogger;
import com.agentscope.memory.InMemoryMemory;
import com.agentscope.message.Msg;
import com.agentscope.model.OpenAIChatModel;
import com.agentscope.tool.Toolkit;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

/**
 * Simple example demonstrating how to use AgentScope4J.
 * This example creates a basic conversational agent using OpenAI.
 * 
 * @author Alex Huangfu
 * @version 1.0.0
 */
@Component
public class SimpleAgentExample implements CommandLineRunner {
    
    private static final Logger logger = AgentScopeLogger.getLogger(SimpleAgentExample.class);
    
    @Autowired
    private AgentScopeConfig config;
    
    @Override
    public void run(String... args) throws Exception {
        // Check if we should run the example
        if (args.length > 0 && "--run-example".equals(args[0])) {
            runSimpleAgentExample();
        }
    }
    
    /**
     * Run a simple agent conversation example
     */
    public void runSimpleAgentExample() {
        try {
            logger.info("Starting Simple Agent Example");
            
            // Set up logging context
            AgentScopeLogger.setSessionContext("example-session-" + System.currentTimeMillis());
            
            // Check if OpenAI API key is configured
            String apiKey = config.getModels().getOpenai().getApiKey();
            if (apiKey == null || apiKey.isEmpty()) {
                logger.warn("OpenAI API key not configured. Please set OPENAI_API_KEY environment variable.");
                logger.info("Running in demo mode with mock responses.");
                runDemoMode();
                return;
            }
            
            // Create components
            OpenAIChatModel model = createModel();
            OpenAIFormatter formatter = new OpenAIFormatter();
            InMemoryMemory memory = new InMemoryMemory(100, true);
            Toolkit toolkit = createToolkit();
            
            // Create agent
            ReActAgent agent = new ReActAgent(
                "Assistant",
                "You are a helpful AI assistant. You can help users with various tasks and answer questions.",
                model,
                formatter,
                toolkit,
                memory,
                null, // No long-term memory for this example
                "static_control",
                false, // No meta tools
                false, // No parallel tool calls
                5 // Max 5 iterations
            );
            
            logger.info("Agent created successfully: {}", agent.getName());
            
            // Start conversation loop
            startConversationLoop(agent);
            
        } catch (Exception e) {
            logger.error("Error running simple agent example", e);
        } finally {
            AgentScopeLogger.clearContext();
        }
    }
    
    /**
     * Create and configure the OpenAI model
     */
    private OpenAIChatModel createModel() {
        OpenAIChatModel model = new OpenAIChatModel("gpt-3.5-turbo", false);
        model.setApiKey(config.getModels().getOpenai().getApiKey());
        model.setBaseUrl(config.getModels().getOpenai().getBaseUrl());
        model.setTimeout(config.getModels().getOpenai().getTimeout());
        
        logger.info("Created OpenAI model: {}", model.getModelName());
        return model;
    }
    
    /**
     * Create and configure the toolkit with example tools
     */
    private Toolkit createToolkit() {
        Toolkit toolkit = new Toolkit();
        
        // Add a simple calculator tool
        toolkit.registerTool(
            "calculator",
            input -> {
                try {
                    String expression = (String) input.get("expression");
                    double result = evaluateSimpleExpression(expression);
                    return com.agentscope.tool.ToolResponse.success("Result: " + result);
                } catch (Exception e) {
                    return com.agentscope.tool.ToolResponse.error("Invalid expression: " + e.getMessage());
                }
            },
            "Simple calculator that can evaluate basic mathematical expressions",
            java.util.Map.of(
                "type", "object",
                "properties", java.util.Map.of(
                    "expression", java.util.Map.of(
                        "type", "string",
                        "description", "Mathematical expression to evaluate (e.g., '2 + 3 * 4')"
                    )
                ),
                "required", java.util.List.of("expression")
            )
        );
        
        // Add a weather tool (mock implementation)
        toolkit.registerTool(
            "get_weather",
            input -> {
                String location = (String) input.get("location");
                // Mock weather data
                String weather = String.format("The weather in %s is sunny with a temperature of 22°C", location);
                return com.agentscope.tool.ToolResponse.success(weather);
            },
            "Get current weather information for a location",
            java.util.Map.of(
                "type", "object",
                "properties", java.util.Map.of(
                    "location", java.util.Map.of(
                        "type", "string",
                        "description", "The location to get weather for"
                    )
                ),
                "required", java.util.List.of("location")
            )
        );
        
        logger.info("Created toolkit with {} tools", toolkit.getToolNames().size());
        return toolkit;
    }
    
    /**
     * Start an interactive conversation loop
     */
    private void startConversationLoop(ReActAgent agent) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("\n=== AgentScope4J Simple Agent Example ===");
        System.out.println("You can now chat with the agent. Type 'quit' to exit.");
        System.out.println("Available tools: calculator, get_weather, echo, get_current_time");
        System.out.println("============================================\n");
        
        while (true) {
            System.out.print("You: ");
            String userInput = scanner.nextLine().trim();
            
            if ("quit".equalsIgnoreCase(userInput)) {
                System.out.println("Goodbye!");
                break;
            }
            
            if (userInput.isEmpty()) {
                continue;
            }
            
            try {
                // Create user message
                Msg userMessage = new Msg("user", userInput, "user");
                
                // Get agent response
                CompletableFuture<Msg> responseFuture = agent.reply(userMessage);
                Msg response = responseFuture.get();
                
                // Print response
                String responseText = response.getTextContent();
                if (responseText != null && !responseText.isEmpty()) {
                    System.out.println("Assistant: " + responseText);
                } else {
                    System.out.println("Assistant: [No text response]");
                }
                
                System.out.println();
                
            } catch (Exception e) {
                logger.error("Error processing user input", e);
                System.out.println("Sorry, I encountered an error processing your request.");
            }
        }
        
        scanner.close();
    }
    
    /**
     * Run in demo mode without actual API calls
     */
    private void runDemoMode() {
        System.out.println("\n=== AgentScope4J Demo Mode ===");
        System.out.println("This is a demonstration of AgentScope4J structure.");
        System.out.println("To run with actual AI models, configure your API keys.");
        System.out.println("==============================\n");
        
        // Demonstrate component creation
        logger.info("Creating demo components...");
        
        InMemoryMemory memory = new InMemoryMemory();
        Toolkit toolkit = new Toolkit();
        
        // Add some demo messages
        memory.addMessage(new Msg("user", "Hello, how are you?", "user"));
        memory.addMessage(new Msg("assistant", "I'm doing well, thank you! How can I help you today?", "assistant"));
        
        logger.info("Memory stats: {}", memory.getMemoryStats());
        logger.info("Available tools: {}", toolkit.getToolNames());
        
        // Test tool execution
        var echoResult = toolkit.executeTool("echo", java.util.Map.of("message", "Hello from demo!"));
        logger.info("Echo tool result: {}", echoResult.getContent());
        
        var timeResult = toolkit.executeTool("get_current_time", java.util.Map.of());
        logger.info("Time tool result: {}", timeResult.getContent());
        
        System.out.println("Demo completed successfully!");
    }
    
    /**
     * Simple expression evaluator for the calculator tool
     */
    private double evaluateSimpleExpression(String expression) {
        // This is a very basic evaluator - in a real application, 
        // you would use a proper expression parser
        expression = expression.replaceAll("\\s+", "");
        
        // Handle simple operations
        if (expression.contains("+")) {
            String[] parts = expression.split("\\+");
            return Double.parseDouble(parts[0]) + Double.parseDouble(parts[1]);
        } else if (expression.contains("-")) {
            String[] parts = expression.split("-");
            return Double.parseDouble(parts[0]) - Double.parseDouble(parts[1]);
        } else if (expression.contains("*")) {
            String[] parts = expression.split("\\*");
            return Double.parseDouble(parts[0]) * Double.parseDouble(parts[1]);
        } else if (expression.contains("/")) {
            String[] parts = expression.split("/");
            return Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
        } else {
            return Double.parseDouble(expression);
        }
    }
}