package de.l3s.learnweb.llm;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.jboss.weld.junit5.auto.ExcludeBean;
import org.junit.jupiter.api.Test;

import de.l3s.interweb.client.Interweb;
import de.l3s.interweb.client.InterwebException;
import de.l3s.interweb.core.chat.Conversation;
import de.l3s.learnweb.app.ConfigProvider;

@EnableAutoWeld
class LlmSearchChatTest {

    @Produces
    @ExcludeBean
    ConfigProvider config = new ConfigProvider(false);

    @Produces
    @ExcludeBean
    Interweb interweb = interweb = new Interweb(config.getProperty("integration_interweb_url"), config.getProperty("integration_interweb_apikey"));

    @Inject
    LlmSearchChat llmSearch;

    @Test
    void searchConversation() throws InterwebException {
        Conversation conversation = llmSearch.searchConversation("What is the weather today in Hannover?");
        conversation.setSave(false);
        interweb.chatComplete(conversation);

        assertNotNull(conversation.getMessages().getLast());
    }
}
