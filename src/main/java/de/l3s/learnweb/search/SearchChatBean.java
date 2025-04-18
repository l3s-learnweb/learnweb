package de.l3s.learnweb.search;

import java.io.Serial;
import java.io.Serializable;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;
import org.primefaces.model.DialogFrameworkOptions;

import de.l3s.interweb.client.Interweb;
import de.l3s.interweb.client.InterwebException;
import de.l3s.interweb.core.chat.Conversation;
import de.l3s.interweb.core.chat.Message;
import de.l3s.interweb.core.chat.Role;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.user.Settings;

@Named
@ViewScoped
public class SearchChatBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = -5707201029228079572L;

    // query params
    private String query = "";

    // input fields
    private String message;

    // output fields
    private boolean newChat;
    private Interweb interweb;
    private transient Conversation conversation;
    private transient List<Conversation> conversations;

    private transient Integer promptSurveyId;
    private transient Integer responseSurveyId;

    public void onLoad() throws InterwebException {
        BeanAssert.authorized(isLoggedIn());
        BeanAssert.hasPermission(getUser().isAdmin() || getUserBean().isSearchChatEnabled());
        interweb = getLearnweb().getInterweb();
        promptSurveyId = getUser().getOrganisation().getSettings().getIntValue(Settings.chat_feedback_prompt_survey_page_id);
        responseSurveyId = getUser().getOrganisation().getSettings().getIntValue(Settings.chat_feedback_response_survey_page_id);

        newChat();
        if (StringUtils.isNotBlank(query)) {
            // Add the query to the conversation as first prompt
            message = URLDecoder.decode(query, StandardCharsets.UTF_8);
            sendMessage();
        }
    }

    public void newChat() {
        conversation = new Conversation();
        conversation.setSave(true);
        conversation.setModel("gpt-4o-mini");
        conversation.setUser(String.valueOf(getUser().getId()));
        // This is the prompt that the bot will refer back to for every message.
        conversation.addMessage("You are Learnweb Assistant, a helpful chat bot.", Role.system);
        newChat = true;
    }

    public void retry() throws InterwebException {
        conversation.getMessages().removeLast();
        interweb.chatComplete(conversation);
    }

    public void sendMessage() throws InterwebException {
        conversation.addMessage(message, Role.user);
        message = null;

        interweb.chatComplete(conversation);
        if (!getConversations().contains(conversation)) {
            getConversations().addFirst(conversation);
        }
    }

    public void switchConversation(Conversation conv) throws InterwebException {
        if (conv.getMessages() == null || conv.getMessages().isEmpty()) {
            Conversation retrieved = interweb.chatById(conv.getId().toString());
            conv.setMessages(retrieved.getMessages());
        }

        this.conversation = conv;
        newChat = false;
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

    public Conversation getConversation() {
        if (conversation == null) {
            newChat();
        }
        return conversation;
    }

    public List<Conversation> getConversations() throws InterwebException {
        if (conversations == null) {
            conversations = interweb.chatAll(String.valueOf(getUser().getId()));
        }
        return conversations;
    }

    public List<Message> getMessages() {
        return conversation.getMessages();
    }

    public boolean isNewChat() {
        return newChat;
    }

    public Integer getPromptSurveyId() {
        return promptSurveyId;
    }

    public Integer getResponseSurveyId() {
        return responseSurveyId;
    }

    public static DialogFrameworkOptions.Builder defaultBuilder() {
        return DialogFrameworkOptions.builder()
            .modal(true)
            .fitViewport(true)
            .responsive(true)
            .resizable(false)
            .resizeObserver(true)
            .resizeObserverCenter(true)
            .closable(true)
            .closeOnEscape(true)
            .draggable(false)
            .width("660px")
            .contentWidth("100%");
    }

    public static DialogFrameworkOptions defaultOptions() {
        return defaultBuilder().build();
    }

    public void showFeedback(Message message, boolean prompt) {
        Map<String, List<String>> params = new HashMap<>();
        params.put("survey_id", List.of(String.valueOf(prompt ? promptSurveyId : responseSurveyId)));
        params.put("message_id", List.of(String.valueOf(message.getId())));

        PrimeFaces.current().dialog().openDynamic("/dialogs/chat-feedback", defaultOptions(), params);
    }
}
