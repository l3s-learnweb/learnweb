package de.l3s.util.email;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

public class PasswordAuthenticator extends Authenticator {
    private final String user;
    private final String pw;

    public PasswordAuthenticator(String username, String password) {
        this.user = username;
        this.pw = password;
    }

    @Override
    public PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(user, pw);
    }
}
