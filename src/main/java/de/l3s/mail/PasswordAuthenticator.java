package de.l3s.mail;

import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;

public class PasswordAuthenticator extends Authenticator {
    private final String username;
    private final String password;

    public PasswordAuthenticator(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(username, password);
    }
}
