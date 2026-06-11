package com.example.ordertools;

import com.example.ordertools.tools.OrderTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class OrderToolsApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderToolsApplication.class, args);
    }

    /**
     * Exposes the {@code @Tool}-annotated methods on {@link OrderTools} to MCP clients.
     */
    @Bean
    public ToolCallbackProvider orderToolCallbackProvider(OrderTools orderTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(orderTools)
                .build();
    }
}