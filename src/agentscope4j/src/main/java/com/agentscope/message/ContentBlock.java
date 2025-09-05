package com.agentscope.message;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;
import java.util.Map;

/**
 * Base interface for all content blocks in AgentScope4J messages.
 * 
 * @author Alex Huangfu
 * @version 1.0.0
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = TextBlock.class, name = "text"),
    @JsonSubTypes.Type(value = ThinkingBlock.class, name = "thinking"),
    @JsonSubTypes.Type(value = ImageBlock.class, name = "image"),
    @JsonSubTypes.Type(value = AudioBlock.class, name = "audio"),
    @JsonSubTypes.Type(value = VideoBlock.class, name = "video"),
    @JsonSubTypes.Type(value = ToolUseBlock.class, name = "tool_use"),
    @JsonSubTypes.Type(value = ToolResultBlock.class, name = "tool_result")
})
public interface ContentBlock {
    String getType();
}

/**
 * Text content block
 */
class TextBlock implements ContentBlock {
    private String type = "text";
    private String text;
    
    public TextBlock() {}
    
    public TextBlock(String text) {
        this.text = text;
    }
    
    @Override
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
}

/**
 * Thinking content block
 */
class ThinkingBlock implements ContentBlock {
    private String type = "thinking";
    private String thinking;
    
    public ThinkingBlock() {}
    
    public ThinkingBlock(String thinking) {
        this.thinking = thinking;
    }
    
    @Override
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getThinking() {
        return thinking;
    }
    
    public void setThinking(String thinking) {
        this.thinking = thinking;
    }
}

/**
 * Base64 source for media content
 */
class Base64Source {
    private String type = "base64";
    private String mediaType;
    private String data;
    
    public Base64Source() {}
    
    public Base64Source(String mediaType, String data) {
        this.mediaType = mediaType;
        this.data = data;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getMediaType() {
        return mediaType;
    }
    
    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }
    
    public String getData() {
        return data;
    }
    
    public void setData(String data) {
        this.data = data;
    }
}

/**
 * URL source for media content
 */
class URLSource {
    private String type = "url";
    private String url;
    
    public URLSource() {}
    
    public URLSource(String url) {
        this.url = url;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
}

/**
 * Image content block
 */
class ImageBlock implements ContentBlock {
    private String type = "image";
    private Object source; // Base64Source or URLSource
    
    public ImageBlock() {}
    
    public ImageBlock(Object source) {
        this.source = source;
    }
    
    @Override
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Object getSource() {
        return source;
    }
    
    public void setSource(Object source) {
        this.source = source;
    }
}

/**
 * Audio content block
 */
class AudioBlock implements ContentBlock {
    private String type = "audio";
    private Object source; // Base64Source or URLSource
    
    public AudioBlock() {}
    
    public AudioBlock(Object source) {
        this.source = source;
    }
    
    @Override
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Object getSource() {
        return source;
    }
    
    public void setSource(Object source) {
        this.source = source;
    }
}

/**
 * Video content block
 */
class VideoBlock implements ContentBlock {
    private String type = "video";
    private Object source; // Base64Source or URLSource
    
    public VideoBlock() {}
    
    public VideoBlock(Object source) {
        this.source = source;
    }
    
    @Override
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Object getSource() {
        return source;
    }
    
    public void setSource(Object source) {
        this.source = source;
    }
}

/**
 * Tool use content block
 */
class ToolUseBlock implements ContentBlock {
    private String type = "tool_use";
    private String id;
    private String name;
    private Map<String, Object> input;
    
    public ToolUseBlock() {}
    
    public ToolUseBlock(String id, String name, Map<String, Object> input) {
        this.id = id;
        this.name = name;
        this.input = input;
    }
    
    @Override
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
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
    
    public Map<String, Object> getInput() {
        return input;
    }
    
    public void setInput(Map<String, Object> input) {
        this.input = input;
    }
}

/**
 * Tool result content block
 */
class ToolResultBlock implements ContentBlock {
    private String type = "tool_result";
    private String id;
    private Object output; // String or List<ContentBlock>
    
    public ToolResultBlock() {}
    
    public ToolResultBlock(String id, Object output) {
        this.id = id;
        this.output = output;
    }
    
    @Override
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public Object getOutput() {
        return output;
    }
    
    public void setOutput(Object output) {
        this.output = output;
    }
}