package com.agentscope.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for AgentScope4J.
 * 
 * @author Alex Huangfu
 * @version 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "agentscope")
@Validated
public class AgentScopeConfig {
    
    @Valid
    private Models models = new Models();
    
    @Valid
    private Agents agents = new Agents();
    
    @Valid
    private Memory memory = new Memory();
    
    @Valid
    private Tools tools = new Tools();
    
    /**
     * Model configuration
     */
    public static class Models {
        
        @Valid
        private OpenAI openai = new OpenAI();
        
        @Valid
        private DashScope dashscope = new DashScope();
        
        @Valid
        private Gemini gemini = new Gemini();
        
        public static class OpenAI {
            private String apiKey;
            private String baseUrl = "https://api.openai.com/v1";
            
            @Min(1000)
            private long timeout = 30000;
            
            // Getters and Setters
            public String getApiKey() {
                return apiKey;
            }
            
            public void setApiKey(String apiKey) {
                this.apiKey = apiKey;
            }
            
            public String getBaseUrl() {
                return baseUrl;
            }
            
            public void setBaseUrl(String baseUrl) {
                this.baseUrl = baseUrl;
            }
            
            public long getTimeout() {
                return timeout;
            }
            
            public void setTimeout(long timeout) {
                this.timeout = timeout;
            }
        }
        
        public static class DashScope {
            private String apiKey;
            private String baseUrl = "https://dashscope.aliyuncs.com/api/v1";
            
            @Min(1000)
            private long timeout = 30000;
            
            // Getters and Setters
            public String getApiKey() {
                return apiKey;
            }
            
            public void setApiKey(String apiKey) {
                this.apiKey = apiKey;
            }
            
            public String getBaseUrl() {
                return baseUrl;
            }
            
            public void setBaseUrl(String baseUrl) {
                this.baseUrl = baseUrl;
            }
            
            public long getTimeout() {
                return timeout;
            }
            
            public void setTimeout(long timeout) {
                this.timeout = timeout;
            }
        }
        
        public static class Gemini {
            private String apiKey;
            private String baseUrl = "https://generativelanguage.googleapis.com/v1";
            
            @Min(1000)
            private long timeout = 30000;
            
            // Getters and Setters
            public String getApiKey() {
                return apiKey;
            }
            
            public void setApiKey(String apiKey) {
                this.apiKey = apiKey;
            }
            
            public String getBaseUrl() {
                return baseUrl;
            }
            
            public void setBaseUrl(String baseUrl) {
                this.baseUrl = baseUrl;
            }
            
            public long getTimeout() {
                return timeout;
            }
            
            public void setTimeout(long timeout) {
                this.timeout = timeout;
            }
        }
        
        // Getters and Setters for Models
        public OpenAI getOpenai() {
            return openai;
        }
        
        public void setOpenai(OpenAI openai) {
            this.openai = openai;
        }
        
        public DashScope getDashscope() {
            return dashscope;
        }
        
        public void setDashscope(DashScope dashscope) {
            this.dashscope = dashscope;
        }
        
        public Gemini getGemini() {
            return gemini;
        }
        
        public void setGemini(Gemini gemini) {
            this.gemini = gemini;
        }
    }
    
    /**
     * Agent configuration
     */
    public static class Agents {
        
        @Min(1000)
        private long defaultTimeout = 60000;
        
        @Min(1)
        private int maxRetry = 3;
        
        // Getters and Setters
        public long getDefaultTimeout() {
            return defaultTimeout;
        }
        
        public void setDefaultTimeout(long defaultTimeout) {
            this.defaultTimeout = defaultTimeout;
        }
        
        public int getMaxRetry() {
            return maxRetry;
        }
        
        public void setMaxRetry(int maxRetry) {
            this.maxRetry = maxRetry;
        }
    }
    
    /**
     * Memory configuration
     */
    public static class Memory {
        
        @Min(1)
        private int maxHistory = 1000;
        
        private boolean enableLongTerm = false;
        
        // Getters and Setters
        public int getMaxHistory() {
            return maxHistory;
        }
        
        public void setMaxHistory(int maxHistory) {
            this.maxHistory = maxHistory;
        }
        
        public boolean isEnableLongTerm() {
            return enableLongTerm;
        }
        
        public void setEnableLongTerm(boolean enableLongTerm) {
            this.enableLongTerm = enableLongTerm;
        }
    }
    
    /**
     * Tools configuration
     */
    public static class Tools {
        
        @Min(1000)
        private long executionTimeout = 30000;
        
        private boolean enableAsync = true;
        
        // Getters and Setters
        public long getExecutionTimeout() {
            return executionTimeout;
        }
        
        public void setExecutionTimeout(long executionTimeout) {
            this.executionTimeout = executionTimeout;
        }
        
        public boolean isEnableAsync() {
            return enableAsync;
        }
        
        public void setEnableAsync(boolean enableAsync) {
            this.enableAsync = enableAsync;
        }
    }
    
    // Main getters and setters
    public Models getModels() {
        return models;
    }
    
    public void setModels(Models models) {
        this.models = models;
    }
    
    public Agents getAgents() {
        return agents;
    }
    
    public void setAgents(Agents agents) {
        this.agents = agents;
    }
    
    public Memory getMemory() {
        return memory;
    }
    
    public void setMemory(Memory memory) {
        this.memory = memory;
    }
    
    public Tools getTools() {
        return tools;
    }
    
    public void setTools(Tools tools) {
        this.tools = tools;
    }
    
    /**
     * Get all configuration as a map
     * 
     * @return Configuration map
     */
    public Map<String, Object> toMap() {
        Map<String, Object> config = new HashMap<>();
        
        // Models configuration
        Map<String, Object> modelsMap = new HashMap<>();
        modelsMap.put("openai", Map.of(
            "apiKey", models.openai.apiKey != null ? "***" : null,
            "baseUrl", models.openai.baseUrl,
            "timeout", models.openai.timeout
        ));
        modelsMap.put("dashscope", Map.of(
            "apiKey", models.dashscope.apiKey != null ? "***" : null,
            "baseUrl", models.dashscope.baseUrl,
            "timeout", models.dashscope.timeout
        ));
        modelsMap.put("gemini", Map.of(
            "apiKey", models.gemini.apiKey != null ? "***" : null,
            "baseUrl", models.gemini.baseUrl,
            "timeout", models.gemini.timeout
        ));
        config.put("models", modelsMap);
        
        // Agents configuration
        config.put("agents", Map.of(
            "defaultTimeout", agents.defaultTimeout,
            "maxRetry", agents.maxRetry
        ));
        
        // Memory configuration
        config.put("memory", Map.of(
            "maxHistory", memory.maxHistory,
            "enableLongTerm", memory.enableLongTerm
        ));
        
        // Tools configuration
        config.put("tools", Map.of(
            "executionTimeout", tools.executionTimeout,
            "enableAsync", tools.enableAsync
        ));
        
        return config;
    }
    
    /**
     * Validate configuration
     * 
     * @return true if configuration is valid
     */
    public boolean isValid() {
        try {
            // Basic validation checks
            if (models.openai.timeout < 1000 || 
                models.dashscope.timeout < 1000 || 
                models.gemini.timeout < 1000) {
                return false;
            }
            
            if (agents.defaultTimeout < 1000 || agents.maxRetry < 1) {
                return false;
            }
            
            if (memory.maxHistory < 1) {
                return false;
            }
            
            if (tools.executionTimeout < 1000) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public String toString() {
        return String.format("AgentScopeConfig{models=%s, agents=%s, memory=%s, tools=%s}",
                           "[configured]", "[configured]", "[configured]", "[configured]");
    }
}