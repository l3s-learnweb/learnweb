package de.l3s.learnweb.search;

import java.io.Serial;
import java.io.Serializable;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.search.chat.ChatMessage;
import de.l3s.learnweb.search.chat.OpenAIException;
import de.l3s.learnweb.search.chat.OpenAIService;

@Named
@ViewScoped
public class SearchChatBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = -5707201029228079572L;
    private static final Logger log = LogManager.getLogger(SearchChatBean.class);

    // query params
    private String query = "";

    // input fields
    private String message;

    private List<ChatMessage> messages;

    @Inject
    private OpenAIService openAIService;

    public void onLoad() throws OpenAIException {
        messages = new ArrayList<>();
        // This is the prompt that the bot will refer back to for every message.
        messages.add(ChatMessage.system("You are Learnweb Assistant, a helpful chat bot."));

        if (StringUtils.isNotBlank(query)) {
            query = URLDecoder.decode(query, StandardCharsets.UTF_8);

            // Add the query to the conversation as first prompt
            messages.add(ChatMessage.user(query));

            ChatMessage assistantMessage = openAIService.completeChat(messages);
            messages.add(assistantMessage);
        }
    }

    public void sendMessage() throws OpenAIException {
        messages.add(ChatMessage.user(message));
        message = null;

        ChatMessage assistantMessage = openAIService.completeChat(messages);
        messages.add(assistantMessage);
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(final String query) {
        this.query = query;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(final List<ChatMessage> messages) {
        this.messages = messages;
    }
}
