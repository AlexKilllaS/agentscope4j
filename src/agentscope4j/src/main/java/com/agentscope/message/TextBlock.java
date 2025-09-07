package com.agentscope.message;

/**
 * Text content block
 */
public class TextBlock implements ContentBlock {
    private String type = "text";
    private String text;

    public TextBlock() {
    }

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
