package de.l3s.learnweb.search.chat;

import java.util.ArrayList;

import com.google.gson.annotations.SerializedName;

class ChatCompletionResponse {
    String id;
    String object;
    int created;
    String model;
    ArrayList<Choice> choices;
    Usage usage;

    static class Usage {
        @SerializedName("completion_tokens")
        int completionTokens;
        @SerializedName("prompt_tokens")
        int promptTokens;
        @SerializedName("total_tokens")
        int totalTokens;
    }

    static class Choice {
        int index;
        @SerializedName("finish_reason")
        String finishReason;
        ChatMessage message;
    }
}
