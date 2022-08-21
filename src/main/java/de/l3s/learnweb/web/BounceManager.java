package de.l3s.learnweb.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Store;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.ReceivedDateTerm;
import jakarta.mail.search.SearchTerm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.mail.Mail;

/**
 * Parses the mailbox every sometime and detects whether bounces are present. Bounced emails are
 *
 * @author Kate
 */
@Dependent
public class BounceManager {
    private static final Logger log = LogManager.getLogger(BounceManager.class);

    private static final Pattern STATUS_CODE_PATTERN = Pattern.compile("(?<=Status: )\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
    private static final Pattern ORIGINAL_RECIPIENT_PATTERN = Pattern.compile("(?<=Original-Recipient:)(\\s.+;)(.+)\\s");

    private Instant lastBounceCheck;

    @Inject
    private BounceDao bounceDao;

    public Store getStore() throws MessagingException {
        return Mail.createSession().getStore("imap");
    }

    public void parseInbox() throws MessagingException, IOException {
        Instant currentCheck = Instant.now();

        if (lastBounceCheck == null) {
            lastBounceCheck = bounceDao.findLastBounceDate().orElse(currentCheck.minus(365, ChronoUnit.DAYS));
        }

        SearchTerm newerThan = new ReceivedDateTerm(ComparisonTerm.GT, Date.from(lastBounceCheck.atZone(ZoneId.systemDefault()).toInstant()));

        Store store = getStore();
        store.connect();

        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_WRITE);

        Message[] messages = inbox.search(newerThan);
        List<Message> bounces = new ArrayList<>();

        for (Message msg : messages) {
            if (parseMessage(msg)) {
                bounces.add(msg);
            }
        }

        moveBouncesToFolder(bounces.toArray(new Message[0]), inbox);

        for (Message msg : bounces) {
            msg.setFlag(Flags.Flag.DELETED, true);
        }

        inbox.close(true);
        store.close();

        lastBounceCheck = currentCheck;
    }

    /**
     * Reads the message contents into a byte stream and returns it as string.
     */
    private String getText(Message msg) throws MessagingException, IOException {
        try (ByteArrayOutputStream outStream = new ByteArrayOutputStream()) {
            msg.writeTo(outStream);
            return outStream.toString(StandardCharsets.UTF_8);
        }
    }

    /**
     * Examines message contents to detect whether it's a bounce or a regular message. If it's a bounce, parses out an error type. and adds it to
     * bounces database If it's not, does nothing.
     *
     * @param msg Message to be examined
     */
    private boolean parseMessage(Message msg) throws MessagingException, IOException {
        //Return path is checked first, since in bounce messages those are usually empty or contain just "<>"
        String[] returnPaths = msg.getHeader("Return-Path");

        if (returnPaths != null && returnPaths.length > 0) {
            if (returnPaths[0].length() > 3) {
                return false;
            }
        }

        log.debug("BOUNCE: {} {}", msg.getSubject(), msg.getReceivedDate());

        //Checks the status code
        String text = getText(msg);
        Matcher matcherCode = STATUS_CODE_PATTERN.matcher(text);

        String code;
        String description;

        if (matcherCode.find()) {
            code = matcherCode.group();
            description = getErrorDescription(code);
        } else {
            code = "Not found";
            description = "";
        }

        String originalRecipient = null;
        Matcher matcherRecipient = ORIGINAL_RECIPIENT_PATTERN.matcher(text);

        if (matcherRecipient.find()) {
            originalRecipient = matcherRecipient.group(2);
        }

        //Adds message to database
        bounceDao.save(originalRecipient, msg.getReceivedDate().toInstant(), code, description);
        return true;
    }

    /**
     * Gets the type of error according to RFC3463. If no code can be found, returns "Unspecified mailing error".
     * For error list see https://www.ietf.org/rfc/rfc3463.txt
     * Some mailing systems use custom codes; getting descriptions of those will be implemented later, if needed
     */
    private String getErrorDescription(String errCode) {
        String description = "";

        String[] codes = errCode.split("\\.", 2);

        //Transient or permanent
        if ("4".equals(codes[0])) {
            description = "Transient Persistent Failure: ";
        } else if ("5".equals(codes[0])) {
            description = "Permanent Failure: ";
        }

        // Actual code explanation. VERY LONG
        description += switch (codes[1]) {
            case "1.0" -> "Other address status";
            case "1.1" -> "Bad destination mailbox address";
            case "1.2" -> "Bad destination system address";
            case "1.3" -> "Bad destination mailbox address syntax";
            case "1.4" -> "Destination mailbox address ambiguous";
            case "1.5" -> "Destination mailbox address valid";
            case "1.6" -> "Mailbox has moved";
            case "1.7" -> "Bad sender's mailbox address syntax";
            case "1.8" -> "Bad sender's system address";
            case "2.0" -> "Other or undefined mailbox status";
            case "2.1" -> "Mailbox disabled, not accepting messages";
            case "2.2" -> "Mailbox full";
            case "2.3" -> "Message length exceeds administrative limit";
            case "2.4" -> "Mailing list expansion problem";
            case "3.0" -> "Other or undefined mail system status";
            case "3.1" -> "Mail system full";
            case "3.2" -> "System not accepting network messages";
            case "3.3" -> "System not capable of selected features";
            case "3.4" -> " Message too big for system";
            case "4.0" -> "Other or undefined network or routing status";
            case "4.1" -> "No answer from host";
            case "4.2" -> "Bad connection";
            case "4.3" -> "Routing server failure";
            case "4.4" -> "Unable to route";
            case "4.5" -> "Network congestion";
            case "4.6" -> "Routing loop detected";
            case "4.7" -> "Delivery time expired";
            case "5.0" -> "Other or undefined protocol status";
            case "5.1" -> "Invalid command";
            case "5.2" -> "Syntax error";
            case "5.3" -> "Too many recipients";
            case "5.4" -> "Invalid command arguments";
            case "5.5" -> "Wrong protocol version";
            case "6.0" -> "Other or undefined media error";
            case "6.1" -> "Media not supported";
            case "6.2" -> "Conversion required and prohibited";
            case "6.3" -> "Conversion required but not supported";
            case "6.4" -> "Conversion with loss performed";
            case "6.5" -> "Conversion failed";
            default -> "Unspecified mailing error";
        };

        return description;
    }

    /**
     * Moves selected messages from inbox to bounces folder.
     */
    private void moveBouncesToFolder(Message[] messages, Folder inbox) throws MessagingException {
        Folder bounceFolder = inbox.getFolder("BOUNCES");
        if (!bounceFolder.exists()) {
            bounceFolder = inbox.getFolder("BOUNCES");
            bounceFolder.create(Folder.HOLDS_MESSAGES);
            log.debug("Bounce folder created.");
        }

        bounceFolder.open(Folder.READ_WRITE);
        inbox.copyMessages(messages, bounceFolder);

        bounceFolder.close(false);
    }
}
