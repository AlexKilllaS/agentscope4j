package com.agentscope.message;

/**
 * Tool result content block
 */
class ToolResultBlock implements ContentBlock {
    private String type = "tool_result";
    private String id;
    private Object output; // String or List<ContentBlock>

    public ToolResultBlock() {
    }

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
