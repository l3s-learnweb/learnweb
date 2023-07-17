package de.l3s.learnweb.search.chat;

import java.time.Instant;

public class ChatMessage {
    enum Role {
        system,
        user,
        assistant
    }

    private final Role role;
    private String content;
    private transient Instant time;

    public ChatMessage(final Role role) {
        this.role = role;
    }

    public ChatMessage(Role role, String content) {
        this.role = role;
        this.content = content;
        this.time = Instant.now();
    }

    public Role getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    public Instant getTime() {
        return time;
    }

    public void setTime(final Instant time) {
        this.time = time;
    }

    public static ChatMessage system(String content) {
        return new ChatMessage(Role.system, content);
    }

    public static ChatMessage user(String content) {
        return new ChatMessage(Role.user, content);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage(Role.assistant, content);
    }
}
