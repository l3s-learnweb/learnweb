package de.l3s.util.email;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Authenticator;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.search.ComparisonTerm;
import javax.mail.search.ReceivedDateTerm;
import javax.mail.search.SearchTerm;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;

/**
 * Parses the mailbox every sometime and detects whether or not bounces are present. Bounced emails are
 *
 * @author Kate
 *
 */
public class BounceManager
{
    private final static Logger log = Logger.getLogger(BounceManager.class);
    private final Learnweb learnweb;

    private static String login = "learnweb";
    private static String pass = "5-FN!@QENtrXh6V][C}*h8-S=yju";
    private static String host = "imap.kbs.uni-hannover.de";
    private static String provider = "imap";

    private static Authenticator authenticator = new PasswordAuthenticator(login, pass);

    private static Pattern statusCodePattern = Pattern.compile("(?<=Status: )\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
    private static Pattern originalRecipientPattern = Pattern.compile("(?<=Original-Recipient:)(\\s.{1,}\\;)(.{1,})\\s");

    private Date lastBounceCheck = null;

    public BounceManager(Learnweb lw)
    {
        learnweb = lw;
    }

    public void parseInbox() throws MessagingException, IOException
    {
        Date currentCheck = new Date();

        if(lastBounceCheck == null)
        {
            lastBounceCheck = getLastBounceDate();
        }

        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, authenticator);

        SearchTerm newerThan = new ReceivedDateTerm(ComparisonTerm.GT, lastBounceCheck);

        Store store = session.getStore(provider);
        store.connect(host, login, pass);

        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_WRITE);

        Message[] messages = inbox.search(newerThan);
        List<Message> bounces = new ArrayList<>();

        for(Message msg : messages)
        {
            if(parseMessage(msg))
            {
                bounces.add(msg);
            }
        }

        moveBouncesToFolder(bounces.toArray(new Message[0]), inbox);

        for(Message msg : bounces)
        {
            msg.setFlag(Flags.Flag.DELETED, true);
        }

        inbox.close(true);
        store.close();

        lastBounceCheck = currentCheck;
    }

    /**
     * Reads the message contents into a bytestream and returns it as string.
     */
    private String getText(Message msg) throws MessagingException, IOException
    {
        try(ByteArrayOutputStream outStream = new ByteArrayOutputStream())
        {
            msg.writeTo(outStream);
            return new String(outStream.toByteArray());
        }

    }

    /**
     * Examines message contents to detect whether it's a bounce or a regular message. If it's a bounce, parses out an error type. and adds it to
     * bounces database If it's not, does nothing.
     *
     * @param msg Message to be examined
     */
    private boolean parseMessage(Message msg) throws MessagingException, IOException
    {
        //Return path is checked first, since in bounce messages those are usually empty or contain just "<>"
        String[] returnPaths = msg.getHeader("Return-Path");

        if(returnPaths != null && returnPaths.length > 0)
        {
            if(returnPaths[0].length() > 3)
            {
                return false;
            }
        }

        log.debug("BOUNCE: " + msg.getSubject() + " " + msg.getReceivedDate());

        //Checks the status code
        String text = getText(msg);
        Matcher matcherCode = statusCodePattern.matcher(text);

        String code;
        String descr;

        if(matcherCode.find())
        {
            code = matcherCode.group();
            descr = getErrorDescription(code);
        }
        else
        {
            code = "Not found";
            descr = "";
        }

        String originalRecipient;
        Matcher matcherRecipient = originalRecipientPattern.matcher(text);

        if(matcherRecipient.find())
        {
            originalRecipient = matcherRecipient.group(2);
        }
        else
        {
            originalRecipient = "Unknown";
        }

        //Adds message to database
        addToDB(originalRecipient, msg.getReceivedDate(), code, descr);
        return true;
    }

    /**
     * Gets the type of error according to RFC3463. If no code can be found, returns "Unspecified mailing error".
     * For error list see https://www.ietf.org/rfc/rfc3463.txt
     * Some mailing systems use custom codes; getting descriptions of those will be implemented later, if needed
     */
    private String getErrorDescription(String errCode)
    {
        String description = "";

        String[] codes = errCode.split("\\.", 2);

        //Transient or permament
        if(codes[0].equals("4"))
        {
            description = "Transient Persistent Failure: ";
        }
        else if(codes[0].equals("5"))
        {
            description = "Permament Failure: ";
        }

        //Actual code explanation. VERY LONG
        if(codes[1].equals("1.0"))
        {
            description += "Other address status";
        }
        else if(codes[1].equals("1.1"))
        {
            description += "Bad destination mailbox address";
        }
        else if(codes[1].equals("1.2"))
        {
            description += " Bad destination system address";
        }
        else if(codes[1].equals("1.3"))
        {
            description += "Bad destination mailbox address syntax";
        }
        else if(codes[1].equals("1.4"))
        {
            description += "Destination mailbox address ambiguous";
        }
        else if(codes[1].equals("1.5"))
        {
            description += "Destination mailbox address valid";
        }
        else if(codes[1].equals("1.6"))
        {
            description += "Mailbox has moved";
        }
        else if(codes[1].equals("1.7"))
        {
            description += "Bad sender's mailbox address syntax";
        }
        else if(codes[1].equals("1.8"))
        {
            description += "Bad sender's system address";
        }
        else if(codes[1].equals("2.0"))
        {
            description += "Other or undefined mailbox status";
        }
        else if(codes[1].equals("2.1"))
        {
            description += "Mailbox disabled, not accepting messages";
        }
        else if(codes[1].equals("2.2"))
        {
            description += "Mailbox full";
        }
        else if(codes[1].equals("2.3"))
        {
            description += "Message length exceeds administrative limit";
        }
        else if(codes[1].equals("2.4"))
        {
            description += "Mailing list expansion problem";
        }
        else if(codes[1].equals("3.0"))
        {
            description += "Other or undefined mail system status";
        }
        else if(codes[1].equals("3.1"))
        {
            description += "Mail system full";
        }
        else if(codes[1].equals("3.2"))
        {
            description += "System not accepting network messages";
        }
        else if(codes[1].equals("3.3"))
        {
            description += "System not capable of selected features";
        }
        else if(codes[1].equals("3.4"))
        {
            description += " Message too big for system";
        }
        else if(codes[1].equals("4.0"))
        {
            description += "Other or undefined network or routing status";
        }
        else if(codes[1].equals("4.1"))
        {
            description += "No answer from host";
        }
        else if(codes[1].equals("4.2"))
        {
            description += "Bad connection";
        }
        else if(codes[1].equals("4.3"))
        {
            description += "Routing server failure";
        }
        else if(codes[1].equals("4.4"))
        {
            description += "Unable to route";
        }
        else if(codes[1].equals("4.5"))
        {
            description += "Network congestion";
        }
        else if(codes[1].equals("4.6"))
        {
            description += "Routing loop detected";
        }
        else if(codes[1].equals("4.7"))
        {
            description += "Delivery time expired";
        }
        else if(codes[1].equals("5.0"))
        {
            description += "Other or undefined protocol status";
        }
        else if(codes[1].equals("5.1"))
        {
            description += "Invalid command";
        }
        else if(codes[1].equals("5.2"))
        {
            description += "Syntax error";
        }
        else if(codes[1].equals("5.3"))
        {
            description += "Too many recipients";
        }
        else if(codes[1].equals("5.4"))
        {
            description += "Invalid command arguments";
        }
        else if(codes[1].equals("5.5"))
        {
            description += "Wrong protocol version";
        }
        else if(codes[1].equals("6.0"))
        {
            description += "Other or undefined media error";
        }
        else if(codes[1].equals("6.1"))
        {
            description += "Media not supported";
        }
        else if(codes[1].equals("6.2"))
        {
            description += "Conversion required and prohibited";
        }
        else if(codes[1].equals("6.3"))
        {
            description += "Conversion required but not supported";
        }
        else if(codes[1].equals("6.4"))
        {
            description += "Conversion with loss performed";
        }
        else if(codes[1].equals("6.5"))
        {
            description += "Conversion failed";
        }
        else
        {
            description += "Unspecified mailing error";
        }

        return description;
    }

    private void addToDB(String originalRecipient, Date date, String code, String description)
    {
        try(PreparedStatement insert = learnweb.getConnection().prepareStatement("INSERT INTO lw_bounces (address, timereceived, code, description) VALUES(?, ?, ?, ?)"))
        {
            insert.setString(1, originalRecipient);
            insert.setTimestamp(2, new java.sql.Timestamp(date.getTime()));
            insert.setString(3, code);
            insert.setString(4, description);

            insert.execute();

        }
        catch(SQLException e)
        {
            log.error("Attempt to log bounce failed. SQL Error: ", e);
        }
    }

    /**
     * Queries the database to get the date of last recorded bounce. Future scans will only fetch letters received after this date.
     *
     * @return Date of last recorded bounce
     */
    private Date getLastBounceDate()
    {
        try(PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT MAX(timereceived) FROM lw_bounces"))
        {
            ResultSet rs = select.executeQuery();

            if(rs.next())
            {
                return rs.getTimestamp(1);
            }

        }
        catch(SQLException e)
        {
            log.error("Failed to get last bounce address. Setting current date as it. SQLException: ", e);
        }

        return new Date(0L);

    }

    /**
     * Moves selected messages from inbox to bounces folder
     */
    private void moveBouncesToFolder(Message[] messages, Folder inbox) throws MessagingException
    {
        Folder bounceFolder = inbox.getFolder("BOUNCES");
        if(!bounceFolder.exists())
        {
            bounceFolder = inbox.getFolder("BOUNCES");
            bounceFolder.create(Folder.HOLDS_MESSAGES);
            log.debug("Bounce folder created.");
        }

        bounceFolder.open(Folder.READ_WRITE);
        inbox.copyMessages(messages, bounceFolder);

        bounceFolder.close(false);
    }

    /**
     * Debug/analysis function that checks contents of bounce folder.
     */
    private void checkBounceFolder() throws MessagingException
    {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, authenticator);
        Store store = session.getStore(provider);
        store.connect(host, login, pass);

        Folder bounceFolder = store.getFolder("INBOX").getFolder("BOUNCES");
        if(!bounceFolder.exists())
        {
            log.debug("Folder doesn't exist.");

        }
        else
        {
            bounceFolder.open(Folder.READ_ONLY);
            Message[] bounces = bounceFolder.getMessages();

            if(bounces.length > 0)
            {
                log.debug("Bounced emails folder contains " + bounceFolder.getMessageCount() + " messages. Oldest and newest messages printed below:");
                log.debug(bounces[0].getSubject() + " " + bounces[0].getReceivedDate());
                log.debug(bounces[bounces.length - 1].getSubject() + " " + bounces[bounces.length - 1].getReceivedDate());
            }
            else
            {
                log.debug("Bounced emails folder is empty.");
            }

            bounceFolder.close(false);
        }

        store.close();
    }

}
