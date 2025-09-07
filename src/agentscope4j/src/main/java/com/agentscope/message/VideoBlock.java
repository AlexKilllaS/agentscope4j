package com.agentscope.message;

/**
 * Video content block
 */
class VideoBlock implements ContentBlock {
    private String type = "video";
    private Object source; // Base64Source or URLSource

    public VideoBlock() {
    }

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
