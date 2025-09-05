package com.agentscope;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * AgentScope4J Spring Boot Application
 * 
 * @author Alex Huangfu
 * @version 1.0.0
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.agentscope")
public class AgentScopeApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentScopeApplication.class, args);
    }
}