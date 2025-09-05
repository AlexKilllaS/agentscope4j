package com.agentscope;

import com.agentscope.agent.AgentBase;
import com.agentscope.config.AgentScopeConfig;
import com.agentscope.formatter.OpenAIFormatter;
import com.agentscope.memory.InMemoryMemory;
import com.agentscope.message.Msg;
import com.agentscope.model.ChatUsage;
import com.agentscope.tool.Toolkit;
import com.agentscope.tool.ToolResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for AgentScope4J application.
 * 
 * @author Alex Huangfu
 * @version 1.0.0
 */
@SpringBootTest
class AgentScope4JApplicationTests {
    
    @Autowired
    private AgentScopeConfig config;
    
    @Test
    void contextLoads() {
        // Test that Spring context loads successfully
        assertNotNull(config);
    }
    
    @Test
    void testConfigurationProperties() {
        // Test configuration loading
        assertNotNull(config.getModels());
        assertNotNull(config.getAgents());
        assertNotNull(config.getMemory());
        assertNotNull(config.getTools());
        
        // Test default values
        assertTrue(config.getAgents().getDefaultTimeout() > 0);
        assertTrue(config.getMemory().getMaxHistory() > 0);
        assertTrue(config.getTools().getExecutionTimeout() > 0);
        
        // Test configuration validation
        assertTrue(config.isValid());
    }
    
    @Test
    void testMessageCreationAndSerialization() {
        // Test basic message creation
        Msg msg = new Msg("test-user", "Hello, world!", "user");
        
        assertNotNull(msg.getId());
        assertEquals("test-user", msg.getName());
        assertEquals("Hello, world!", msg.getContent());
        assertEquals("user", msg.getRole());
        assertNotNull(msg.getTimestamp());
        
        // Test serialization
        Map<String, Object> dict = msg.toDict();
        assertNotNull(dict);
        assertEquals("test-user", dict.get("name"));
        assertEquals("Hello, world!", dict.get("content"));
        assertEquals("user", dict.get("role"));
        
        // Test deserialization
        Msg deserializedMsg = Msg.fromDict(dict);
        assertEquals(msg.getName(), deserializedMsg.getName());
        assertEquals(msg.getContent(), deserializedMsg.getContent());
        assertEquals(msg.getRole(), deserializedMsg.getRole());
    }
    
    @Test
    void testInMemoryMemory() {
        InMemoryMemory memory = new InMemoryMemory(10, true);
        
        // Test initial state
        assertTrue(memory.isEmpty());
        assertEquals(0, memory.getMessageCount());
        
        // Test adding messages
        Msg msg1 = new Msg("user", "First message", "user");
        Msg msg2 = new Msg("assistant", "Second message", "assistant");
        
        memory.addMessage(msg1);
        memory.addMessage(msg2);
        
        assertFalse(memory.isEmpty());
        assertEquals(2, memory.getMessageCount());
        
        // Test retrieving messages
        List<Msg> messages = memory.getMessages();
        assertEquals(2, messages.size());
        assertEquals(msg1.getId(), messages.get(0).getId());
        assertEquals(msg2.getId(), messages.get(1).getId());
        
        // Test filtering by role
        List<Msg> userMessages = memory.getMessagesByRole("user");
        assertEquals(1, userMessages.size());
        assertEquals("user", userMessages.get(0).getRole());
        
        // Test recent messages
        List<Msg> recentMessages = memory.getRecentMessages(1);
        assertEquals(1, recentMessages.size());
        assertEquals(msg2.getId(), recentMessages.get(0).getId());
        
        // Test clearing
        memory.clear();
        assertTrue(memory.isEmpty());
        assertEquals(0, memory.getMessageCount());
    }
    
    @Test
    void testToolkit() {
        Toolkit toolkit = new Toolkit();
        
        // Test built-in tools
        assertTrue(toolkit.hasTool("echo"));
        assertTrue(toolkit.hasTool("get_current_time"));
        
        // Test echo tool
        ToolResponse echoResponse = toolkit.executeTool("echo", 
            Map.of("message", "Hello, toolkit!"));
        
        assertTrue(echoResponse.isFinal());
        assertEquals("Hello, toolkit!", echoResponse.getContent());
        
        // Test time tool
        ToolResponse timeResponse = toolkit.executeTool("get_current_time", Map.of());
        assertTrue(timeResponse.isFinal());
        assertNotNull(timeResponse.getContent());
        
        // Test non-existent tool
        ToolResponse errorResponse = toolkit.executeTool("non_existent_tool", Map.of());
        assertTrue(errorResponse.isError());
        
        // Test custom tool registration
        toolkit.registerTool(
            "test_tool",
            input -> ToolResponse.success("Test result: " + input.get("input")),
            "A test tool",
            Map.of("type", "object", "properties", Map.of())
        );
        
        assertTrue(toolkit.hasTool("test_tool"));
        
        ToolResponse testResponse = toolkit.executeTool("test_tool", 
            Map.of("input", "test_value"));
        assertEquals("Test result: test_value", testResponse.getContent());
    }
    
    @Test
    void testOpenAIFormatter() {
        OpenAIFormatter formatter = new OpenAIFormatter();
        
        // Test formatter capabilities
        assertTrue(formatter.supportsStreaming());
        assertTrue(formatter.supportsToolCalls());
        assertTrue(formatter.supportsMultimodal());
        assertTrue(formatter.getMaxTokens() > 0);
        
        // Test message formatting
        Msg userMsg = new Msg("user", "Hello, AI!", "user");
        Msg assistantMsg = new Msg("assistant", "Hello, human!", "assistant");
        
        List<Msg> messages = List.of(userMsg, assistantMsg);
        
        // Test basic formatting
        Object formatted = formatter.format(messages);
        assertNotNull(formatted);
        assertTrue(formatted instanceof List);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> formattedMessages = (List<Map<String, Object>>) formatted;
        assertEquals(2, formattedMessages.size());
        
        Map<String, Object> formattedUserMsg = formattedMessages.get(0);
        assertEquals("user", formattedUserMsg.get("role"));
        assertEquals("Hello, AI!", formattedUserMsg.get("content"));
        
        Map<String, Object> formattedAssistantMsg = formattedMessages.get(1);
        assertEquals("assistant", formattedAssistantMsg.get("role"));
        assertEquals("Hello, human!", formattedAssistantMsg.get("content"));
    }
    
    @Test
    void testChatUsage() {
        ChatUsage usage = new ChatUsage(100, 50, 1.5);
        
        assertEquals(100, usage.getInputTokens());
        assertEquals(50, usage.getOutputTokens());
        assertEquals(150, usage.getTotalTokens());
        assertEquals(1.5, usage.getTime(), 0.001);
        assertEquals("chat", usage.getType());
        
        // Test serialization
        Map<String, Object> usageDict = usage.toDict();
        assertEquals(100, usageDict.get("input_tokens"));
        assertEquals(50, usageDict.get("output_tokens"));
        assertEquals(1.5, usageDict.get("time"));
        assertEquals("chat", usageDict.get("type"));
        
        // Test deserialization
        ChatUsage deserializedUsage = ChatUsage.fromDict(usageDict);
        assertEquals(usage.getInputTokens(), deserializedUsage.getInputTokens());
        assertEquals(usage.getOutputTokens(), deserializedUsage.getOutputTokens());
        assertEquals(usage.getTime(), deserializedUsage.getTime(), 0.001);
        
        // Test addition
        ChatUsage usage2 = new ChatUsage(50, 25, 0.8);
        ChatUsage combined = usage.add(usage2);
        
        assertEquals(150, combined.getInputTokens());
        assertEquals(75, combined.getOutputTokens());
        assertEquals(2.3, combined.getTime(), 0.001);
    }
    
    @Test
    void testToolResponse() {
        // Test successful response
        ToolResponse successResponse = ToolResponse.success("Operation completed");
        assertTrue(successResponse.isFinal());
        assertFalse(successResponse.isError());
        assertEquals("Operation completed", successResponse.getContent());
        assertTrue(successResponse.hasContent());
        
        // Test error response
        ToolResponse errorResponse = ToolResponse.error("Something went wrong");
        assertTrue(errorResponse.isFinal());
        assertTrue(errorResponse.isError());
        assertTrue(errorResponse.getContentAsString().contains("Error:"));
        
        // Test streaming response
        ToolResponse streamingResponse = ToolResponse.streaming("Partial result");
        assertFalse(streamingResponse.isFinal());
        assertFalse(streamingResponse.isError());
        assertEquals("Partial result", streamingResponse.getContent());
        
        // Test serialization
        Map<String, Object> responseDict = successResponse.toDict();
        assertNotNull(responseDict);
        assertEquals("Operation completed", responseDict.get("content"));
        assertTrue((Boolean) responseDict.get("is_final"));
        
        // Test deserialization
        ToolResponse deserializedResponse = ToolResponse.fromDict(responseDict);
        assertEquals(successResponse.getContent(), deserializedResponse.getContent());
        assertEquals(successResponse.isFinal(), deserializedResponse.isFinal());
    }
    
    /**
     * Simple test agent for testing purposes
     */
    static class TestAgent extends AgentBase {
        
        public TestAgent(String name) {
            super(name);
        }
        
        @Override
        public CompletableFuture<Void> observe(Msg msg) {
            return CompletableFuture.completedFuture(null);
        }
        
        @Override
        public CompletableFuture<Msg> reply(Object... args) {
            String response = "Test response from " + getName();
            if (args.length > 0) {
                response += ": " + args[0];
            }
            Msg responseMsg = new Msg(getName(), response, "assistant");
            return CompletableFuture.completedFuture(responseMsg);
        }
    }
    
    @Test
    void testAgentBase() {
        TestAgent agent = new TestAgent("TestAgent");
        
        assertNotNull(agent.getId());
        assertEquals("TestAgent", agent.getName());
        assertFalse(agent.isDisableConsoleOutput());
        
        // Test reply functionality
        CompletableFuture<Msg> replyFuture = agent.reply("Hello");
        assertNotNull(replyFuture);
        
        Msg reply = replyFuture.join();
        assertNotNull(reply);
        assertEquals("TestAgent", reply.getName());
        assertEquals("assistant", reply.getRole());
        assertTrue(reply.getTextContent().contains("Test response"));
        assertTrue(reply.getTextContent().contains("Hello"));
        
        // Test observe functionality
        Msg observeMsg = new Msg("user", "Observe this", "user");
        CompletableFuture<Void> observeFuture = agent.observe(observeMsg);
        assertNotNull(observeFuture);
        assertDoesNotThrow(() -> observeFuture.join());
    }
}