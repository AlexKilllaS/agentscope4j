package com.agentscope.memory;

import com.agentscope.message.Msg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * In-memory implementation of MemoryBase for AgentScope4J.
 * Stores messages in memory with thread-safe operations.
 * 
 * @author Alex Huangfu
 * @version 1.0.0
 */
@Component
public class InMemoryMemory extends MemoryBase {
    
    private static final Logger logger = LoggerFactory.getLogger(InMemoryMemory.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    // Thread-safe storage for messages
    private final Queue<Msg> messages = new ConcurrentLinkedQueue<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    // Configuration
    private int maxMessages = 1000; // Maximum number of messages to store
    private boolean autoTruncate = true; // Automatically remove old messages when limit is reached
    
    /**
     * Default constructor
     */
    public InMemoryMemory() {
        super();
        logger.debug("Initialized InMemoryMemory with maxMessages={}", maxMessages);
    }
    
    /**
     * Constructor with custom configuration
     * 
     * @param maxMessages Maximum number of messages to store
     * @param autoTruncate Whether to automatically truncate old messages
     */
    public InMemoryMemory(int maxMessages, boolean autoTruncate) {
        super();
        this.maxMessages = maxMessages;
        this.autoTruncate = autoTruncate;
        logger.debug("Initialized InMemoryMemory with maxMessages={}, autoTruncate={}", 
                    maxMessages, autoTruncate);
    }
    
    @Override
    public void addMessage(Msg message) {
        if (message == null) {
            logger.warn("Attempted to add null message to memory");
            return;
        }
        
        lock.writeLock().lock();
        try {
            messages.offer(message);
            logger.debug("Added message to memory: {}", message.getId());
            
            // Auto-truncate if enabled and limit exceeded
            if (autoTruncate && messages.size() > maxMessages) {
                Msg removed = messages.poll();
                if (removed != null) {
                    logger.debug("Auto-truncated old message: {}", removed.getId());
                }
            }
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public List<Msg> getMessages() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(messages);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public List<Msg> getMessages(Map<String, Object> filter) {
        if (filter == null || filter.isEmpty()) {
            return getMessages();
        }
        
        lock.readLock().lock();
        try {
            return messages.stream()
                .filter(msg -> matchesFilter(msg, filter))
                .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public List<Msg> getRecentMessages(int count) {
        if (count <= 0) {
            return new ArrayList<>();
        }
        
        lock.readLock().lock();
        try {
            List<Msg> allMessages = new ArrayList<>(messages);
            int size = allMessages.size();
            
            if (count >= size) {
                return allMessages;
            }
            
            return allMessages.subList(size - count, size);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public List<Msg> getMessagesByRole(String role) {
        if (role == null) {
            return new ArrayList<>();
        }
        
        lock.readLock().lock();
        try {
            return messages.stream()
                .filter(msg -> role.equals(msg.getRole()))
                .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public int getMessageCount() {
        lock.readLock().lock();
        try {
            return messages.size();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            int count = messages.size();
            messages.clear();
            logger.info("Cleared {} messages from memory", count);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public boolean removeMessage(String messageId) {
        if (messageId == null) {
            return false;
        }
        
        lock.writeLock().lock();
        try {
            boolean removed = messages.removeIf(msg -> messageId.equals(msg.getId()));
            if (removed) {
                logger.debug("Removed message from memory: {}", messageId);
            }
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public int removeMessagesOlderThan(String timestamp) {
        if (timestamp == null) {
            return 0;
        }
        
        lock.writeLock().lock();
        try {
            int initialSize = messages.size();
            messages.removeIf(msg -> isOlderThan(msg.getTimestamp(), timestamp));
            int removed = initialSize - messages.size();
            
            if (removed > 0) {
                logger.info("Removed {} messages older than {}", removed, timestamp);
            }
            
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Get messages within a specific time range
     * 
     * @param startTime Start timestamp (inclusive)
     * @param endTime End timestamp (inclusive)
     * @return List of messages within the time range
     */
    public List<Msg> getMessagesInTimeRange(String startTime, String endTime) {
        lock.readLock().lock();
        try {
            return messages.stream()
                .filter(msg -> isInTimeRange(msg.getTimestamp(), startTime, endTime))
                .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Get messages by sender name
     * 
     * @param senderName The name of the sender
     * @return List of messages from the specified sender
     */
    public List<Msg> getMessagesBySender(String senderName) {
        if (senderName == null) {
            return new ArrayList<>();
        }
        
        lock.readLock().lock();
        try {
            return messages.stream()
                .filter(msg -> senderName.equals(msg.getName()))
                .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Truncate memory to keep only the most recent N messages
     * 
     * @param keepCount Number of recent messages to keep
     * @return Number of messages removed
     */
    public int truncateToRecent(int keepCount) {
        if (keepCount < 0) {
            return 0;
        }
        
        lock.writeLock().lock();
        try {
            int currentSize = messages.size();
            
            if (currentSize <= keepCount) {
                return 0; // No truncation needed
            }
            
            // Convert to list, keep recent messages, clear and re-add
            List<Msg> allMessages = new ArrayList<>(messages);
            List<Msg> recentMessages = allMessages.subList(
                currentSize - keepCount, currentSize);
            
            messages.clear();
            messages.addAll(recentMessages);
            
            int removed = currentSize - keepCount;
            logger.info("Truncated memory: kept {} recent messages, removed {}", 
                       keepCount, removed);
            
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Check if a message matches the given filter criteria
     */
    private boolean matchesFilter(Msg msg, Map<String, Object> filter) {
        for (Map.Entry<String, Object> entry : filter.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            switch (key) {
                case "role":
                    if (!Objects.equals(msg.getRole(), value)) {
                        return false;
                    }
                    break;
                case "name":
                    if (!Objects.equals(msg.getName(), value)) {
                        return false;
                    }
                    break;
                case "contains_text":
                    String textContent = msg.getTextContent();
                    if (textContent == null || !textContent.contains(value.toString())) {
                        return false;
                    }
                    break;
                case "after_timestamp":
                    if (isOlderThan(msg.getTimestamp(), value.toString())) {
                        return false;
                    }
                    break;
                case "before_timestamp":
                    if (!isOlderThan(msg.getTimestamp(), value.toString())) {
                        return false;
                    }
                    break;
                default:
                    // Unknown filter key, ignore
                    break;
            }
        }
        
        return true;
    }
    
    /**
     * Check if timestamp1 is older than timestamp2
     */
    private boolean isOlderThan(String timestamp1, String timestamp2) {
        try {
            LocalDateTime time1 = LocalDateTime.parse(timestamp1, TIMESTAMP_FORMAT);
            LocalDateTime time2 = LocalDateTime.parse(timestamp2, TIMESTAMP_FORMAT);
            return time1.isBefore(time2);
        } catch (Exception e) {
            logger.warn("Error comparing timestamps: {} vs {}", timestamp1, timestamp2, e);
            return false;
        }
    }
    
    /**
     * Check if timestamp is within the given time range
     */
    private boolean isInTimeRange(String timestamp, String startTime, String endTime) {
        try {
            LocalDateTime time = LocalDateTime.parse(timestamp, TIMESTAMP_FORMAT);
            LocalDateTime start = LocalDateTime.parse(startTime, TIMESTAMP_FORMAT);
            LocalDateTime end = LocalDateTime.parse(endTime, TIMESTAMP_FORMAT);
            
            return !time.isBefore(start) && !time.isAfter(end);
        } catch (Exception e) {
            logger.warn("Error checking time range for timestamp: {}", timestamp, e);
            return false;
        }
    }
    
    @Override
    protected void copyStateFrom(Object other) {
        if (other instanceof InMemoryMemory) {
            InMemoryMemory otherMemory = (InMemoryMemory) other;
            
            lock.writeLock().lock();
            try {
                this.messages.clear();
                this.messages.addAll(otherMemory.getMessages());
                this.maxMessages = otherMemory.maxMessages;
                this.autoTruncate = otherMemory.autoTruncate;
                
                logger.debug("Copied state from another InMemoryMemory instance");
            } finally {
                lock.writeLock().unlock();
            }
        }
    }
    
    @Override
    public void reset() {
        clear();
        logger.info("Reset InMemoryMemory");
    }
    
    // Getters and Setters
    public int getMaxMessages() {
        return maxMessages;
    }
    
    public void setMaxMessages(int maxMessages) {
        lock.writeLock().lock();
        try {
            this.maxMessages = maxMessages;
            
            // Truncate if current size exceeds new limit
            if (autoTruncate && messages.size() > maxMessages) {
                truncateToRecent(maxMessages);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public boolean isAutoTruncate() {
        return autoTruncate;
    }
    
    public void setAutoTruncate(boolean autoTruncate) {
        this.autoTruncate = autoTruncate;
    }
}