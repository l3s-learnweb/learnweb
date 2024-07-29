package de.l3s.learnweb.search;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
import de.l3s.interweb.core.chat.CompletionsQuery;
import de.l3s.interweb.core.chat.CompletionsResults;
import de.l3s.interweb.core.chat.Conversation;
import de.l3s.interweb.core.chat.Message;
import de.l3s.interweb.core.chat.Role;
import de.l3s.interweb.core.search.ContentType;
import de.l3s.interweb.core.search.SearchConnectorResults;
import de.l3s.interweb.core.search.SearchItem;
import de.l3s.interweb.core.search.SearchQuery;
import de.l3s.interweb.core.search.SearchResults;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.resource.ResourceDecorator;
import de.l3s.learnweb.resource.search.solrClient.SolrSearch;
import de.l3s.learnweb.user.Settings;

@Named
@ViewScoped
public class SearchChat2Bean extends ApplicationBean implements Serializable {
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

    public void onLoad() throws InterwebException, IOException {
        BeanAssert.authorized(isLoggedIn());
        BeanAssert.hasPermission(getUser().isModerator() || getUserBean().isSearchChatEnabled());
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
        // conversation.setModel("llama3.1:70b");
        conversation.setGenerateTitle(true);
        conversation.setUser(String.valueOf(getUser().getId()));
        // This is the prompt that the bot will refer back to for every message.
        conversation.addMessage("""
            You are Learnweb Assistant, a search assistant.
            """, Role.system);
        newChat = true;
    }

    public void retry() throws InterwebException {
        conversation.getMessages().removeLast();
        interweb.chatComplete(conversation);
    }

    private String systemTask(String instruction, String input) throws InterwebException {
        var messages = new ArrayList<Message>();
        messages.add(new Message(Role.system, instruction));
        messages.add(new Message(Role.user, input));

        CompletionsQuery query = new CompletionsQuery();
        query.setModel("gemma2:9b");
        query.setMessages(messages);
        CompletionsResults results = interweb.chatCompletions(query);
        return results.getLastMessage().getContent();
    }

    public void sendMessage() throws InterwebException, IOException {
        String result = systemTask("""
            Given the user message, determine whether your knowledge is enough to answer it.
            If not, there are two extra options:
            - search: the search results will be provided to you. They include actual information and links.
            - learnweb: the Learnweb data will be provided, they include data from private resources, group information and files of the user.

            Answer only one word, either 'search', 'learnweb' or 'yes'.
            """, message).trim();

        if (result.equalsIgnoreCase("search")) {
            // add Bing search results
            SearchQuery searchQuery = new SearchQuery();
            searchQuery.setPerPage(10);
            searchQuery.setQuery(message);
            searchQuery.setContentTypes(ContentType.webpage);
            searchQuery.setServices("bing");
            SearchResults searchresults = interweb.search(searchQuery);

            if (!searchresults.getResults().isEmpty()) {
                SearchConnectorResults searchResult = searchresults.getResults().getFirst();
                StringBuilder sb = new StringBuilder();

                for (SearchItem item : searchResult.getItems()) {
                    sb.append("- ").append(item.getTitle()).append("\n")
                        .append(item.getUrl()).append("\n")
                        .append(item.getDescription()).append("\n\n");
                }

                conversation.addMessage("""
                    You are an alternate to google search. Your job is to answer the user query in as detailed manner as possible.
                    You have access to the internet and other relevant data related to the user's question.
                    Give time for yourself to read the context and user query and extract relevant data and then answer the query.
                    Give the output structured as a Wikipedia article.

                    Now use the following context items to answer the user query:
                    %s

                    User query:
                    %s
                    """.formatted(sb, message), Role.user);
            } else {
                throw new InterwebException("No search results found for query: " + message);
            }
        } else if (result.equalsIgnoreCase("learnweb")) {
            String solrQuery = systemTask("""
                Create a short, but high quality SOLR query to retrieve resources which are relevant to the user query.
                The search is preformed on all fields, stored in in index, which includes title, description, url, location, etc.
                Do not use any filters or special syntax, only transform query to a simple text. Do not explain your answer.
                """, message).trim();

            // add Learnweb results
            SolrSearch solrSearch = new SolrSearch(solrQuery, getUser(), false);
            List<ResourceDecorator> solrResults = solrSearch.getResourcesByPage(1);
            if (!solrResults.isEmpty()) {
                StringBuilder sb = new StringBuilder();

                for (ResourceDecorator item : solrResults) {
                    sb.append("- ").append(item.getUrl()).append("\n")
                        .append(item.getTitle()).append("\n")
                        .append(item.getDescription()).append("\n\n");
                }

                conversation.addMessage("""
                    You are an alternate to google search. Your job is to answer the user query in as detailed manner as possible.
                    You have access to the relevant data related to the user's question stored on Learnweb.
                    Give time for yourself to read the context and user query and extract relevant data and then answer the query.
                    Give the output structured as a Wikipedia article.

                    Now use the following context items to answer the user query:
                    %s

                    User query:
                    %s
                    """.formatted(sb, message), Role.user);
            }
        } else {
            conversation.addMessage(message, Role.user);
        }

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
