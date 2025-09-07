package com.agentscope.message;

import java.util.Map;

/**
 * Tool use content block
 */
class ToolUseBlock implements ContentBlock {
    private String type = "tool_use";
    private String id;
    private String name;
    private Map<String, Object> input;

    public ToolUseBlock() {
    }

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
