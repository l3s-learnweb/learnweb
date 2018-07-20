package de.l3s.learnweb.chat;

import java.io.Serializable;

import javax.faces.application.FacesMessage;
import javax.inject.Inject;
import javax.inject.Named;
import javax.faces.view.ViewScoped;
import javax.faces.context.FacesContext;

import org.primefaces.PrimeFaces;
import org.primefaces.push.EventBus;
import org.primefaces.push.EventBusFactory;

import de.l3s.learnweb.beans.ApplicationBean;

@Named
@ViewScoped
public class ChatView extends ApplicationBean implements Serializable
{

    //private final PushContext pushContext = PushContextFactory.getDefault().getPushContext();

    private static final long serialVersionUID = -2190983402628583947L;

    private final EventBus eventBus;

    @Inject
    private ChatUsers chatUsers;

    private String privateMessage;

    private String globalMessage;

    private String username;

    private boolean loggedIn;

    private String privateUser;

    private final static String CHANNEL = "/{room}/";

    public ChatView()
    {
        if(isLoggedIn())
            username = getUser().getUsername();

        EventBusFactory bla = EventBusFactory.getDefault();
        eventBus = bla.eventBus();

    }

    public ChatUsers getChatUsers()
    {
        return chatUsers;
    }

    public void setChatUsers(ChatUsers chatUsers)
    {
        this.chatUsers = chatUsers;
    }

    public String getPrivateUser()
    {
        return privateUser;
    }

    public void setPrivateUser(String privateUser)
    {
        this.privateUser = privateUser;
    }

    public String getGlobalMessage()
    {
        return globalMessage;
    }

    public void setGlobalMessage(String globalMessage)
    {
        this.globalMessage = globalMessage;
    }

    public String getPrivateMessage()
    {
        return privateMessage;
    }

    public void setPrivateMessage(String privateMessage)
    {
        this.privateMessage = privateMessage;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public boolean isLoggedIn()
    {
        return loggedIn;
    }

    public void setLoggedIn(boolean loggedIn)
    {
        this.loggedIn = loggedIn;
    }

    public void sendGlobal()
    {
        eventBus.publish(CHANNEL + "*", username + ": " + globalMessage);

        globalMessage = null;
    }

    public void sendPrivate()
    {
        eventBus.publish(CHANNEL + privateUser, "[PM] " + username + ": " + privateMessage);

        privateMessage = null;
    }

    public void login()
    {
        if(chatUsers.contains(username))
        {
            loggedIn = false;
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Username taken", "Try with another username."));
            PrimeFaces.current().ajax().update("growl");
        }
        else
        {
            chatUsers.add(username);
            PrimeFaces.current().executeScript("PF('subscriber').connect('/" + username + "')");
            loggedIn = true;
        }
    }

    public void disconnect()
    {
        //remove user and update ui
        chatUsers.remove(username);
        PrimeFaces.current().ajax().update("form:users");

        //push leave information
        eventBus.publish(CHANNEL + "*", username + " left the channel.");

        //reset state
        loggedIn = false;
        username = null;
    }
}
