package com.agentscope.agent;

import com.agentscope.message.Msg;
import com.agentscope.module.StateModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Base class for agents in AgentScope4J.
 *
 * @author Alex Huangfu
 * @version 1.0.0
 */
@Component
public abstract class AgentBase extends StateModule {

    private static final Logger logger = LoggerFactory.getLogger(AgentBase.class);

    protected String id;
    protected String name;

    // Hook types supported by the agent
    protected static final List<String> SUPPORTED_HOOK_TYPES = Arrays.asList(
            "pre_reply", "post_reply", "pre_print", "post_print",
            "pre_observe", "post_observe"
    );

    // Class-level hooks (static)
    private static final Map<String, Function<AgentBase, Map<String, Object>>> classPreReplyHooks = new LinkedHashMap<>();
    private static final Map<String, Function<AgentBase, Msg>> classPostReplyHooks = new LinkedHashMap<>();
    private static final Map<String, Function<AgentBase, Map<String, Object>>> classPrePrintHooks = new LinkedHashMap<>();
    private static final Map<String, Function<AgentBase, Object>> classPostPrintHooks = new LinkedHashMap<>();
    private static final Map<String, Function<AgentBase, Map<String, Object>>> classPreObserveHooks = new LinkedHashMap<>();
    private static final Map<String, Function<AgentBase, Void>> classPostObserveHooks = new LinkedHashMap<>();

    // Instance-level hooks
    private final Map<String, Function<AgentBase, Map<String, Object>>> instancePreReplyHooks = new LinkedHashMap<>();
    private final Map<String, Function<AgentBase, Msg>> instancePostReplyHooks = new LinkedHashMap<>();
    private final Map<String, Function<AgentBase, Map<String, Object>>> instancePrePrintHooks = new LinkedHashMap<>();
    private final Map<String, Function<AgentBase, Object>> instancePostPrintHooks = new LinkedHashMap<>();
    private final Map<String, Function<AgentBase, Map<String, Object>>> instancePreObserveHooks = new LinkedHashMap<>();
    private final Map<String, Function<AgentBase, Void>> instancePostObserveHooks = new LinkedHashMap<>();

    // Current replying task and ID
    private CompletableFuture<Msg> replyTask;
    private String replyId;

    // Stream prefix for streaming messages
    private final Map<String, String> streamPrefix = new ConcurrentHashMap<>();

    // Subscribers for message broadcasting
    private final Map<String, List<AgentBase>> subscribers = new ConcurrentHashMap<>();

    // Console output control
    private boolean disableConsoleOutput = false;

    /**
     * Initialize the agent
     */
    public AgentBase() {
        super();
        this.id = UUID.randomUUID().toString();
        this.name = getClass().getSimpleName();
    }

    /**
     * Initialize the agent with name
     *
     * @param name The name of the agent
     */
    public AgentBase(String name) {
        super();
        this.id = UUID.randomUUID().toString();
        this.name = name;
    }

    /**
     * Receive the given message(s) without generating a reply.
     *
     * @param msg The message(s) to be observed
     * @return CompletableFuture for async operation
     */
    public abstract CompletableFuture<Void> observe(Msg msg);

    /**
     * Observe multiple messages
     *
     * @param messages List of messages to observe
     * @return CompletableFuture for async operation
     */
    public CompletableFuture<Void> observe(List<Msg> messages) {
        if (messages == null || messages.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
        for (Msg msg : messages) {
            future = future.thenCompose(v -> observe(msg));
        }
        return future;
    }

    /**
     * The main logic of the agent, which generates a reply based on the
     * current state and input arguments.
     *
     * @param args Input arguments
     * @return CompletableFuture of the reply message
     */
    public abstract CompletableFuture<Msg> reply(Object... args);

    /**
     * The function to display the message.
     *
     * @param msg  The message object to be printed
     * @param last Whether this is the last one in streaming messages
     * @return CompletableFuture for async operation
     */
    public CompletableFuture<Void> print(Msg msg, boolean last) {
        return CompletableFuture.runAsync(() -> {
            if (disableConsoleOutput) {
                return;
            }

            try {
                List<String> thinkingAndTextToPrint = new ArrayList<>();
                List<Map<String, Object>> contentBlocks = msg.getContentBlocks(null);

                String prefix = streamPrefix.getOrDefault(msg.getId(), "");

                for (Map<String, Object> block : contentBlocks) {
                    String blockType = (String) block.get("type");
                    if ("text".equals(blockType) || "thinking".equals(blockType)) {
                        String formatPrefix = "text".equals(blockType) ? "" : "(thinking)";
                        String content = "";

                        if ("text".equals(blockType)) {
                            content = (String) block.get("text");
                        } else if ("thinking".equals(blockType)) {
                            content = (String) block.get("thinking");
                        }

                        if (content != null && !content.isEmpty()) {
                            thinkingAndTextToPrint.add(formatPrefix + content);
                        }
                    }
                }

                // Print the content
                for (String content : thinkingAndTextToPrint) {
                    if (last) {
                        System.out.println(String.format("%s: %s%s", name, prefix, content));
                    } else {
                        System.out.print(content);
                    }
                }

                if (last) {
                    streamPrefix.remove(msg.getId());
                }

            } catch (Exception e) {
                logger.error("Error printing message", e);
            }
        });
    }

    /**
     * Print message with default last=true
     *
     * @param msg The message to print
     * @return CompletableFuture for async operation
     */
    public CompletableFuture<Void> print(Msg msg) {
        return print(msg, true);
    }

    /**
     * Interrupt the current replying process
     *
     * @param msg Optional message to observe before interrupting
     * @return CompletableFuture for async operation
     */
    public CompletableFuture<Void> interrupt(Msg msg) {
        return CompletableFuture.runAsync(() -> {
            if (msg != null) {
                observe(msg).join();
            }

            if (replyTask != null && !replyTask.isDone()) {
                replyTask.cancel(true);
                logger.info("Agent {} reply task interrupted", name);
            }
        });
    }

    /**
     * Register an instance-level hook
     *
     * @param hookType The type of hook
     * @param hookName The name of the hook
     * @param hook     The hook function
     */
    public void registerInstanceHook(String hookType, String hookName, Function<AgentBase, ?> hook) {
        if (!SUPPORTED_HOOK_TYPES.contains(hookType)) {
            throw new IllegalArgumentException("Unsupported hook type: " + hookType);
        }

        switch (hookType) {
            case "pre_reply":
                instancePreReplyHooks.put(hookName, (Function<AgentBase, Map<String, Object>>) hook);
                break;
            case "post_reply":
                instancePostReplyHooks.put(hookName, (Function<AgentBase, Msg>) hook);
                break;
            case "pre_print":
                instancePrePrintHooks.put(hookName, (Function<AgentBase, Map<String, Object>>) hook);
                break;
            case "post_print":
                instancePostPrintHooks.put(hookName, (Function<AgentBase, Object>) hook);
                break;
            case "pre_observe":
                instancePreObserveHooks.put(hookName, (Function<AgentBase, Map<String, Object>>) hook);
                break;
            case "post_observe":
                instancePostObserveHooks.put(hookName, (Function<AgentBase, Void>) hook);
                break;
        }
    }

    /**
     * Remove an instance-level hook
     *
     * @param hookType The type of hook
     * @param hookName The name of the hook
     */
    public void removeInstanceHook(String hookType, String hookName) {
        switch (hookType) {
            case "pre_reply":
                instancePreReplyHooks.remove(hookName);
                break;
            case "post_reply":
                instancePostReplyHooks.remove(hookName);
                break;
            case "pre_print":
                instancePrePrintHooks.remove(hookName);
                break;
            case "post_print":
                instancePostPrintHooks.remove(hookName);
                break;
            case "pre_observe":
                instancePreObserveHooks.remove(hookName);
                break;
            case "post_observe":
                instancePostObserveHooks.remove(hookName);
                break;
        }
    }

    /**
     * Add subscribers for message broadcasting
     *
     * @param msgHubName  The message hub name
     * @param subscribers List of subscriber agents
     */
    public void addSubscribers(String msgHubName, List<AgentBase> subscribers) {
        this.subscribers.put(msgHubName, new ArrayList<>(subscribers));
    }

    /**
     * Reset subscribers for a message hub
     *
     * @param msgHubName  The message hub name
     * @param subscribers New list of subscriber agents
     */
    public void resetSubscribers(String msgHubName, List<AgentBase> subscribers) {
        this.subscribers.put(msgHubName, new ArrayList<>(subscribers));
    }

    /**
     * Broadcast message to subscribers
     *
     * @param msgHubName The message hub name
     * @param msg        The message to broadcast
     * @return CompletableFuture for async operation
     */
    protected CompletableFuture<Void> broadcastToSubscribers(String msgHubName, Msg msg) {
        List<AgentBase> hubSubscribers = subscribers.get(msgHubName);
        if (hubSubscribers == null || hubSubscribers.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (AgentBase subscriber : hubSubscribers) {
            futures.add(subscriber.observe(msg));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDisableConsoleOutput() {
        return disableConsoleOutput;
    }

    public void setDisableConsoleOutput(boolean disableConsoleOutput) {
        this.disableConsoleOutput = disableConsoleOutput;
    }

    public CompletableFuture<Msg> getReplyTask() {
        return replyTask;
    }

    protected void setReplyTask(CompletableFuture<Msg> replyTask) {
        this.replyTask = replyTask;
    }

    public String getReplyId() {
        return replyId;
    }

    protected void setReplyId(String replyId) {
        this.replyId = replyId;
    }

    @Override
    public String toString() {
        return String.format("%s{id='%s', name='%s'}",
                getClass().getSimpleName(), id, name);
    }
}