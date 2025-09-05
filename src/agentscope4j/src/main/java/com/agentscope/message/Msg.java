package com.agentscope.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The message class in AgentScope4J.
 * 
 * @author Alex Huangfu
 * @version 1.0.0
 */
@Component
public class Msg {
    
    private String id;
    private String name;
    private Object content; // Can be String or List<ContentBlock>
    private String role; // "user", "assistant", "system"
    private Map<String, Object> metadata;
    private String timestamp;
    private String invocationId;
    
    private static final DateTimeFormatter TIMESTAMP_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    /**
     * Default constructor for Spring
     */
    public Msg() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
    }
    
    /**
     * Constructor for creating a message
     * 
     * @param name The name of the message sender
     * @param content The content of the message
     * @param role The role of the message sender
     */
    public Msg(String name, Object content, String role) {
        this(name, content, role, null, null, null);
    }
    
    /**
     * Full constructor for creating a message
     * 
     * @param name The name of the message sender
     * @param content The content of the message (String or List<ContentBlock>)
     * @param role The role of the message sender ("user", "assistant", "system")
     * @param metadata The metadata of the message
     * @param timestamp The created timestamp of the message
     * @param invocationId The related API invocation id
     */
    @JsonCreator
    public Msg(
            @JsonProperty("name") String name,
            @JsonProperty("content") Object content,
            @JsonProperty("role") String role,
            @JsonProperty("metadata") Map<String, Object> metadata,
            @JsonProperty("timestamp") String timestamp,
            @JsonProperty("invocationId") String invocationId) {
        
        this.name = name;
        this.content = content;
        
        // Validate role
        if (!Arrays.asList("user", "assistant", "system").contains(role)) {
            throw new IllegalArgumentException("Role must be one of: user, assistant, system");
        }
        this.role = role;
        
        this.metadata = metadata;
        this.id = UUID.randomUUID().toString();
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now().format(TIMESTAMP_FORMAT);
        this.invocationId = invocationId;
    }
    
    /**
     * Convert the message into a Map for JSON serialization
     * 
     * @return Map representation of the message
     */
    public Map<String, Object> toDict() {
        Map<String, Object> dict = new HashMap<>();
        dict.put("id", id);
        dict.put("name", name);
        dict.put("role", role);
        dict.put("content", content);
        dict.put("metadata", metadata);
        dict.put("timestamp", timestamp);
        if (invocationId != null) {
            dict.put("invocationId", invocationId);
        }
        return dict;
    }
    
    /**
     * Create a message object from a Map
     * 
     * @param jsonData Map containing message data
     * @return New Msg object
     */
    public static Msg fromDict(Map<String, Object> jsonData) {
        Msg msg = new Msg(
            (String) jsonData.get("name"),
            jsonData.get("content"),
            (String) jsonData.get("role"),
            (Map<String, Object>) jsonData.get("metadata"),
            (String) jsonData.get("timestamp"),
            (String) jsonData.get("invocationId")
        );
        
        if (jsonData.containsKey("id")) {
            msg.id = (String) jsonData.get("id");
        }
        
        return msg;
    }
    
    /**
     * Check if the message has content blocks of the given type
     * 
     * @param blockType The type of block to check for (null to check for any blocks)
     * @return true if blocks of the specified type exist
     */
    public boolean hasContentBlocks(String blockType) {
        return !getContentBlocks(blockType).isEmpty();
    }
    
    /**
     * Get the pure text content from the message
     * 
     * @return Text content or null if no text found
     */
    public String getTextContent() {
        if (content instanceof String) {
            return (String) content;
        }
        
        if (content instanceof List) {
            StringBuilder gatheredText = new StringBuilder();
            List<?> contentList = (List<?>) content;
            
            for (Object block : contentList) {
                if (block instanceof Map) {
                    Map<?, ?> blockMap = (Map<?, ?>) block;
                    if ("text".equals(blockMap.get("type"))) {
                        Object text = blockMap.get("text");
                        if (text != null) {
                            gatheredText.append(text.toString());
                        }
                    }
                }
            }
            
            return gatheredText.length() > 0 ? gatheredText.toString() : null;
        }
        
        return null;
    }
    
    /**
     * Get content blocks of the specified type
     * 
     * @param blockType The type of blocks to retrieve (null for all blocks)
     * @return List of content blocks
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getContentBlocks(String blockType) {
        if (!(content instanceof List)) {
            return new ArrayList<>();
        }
        
        List<?> contentList = (List<?>) content;
        return contentList.stream()
            .filter(block -> block instanceof Map)
            .map(block -> (Map<String, Object>) block)
            .filter(blockMap -> blockType == null || blockType.equals(blockMap.get("type")))
            .collect(Collectors.toList());
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
    
    public Object getContent() {
        return content;
    }
    
    public void setContent(Object content) {
        this.content = content;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        if (!Arrays.asList("user", "assistant", "system").contains(role)) {
            throw new IllegalArgumentException("Role must be one of: user, assistant, system");
        }
        this.role = role;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getInvocationId() {
        return invocationId;
    }
    
    public void setInvocationId(String invocationId) {
        this.invocationId = invocationId;
    }
    
    @Override
    public String toString() {
        return String.format("Msg{id='%s', name='%s', role='%s', timestamp='%s'}", 
                           id, name, role, timestamp);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Msg msg = (Msg) obj;
        return Objects.equals(id, msg.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}