package de.l3s.learnweb.search.chat;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class ChatCompletionRequest {

    Double temperature = 0.7;
    @SerializedName("top_p")
    Double topP = 0.95;
    @SerializedName("frequency_penalty")
    Double frequencyPenalty = 0.0;
    @SerializedName("presence_penalty")
    Double presencePenalty = 0.0;
    @SerializedName("max_tokens")
    Integer maxTokens = 800;
    List<ChatMessage> messages = new ArrayList<>();

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(final Double temperature) {
        this.temperature = temperature;
    }

    public Double getTopP() {
        return topP;
    }

    public void setTopP(final Double topP) {
        this.topP = topP;
    }

    public Double getFrequencyPenalty() {
        return frequencyPenalty;
    }

    public void setFrequencyPenalty(final Double frequencyPenalty) {
        this.frequencyPenalty = frequencyPenalty;
    }

    public Double getPresencePenalty() {
        return presencePenalty;
    }

    public void setPresencePenalty(final Double presencePenalty) {
        this.presencePenalty = presencePenalty;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(final Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(final List<ChatMessage> messages) {
        this.messages = messages;
    }
}
