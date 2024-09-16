package de.l3s.mail;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;

public class Mail implements Serializable {
    @Serial
    private static final long serialVersionUID = -5917197634340620811L;

    String subject;
    String text;
    String textHtml;

    ArrayList<String> recipients = new ArrayList<>();
    ArrayList<String> recipientsCc = new ArrayList<>();
    ArrayList<String> recipientsBcc = new ArrayList<>();
    String replyTo;

    public Mail() {
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setHTML(String text) {
        this.textHtml = text;
    }

    public void addRecipient(String address) {
        this.recipients.add(address);
    }

    public void addRecipientBc(String address) {
        this.recipientsCc.add(address);
    }

    public void addRecipientBcc(String address) {
        this.recipientsBcc.add(address);
    }

    public void setRecipientsBcc(ArrayList<String> addresses) {
        this.recipientsBcc = addresses;
    }

    public void setReplyTo(String address) {
        replyTo = address;
    }
}
