package com.agentscope.message;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

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
 * Base64 source for media content
 */
class Base64Source {
    private String type = "base64";
    private String mediaType;
    private String data;

    public Base64Source() {
    }

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

    public URLSource() {
    }

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

