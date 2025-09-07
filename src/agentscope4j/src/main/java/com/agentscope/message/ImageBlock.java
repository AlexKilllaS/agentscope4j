package com.agentscope.message;

/**
 * Image content block
 */
class ImageBlock implements ContentBlock {
    private String type = "image";
    private Object source; // Base64Source or URLSource

    public ImageBlock() {
    }

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
