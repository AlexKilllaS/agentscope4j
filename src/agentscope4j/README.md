# AgentScope4J

AgentScope4J is a Java implementation of the AgentScope multi-agent framework, built with Spring Boot. It provides a comprehensive platform for building LLM-powered agent applications with support for multiple models, tools, memory management, and more.

## Features

- **Multi-Model Support**: OpenAI, DashScope, Gemini, and more
- **Agent Framework**: ReAct agents with reasoning and acting capabilities
- **Tool System**: Extensible tool registration and execution
- **Memory Management**: Short-term and long-term memory support
- **Message Formatting**: Support for different model API formats
- **Configuration Management**: Flexible configuration with Spring Boot
- **Logging**: Structured logging with context tracking
- **Spring Boot Integration**: Full Spring ecosystem support

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- API keys for your chosen LLM providers (OpenAI, DashScope, etc.)

### Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd agentscope4j
```

2. Build the project:
```bash
mvn clean install
```

3. Configure your API keys in `application.yml` or environment variables:
```yaml
agentscope:
  models:
    openai:
      api-key: ${OPENAI_API_KEY}
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
    gemini:
      api-key: ${GEMINI_API_KEY}
```

### Running the Example

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--run-example"
```

## Basic Usage

### Creating a Simple Agent

```java
@Component
public class MyAgentService {
    
    @Autowired
    private AgentScopeConfig config;
    
    public void createAgent() {
        // Create model
        OpenAIChatModel model = new OpenAIChatModel("gpt-3.5-turbo", false);
        model.setApiKey(config.getModels().getOpenai().getApiKey());
        
        // Create formatter
        OpenAIFormatter formatter = new OpenAIFormatter();
        
        // Create memory
        InMemoryMemory memory = new InMemoryMemory();
        
        // Create toolkit
        Toolkit toolkit = new Toolkit();
        
        // Create agent
        ReActAgent agent = new ReActAgent(
            "MyAgent",
            "You are a helpful assistant.",
            model,
            formatter,
            toolkit,
            memory,
            null, // No long-term memory
            "static_control",
            false, // No meta tools
            false, // No parallel tool calls
            5 // Max iterations
        );
        
        // Use the agent
        Msg userMessage = new Msg("user", "Hello, how are you?", "user");
        CompletableFuture<Msg> response = agent.reply(userMessage);
        
        response.thenAccept(msg -> {
            System.out.println("Agent response: " + msg.getTextContent());
        });
    }
}
```

### Adding Custom Tools

```java
// Register a custom tool
toolkit.registerTool(
    "weather_tool",
    input -> {
        String location = (String) input.get("location");
        // Your weather API logic here
        String weather = getWeatherForLocation(location);
        return ToolResponse.success(weather);
    },
    "Get weather information for a location",
    Map.of(
        "type", "object",
        "properties", Map.of(
            "location", Map.of(
                "type", "string",
                "description", "The location to get weather for"
            )
        ),
        "required", List.of("location")
    )
);
```

### Working with Memory

```java
// Create memory with configuration
InMemoryMemory memory = new InMemoryMemory(1000, true); // Max 1000 messages, auto-truncate

// Add messages
Msg userMsg = new Msg("user", "Remember this important fact", "user");
memory.addMessage(userMsg);

// Retrieve messages
List<Msg> allMessages = memory.getMessages();
List<Msg> recentMessages = memory.getRecentMessages(10);
List<Msg> userMessages = memory.getMessagesByRole("user");

// Search messages
List<Msg> searchResults = memory.searchMessages("important", false);

// Get memory statistics
Map<String, Object> stats = memory.getMemoryStats();
System.out.println("Memory stats: " + stats);
```

### Configuration

The application can be configured through `application.yml`:

```yaml
agentscope:
  models:
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: https://api.openai.com/v1
      timeout: 30000
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
      base-url: https://dashscope.aliyuncs.com/api/v1
      timeout: 30000
  
  agents:
    default-timeout: 60000
    max-retry: 3
  
  memory:
    max-history: 1000
    enable-long-term: false
  
  tools:
    execution-timeout: 30000
    enable-async: true
```

### Logging

AgentScope4J provides structured logging with context:

```java
// Set logging context
AgentScopeLogger.setAgentContext(agent.getId(), agent.getName());
AgentScopeLogger.setSessionContext("session-123");

// Log with context
Logger logger = AgentScopeLogger.getLogger(MyClass.class);
logger.info("Agent is processing request");

// Log performance metrics
AgentScopeLogger.logModelInteraction(
    logger, "gpt-3.5-turbo", "chat", 100, 50, 1500
);

// Clear context when done
AgentScopeLogger.clearContext();
```

## Architecture

### Core Components

1. **Message System** (`com.agentscope.message`)
   - `Msg`: Core message class
   - `ContentBlock`: Support for different content types (text, images, tools)

2. **Agent Framework** (`com.agentscope.agent`)
   - `AgentBase`: Base class for all agents
   - `ReActAgent`: ReAct (Reasoning + Acting) agent implementation

3. **Model Integration** (`com.agentscope.model`)
   - `ChatModelBase`: Base class for model integrations
   - `OpenAIChatModel`: OpenAI API integration
   - `ChatResponse` and `ChatUsage`: Response and usage tracking

4. **Tool System** (`com.agentscope.tool`)
   - `Toolkit`: Tool management and execution
   - `ToolResponse`: Tool execution results

5. **Memory Management** (`com.agentscope.memory`)
   - `MemoryBase`: Base memory interface
   - `InMemoryMemory`: In-memory storage implementation
   - `LongTermMemoryBase`: Long-term memory interface

6. **Formatters** (`com.agentscope.formatter`)
   - `FormatterBase`: Base formatter class
   - `OpenAIFormatter`: OpenAI API format converter

### Design Principles

- **Modularity**: Each component is independent and replaceable
- **Extensibility**: Easy to add new models, tools, and memory backends
- **Type Safety**: Strong typing with proper validation
- **Async Support**: Non-blocking operations with CompletableFuture
- **Spring Integration**: Full Spring Boot ecosystem support

## Testing

Run the test suite:

```bash
mvn test
```

The tests cover:
- Component integration
- Message serialization/deserialization
- Memory operations
- Tool execution
- Configuration validation

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass
6. Submit a pull request

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.

## Acknowledgments

- Based on the original [AgentScope](https://github.com/agentscope-ai/agentscope) Python framework
- Built with Spring Boot and the Java ecosystem
- Inspired by the multi-agent systems research community

## Support

For questions, issues, or contributions:
- Open an issue on GitHub
- Check the documentation
- Review the example code

---

**AgentScope4J** - Building the future of multi-agent systems in Java