package com.agentscope.message;

/**
 * Thinking content block
 */
class ThinkingBlock implements ContentBlock {
    private String type = "thinking";
    private String thinking;

    public ThinkingBlock() {
    }

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
