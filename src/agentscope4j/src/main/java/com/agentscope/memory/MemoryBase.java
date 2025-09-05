package com.agentscope.memory;

import com.agentscope.message.Msg;
import com.agentscope.module.StateModule;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Base class for memory management in AgentScope4J.
 * Handles storage and retrieval of conversation history and context.
 * 
 * @author Alex Huangfu
 * @version 1.0.0
 */
@Component
public abstract class MemoryBase extends StateModule {
    
    /**
     * Add a message to the memory.
     * 
     * @param message The message to add
     */
    public abstract void addMessage(Msg message);
    
    /**
     * Add multiple messages to the memory.
     * 
     * @param messages List of messages to add
     */
    public void addMessages(List<Msg> messages) {
        if (messages != null) {
            for (Msg message : messages) {
                addMessage(message);
            }
        }
    }
    
    /**
     * Get all messages from the memory.
     * 
     * @return List of all stored messages
     */
    public abstract List<Msg> getMessages();
    
    /**
     * Get messages with optional filtering.
     * 
     * @param filter Filter criteria (implementation-specific)
     * @return Filtered list of messages
     */
    public List<Msg> getMessages(Map<String, Object> filter) {
        return getMessages(); // Default implementation returns all messages
    }
    
    /**
     * Get the most recent N messages.
     * 
     * @param count Number of recent messages to retrieve
     * @return List of recent messages
     */
    public abstract List<Msg> getRecentMessages(int count);
    
    /**
     * Get messages by role (user, assistant, system).
     * 
     * @param role The role to filter by
     * @return List of messages with the specified role
     */
    public abstract List<Msg> getMessagesByRole(String role);
    
    /**
     * Get the total number of messages in memory.
     * 
     * @return Total message count
     */
    public abstract int getMessageCount();
    
    /**
     * Clear all messages from memory.
     */
    public abstract void clear();
    
    /**
     * Remove a specific message by ID.
     * 
     * @param messageId The ID of the message to remove
     * @return true if the message was removed, false if not found
     */
    public abstract boolean removeMessage(String messageId);
    
    /**
     * Remove messages older than the specified timestamp.
     * 
     * @param timestamp The cutoff timestamp
     * @return Number of messages removed
     */
    public abstract int removeMessagesOlderThan(String timestamp);
    
    /**
     * Get the memory size in terms of estimated tokens.
     * This is useful for managing context length limits.
     * 
     * @return Estimated token count
     */
    public int getEstimatedTokenCount() {
        List<Msg> messages = getMessages();
        int totalTokens = 0;
        
        for (Msg msg : messages) {
            String textContent = msg.getTextContent();
            if (textContent != null) {
                // Rough estimation: 1 token per 4 characters
                totalTokens += textContent.length() / 4;
            }
            // Add overhead for message structure
            totalTokens += 10;
        }
        
        return totalTokens;
    }
    
    /**
     * Check if the memory is empty.
     * 
     * @return true if no messages are stored
     */
    public boolean isEmpty() {
        return getMessageCount() == 0;
    }
    
    /**
     * Get the first message in memory.
     * 
     * @return The first message, or null if memory is empty
     */
    public Msg getFirstMessage() {
        List<Msg> messages = getMessages();
        return messages.isEmpty() ? null : messages.get(0);
    }
    
    /**
     * Get the last message in memory.
     * 
     * @return The last message, or null if memory is empty
     */
    public Msg getLastMessage() {
        List<Msg> messages = getMessages();
        return messages.isEmpty() ? null : messages.get(messages.size() - 1);
    }
    
    /**
     * Search for messages containing specific text.
     * 
     * @param searchText The text to search for
     * @param caseSensitive Whether the search should be case-sensitive
     * @return List of messages containing the search text
     */
    public List<Msg> searchMessages(String searchText, boolean caseSensitive) {
        List<Msg> allMessages = getMessages();
        List<Msg> results = new java.util.ArrayList<>();
        
        String searchTerm = caseSensitive ? searchText : searchText.toLowerCase();
        
        for (Msg msg : allMessages) {
            String textContent = msg.getTextContent();
            if (textContent != null) {
                String content = caseSensitive ? textContent : textContent.toLowerCase();
                if (content.contains(searchTerm)) {
                    results.add(msg);
                }
            }
        }
        
        return results;
    }
    
    /**
     * Get memory statistics.
     * 
     * @return Map containing memory statistics
     */
    public Map<String, Object> getMemoryStats() {
        List<Msg> messages = getMessages();
        Map<String, Long> roleCounts = new java.util.HashMap<>();
        
        for (Msg msg : messages) {
            String role = msg.getRole();
            roleCounts.put(role, roleCounts.getOrDefault(role, 0L) + 1);
        }
        
        return Map.of(
            "total_messages", getMessageCount(),
            "estimated_tokens", getEstimatedTokenCount(),
            "role_counts", roleCounts,
            "is_empty", isEmpty(),
            "memory_type", getClass().getSimpleName()
        );
    }
    
    /**
     * Export memory content to a serializable format.
     * 
     * @return Map containing exportable memory data
     */
    public Map<String, Object> exportMemory() {
        List<Msg> messages = getMessages();
        List<Map<String, Object>> serializedMessages = new java.util.ArrayList<>();
        
        for (Msg msg : messages) {
            serializedMessages.add(msg.toDict());
        }
        
        return Map.of(
            "messages", serializedMessages,
            "metadata", getMemoryStats(),
            "export_timestamp", java.time.LocalDateTime.now().toString()
        );
    }
    
    /**
     * Import memory content from a serialized format.
     * 
     * @param memoryData The memory data to import
     */
    @SuppressWarnings("unchecked")
    public void importMemory(Map<String, Object> memoryData) {
        if (memoryData.containsKey("messages")) {
            List<Map<String, Object>> messageData = 
                (List<Map<String, Object>>) memoryData.get("messages");
            
            clear(); // Clear existing memory
            
            for (Map<String, Object> msgData : messageData) {
                Msg msg = Msg.fromDict(msgData);
                addMessage(msg);
            }
        }
    }
    
    /**
     * Create a snapshot of the current memory state.
     * 
     * @return Memory snapshot
     */
    public MemorySnapshot createSnapshot() {
        return new MemorySnapshot(getMessages(), getMemoryStats());
    }
    
    /**
     * Restore memory from a snapshot.
     * 
     * @param snapshot The snapshot to restore from
     */
    public void restoreFromSnapshot(MemorySnapshot snapshot) {
        clear();
        addMessages(snapshot.getMessages());
    }
    
    @Override
    public String toString() {
        return String.format("%s{messageCount=%d, estimatedTokens=%d}",
                           getClass().getSimpleName(),
                           getMessageCount(),
                           getEstimatedTokenCount());
    }
    
    /**
     * Inner class representing a memory snapshot
     */
    public static class MemorySnapshot {
        private final List<Msg> messages;
        private final Map<String, Object> metadata;
        private final String timestamp;
        
        public MemorySnapshot(List<Msg> messages, Map<String, Object> metadata) {
            this.messages = new java.util.ArrayList<>(messages);
            this.metadata = new java.util.HashMap<>(metadata);
            this.timestamp = java.time.LocalDateTime.now().toString();
        }
        
        public List<Msg> getMessages() {
            return new java.util.ArrayList<>(messages);
        }
        
        public Map<String, Object> getMetadata() {
            return new java.util.HashMap<>(metadata);
        }
        
        public String getTimestamp() {
            return timestamp;
        }
    }
}