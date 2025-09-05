package com.agentscope.agent;

import com.agentscope.formatter.FormatterBase;
import com.agentscope.memory.InMemoryMemory;
import com.agentscope.memory.LongTermMemoryBase;
import com.agentscope.memory.MemoryBase;
import com.agentscope.message.Msg;
import com.agentscope.model.ChatModelBase;
import com.agentscope.model.ChatResponse;
import com.agentscope.tool.ToolResponse;
import com.agentscope.tool.Toolkit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * A ReAct agent implementation in AgentScope4J, which supports:
 * - Realtime steering
 * - API-based (parallel) tool calling
 * - Hooks around reasoning, acting, reply, observe and print functions
 * - Structured output generation
 *
 * @author Alex Huangfu
 * @version 1.0.0
 */
@Component
public class ReActAgent extends AgentBase {

    private static final Logger logger = LoggerFactory.getLogger(ReActAgent.class);

    // Configuration
    private String sysPrompt;
    private ChatModelBase model;
    private FormatterBase formatter;
    private Toolkit toolkit;
    private MemoryBase memory;
    private LongTermMemoryBase longTermMemory;
    private String longTermMemoryMode = "both"; // "agent_control", "static_control", "both"
    private boolean enableMetaTool = false;
    private boolean parallelToolCalls = false;
    private int maxIters = 10;

    // Runtime state
    private boolean staticControl = false;
    private boolean agentControl = false;
    private String finishFunctionName = "generate_response";

    /**
     * Default constructor for Spring
     */
    public ReActAgent() {
        super();
        this.memory = new InMemoryMemory();
        this.toolkit = new Toolkit();
    }

    /**
     * Initialize the ReAct agent
     *
     * @param name      The name of the agent
     * @param sysPrompt The system prompt of the agent
     * @param model     The chat model used by the agent
     * @param formatter The formatter used to format messages
     */
    public ReActAgent(String name, String sysPrompt, ChatModelBase model, FormatterBase formatter) {
        super(name);
        this.sysPrompt = sysPrompt;
        this.model = model;
        this.formatter = formatter;
        this.memory = new InMemoryMemory();
        this.toolkit = new Toolkit();

        initializeLongTermMemoryMode();
    }

    /**
     * Full constructor for ReAct agent
     *
     * @param name               The name of the agent
     * @param sysPrompt          The system prompt of the agent
     * @param model              The chat model used by the agent
     * @param formatter          The formatter used to format messages
     * @param toolkit            A Toolkit object that contains the tool functions
     * @param memory             The memory used to store dialogue history
     * @param longTermMemory     The optional long-term memory
     * @param longTermMemoryMode The mode of the long-term memory
     * @param enableMetaTool     If true, meta tool functions will be added
     * @param parallelToolCalls  Whether to execute multiple tool calls in parallel
     * @param maxIters           The maximum number of reasoning-acting loops
     */
    public ReActAgent(
            String name,
            String sysPrompt,
            ChatModelBase model,
            FormatterBase formatter,
            Toolkit toolkit,
            MemoryBase memory,
            LongTermMemoryBase longTermMemory,
            String longTermMemoryMode,
            boolean enableMetaTool,
            boolean parallelToolCalls,
            int maxIters) {

        super(name);

        // Validate long-term memory mode
        if (!Arrays.asList("agent_control", "static_control", "both").contains(longTermMemoryMode)) {
            throw new IllegalArgumentException("Invalid long_term_memory_mode: " + longTermMemoryMode);
        }

        this.sysPrompt = sysPrompt;
        this.model = model;
        this.formatter = formatter;
        this.toolkit = toolkit != null ? toolkit : new Toolkit();
        this.memory = memory != null ? memory : new InMemoryMemory();
        this.longTermMemory = longTermMemory;
        this.longTermMemoryMode = longTermMemoryMode;
        this.enableMetaTool = enableMetaTool;
        this.parallelToolCalls = parallelToolCalls;
        this.maxIters = maxIters;

        initializeLongTermMemoryMode();
        setupHooks();
    }

    /**
     * Initialize long-term memory mode settings
     */
    private void initializeLongTermMemoryMode() {
        if (longTermMemory != null) {
            this.staticControl = Arrays.asList("static_control", "both").contains(longTermMemoryMode);
            this.agentControl = Arrays.asList("agent_control", "both").contains(longTermMemoryMode);
        }
    }

    /**
     * Setup hooks for the agent
     */
    private void setupHooks() {
        // Register finish function pre-print hook
        registerInstanceHook("pre_print", "finish_function_pre_print_hook", this::finishFunctionPrePrintHook);
    }

    /**
     * Pre-print hook that checks if finish_function is called
     */
    private Map<String, Object> finishFunctionPrePrintHook(AgentBase agent) {
        // Implementation of the finish function pre-print hook
        // This would modify the message content if finish function is called
        return null; // Simplified implementation
    }

    @Override
    public CompletableFuture<Void> observe(Msg msg) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (msg != null) {
                    memory.addMessage(msg);
                    logger.debug("Agent {} observed message: {}", getName(), msg.getId());
                }
            } catch (Exception e) {
                logger.error("Error observing message in agent {}", getName(), e);
            }
        });
    }

    @Override
    public CompletableFuture<Msg> reply(Object... args) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Convert args to input message if needed
                Msg inputMsg = null;
                if (args.length > 0 && args[0] instanceof Msg) {
                    inputMsg = (Msg) args[0];
                } else if (args.length > 0 && args[0] instanceof String) {
                    inputMsg = new Msg("user", args[0], "user");
                }

                if (inputMsg != null) {
                    observe(inputMsg).join();
                }

                // Perform reasoning and acting loop
                return performReasoningActingLoop();

            } catch (Exception e) {
                logger.error("Error in reply for agent {}", getName(), e);
                return new Msg(getName(), "Error occurred during reply: " + e.getMessage(), "assistant");
            }
        });
    }

    /**
     * Perform the main reasoning-acting loop
     *
     * @return The final response message
     */
    private Msg performReasoningActingLoop() {
        try {
            // Retrieve from long-term memory if static control is enabled
            if (staticControl && longTermMemory != null) {
                retrieveFromLongTermMemory();
            }

            List<Msg> messages = memory.getMessages();

            // Add system prompt
            if (sysPrompt != null && !sysPrompt.isEmpty()) {
                Msg systemMsg = new Msg("system", sysPrompt, "system");
                messages.add(0, systemMsg);
            }

            // Perform reasoning-acting iterations
            for (int iter = 0; iter < maxIters; iter++) {
                logger.debug("Agent {} reasoning-acting iteration {}", getName(), iter + 1);

                // Call the model
                Object response = model.call(messages, toolkit.getToolSchemas(), null, new HashMap<>());

                if (response instanceof ChatResponse) {
                    ChatResponse chatResponse = (ChatResponse) response;

                    // Create response message
                    Msg responseMsg = new Msg(getName(), chatResponse.getContent(), "assistant");

                    // Check if this is the final response
                    if (isFinishResponse(responseMsg)) {
                        memory.addMessage(responseMsg);

                        // Record to long-term memory if static control is enabled
                        if (staticControl && longTermMemory != null) {
                            recordToLongTermMemory(responseMsg);
                        }

                        return responseMsg;
                    }

                    // Handle tool calls if present
                    List<Map<String, Object>> toolUseBlocks = responseMsg.getContentBlocks("tool_use");
                    if (!toolUseBlocks.isEmpty()) {
                        handleToolCalls(toolUseBlocks, messages);
                    } else {
                        // No tool calls, add response and continue
                        memory.addMessage(responseMsg);
                        messages.add(responseMsg);
                    }
                } else {
                    logger.warn("Unexpected response type from model: {}", response.getClass());
                    break;
                }
            }

            // If we reach here, max iterations exceeded
            Msg finalMsg = new Msg(getName(), "Maximum iterations reached without completion.", "assistant");
            memory.addMessage(finalMsg);
            return finalMsg;

        } catch (Exception e) {
            logger.error("Error in reasoning-acting loop for agent {}", getName(), e);
            return new Msg(getName(), "Error in reasoning-acting loop: " + e.getMessage(), "assistant");
        }
    }

    /**
     * Check if the response indicates completion
     */
    private boolean isFinishResponse(Msg msg) {
        List<Map<String, Object>> toolUseBlocks = msg.getContentBlocks("tool_use");
        for (Map<String, Object> block : toolUseBlocks) {
            if (finishFunctionName.equals(block.get("name"))) {
                return true;
            }
        }
        return toolUseBlocks.isEmpty() && msg.getTextContent() != null;
    }

    /**
     * Handle tool calls in the response
     */
    private void handleToolCalls(List<Map<String, Object>> toolUseBlocks, List<Msg> messages) {
        for (Map<String, Object> toolUse : toolUseBlocks) {
            String toolName = (String) toolUse.get("name");
            String toolId = (String) toolUse.get("id");
            Map<String, Object> toolInput = (Map<String, Object>) toolUse.get("input");

            try {
                ToolResponse toolResponse = toolkit.executeTool(toolName, toolInput);

                // Create tool result message
                Map<String, Object> toolResult = new HashMap<>();
                toolResult.put("type", "tool_result");
                toolResult.put("id", toolId);
                toolResult.put("output", toolResponse.getContent());

                Msg toolResultMsg = new Msg(getName(), Arrays.asList(toolResult), "assistant");
                memory.addMessage(toolResultMsg);
                messages.add(toolResultMsg);

            } catch (Exception e) {
                logger.error("Error executing tool {}", toolName, e);

                // Create error result
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("type", "tool_result");
                errorResult.put("id", toolId);
                errorResult.put("output", "Error: " + e.getMessage());

                Msg errorMsg = new Msg(getName(), Arrays.asList(errorResult), "assistant");
                memory.addMessage(errorMsg);
                messages.add(errorMsg);
            }
        }
    }

    /**
     * Retrieve information from long-term memory
     */
    private void retrieveFromLongTermMemory() {
        if (longTermMemory != null) {
            try {
                // Implementation would retrieve relevant information
                // and add it to the system prompt or memory
                logger.debug("Retrieving from long-term memory for agent {}", getName());
            } catch (Exception e) {
                logger.error("Error retrieving from long-term memory", e);
            }
        }
    }

    /**
     * Record information to long-term memory
     */
    private void recordToLongTermMemory(Msg msg) {
        if (longTermMemory != null) {
            try {
                // Implementation would record the message to long-term memory
                logger.debug("Recording to long-term memory for agent {}", getName());
            } catch (Exception e) {
                logger.error("Error recording to long-term memory", e);
            }
        }
    }

    /**
     * Generate response tool function
     */
    public ToolResponse generateResponse(String response, Map<String, Object> kwargs) {
        return new ToolResponse(response, true);
    }

    /**
     * Handle interrupt during execution
     */
    public CompletableFuture<Msg> handleInterrupt(Msg msg) {
        return CompletableFuture.supplyAsync(() -> {
            if (msg != null) {
                observe(msg).join();
            }
            return new Msg(getName(), "Agent interrupted and ready for new input.", "assistant");
        });
    }

    // Getters and Setters
    public String getSysPrompt() {
        return sysPrompt;
    }

    public void setSysPrompt(String sysPrompt) {
        this.sysPrompt = sysPrompt;
    }

    public ChatModelBase getModel() {
        return model;
    }

    public void setModel(ChatModelBase model) {
        this.model = model;
    }

    public FormatterBase getFormatter() {
        return formatter;
    }

    public void setFormatter(FormatterBase formatter) {
        this.formatter = formatter;
    }

    public Toolkit getToolkit() {
        return toolkit;
    }

    public void setToolkit(Toolkit toolkit) {
        this.toolkit = toolkit;
    }

    public MemoryBase getMemory() {
        return memory;
    }

    public void setMemory(MemoryBase memory) {
        this.memory = memory;
    }

    public LongTermMemoryBase getLongTermMemory() {
        return longTermMemory;
    }

    public void setLongTermMemory(LongTermMemoryBase longTermMemory) {
        this.longTermMemory = longTermMemory;
    }

    public int getMaxIters() {
        return maxIters;
    }

    public void setMaxIters(int maxIters) {
        this.maxIters = maxIters;
    }

    public boolean isParallelToolCalls() {
        return parallelToolCalls;
    }

    public void setParallelToolCalls(boolean parallelToolCalls) {
        this.parallelToolCalls = parallelToolCalls;
    }
}