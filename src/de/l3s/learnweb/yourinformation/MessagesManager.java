package de.l3s.learnweb.yourinformation;

import de.l3s.learnweb.user.Message;

import java.util.ArrayList;
import java.util.List;

public class MessagesManager {
    public List<Message> getAllUserMessages(){
        List<Message> messages = new ArrayList<>();

        String query = "SELECT t.* FROM learnweb_main.message t WHERE from_user = ";

        return messages;
    }
}
