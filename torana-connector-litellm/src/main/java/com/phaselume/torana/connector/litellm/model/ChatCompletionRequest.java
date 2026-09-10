package com.phaselume.torana.connector.litellm.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OpenAI-compatible Chat Completions payload for LiteLLM.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatCompletionRequest {

    private String model;
    
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();
    
    private Double temperature;
    
    @JsonProperty("max_tokens")
    private Integer maxTokens;
    
    @Builder.Default
    private boolean stream = true;
    
    private List<Map<String, Object>> tools;
    
    @JsonProperty("tool_choice")
    private Object toolChoice;
    
    private Map<String, Object> metadata;
}
