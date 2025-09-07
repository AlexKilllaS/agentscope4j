package com.agentscope.message;

/**
 * Audio content block
 */
class AudioBlock implements ContentBlock {
    private String type = "audio";
    private Object source; // Base64Source or URLSource

    public AudioBlock() {
    }

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
