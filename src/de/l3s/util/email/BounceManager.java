package de.l3s.util.email;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
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
 */
public class BounceManager
{
    private final static Logger log = Logger.getLogger(BounceManager.class);
    private final Learnweb learnweb;

    private static final String login = "learnweb";
    private static final String pass = "5-FN!@QENtrXh6V][C}*h8-S=yju";
    private static final String host = "imap.kbs.uni-hannover.de";
    private static final String provider = "imap";

    private static Authenticator authenticator = new PasswordAuthenticator(login, pass);

    private static Pattern statusCodePattern = Pattern.compile("(?<=Status: )\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
    private static Pattern originalRecipientPattern = Pattern.compile("(?<=Original-Recipient:)(\\s.+;)(.+)\\s");

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
        Session session = Session.getInstance(props, authenticator);

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
     * Reads the message contents into a byte stream and returns it as string.
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
        String description;

        if(matcherCode.find())
        {
            code = matcherCode.group();
            description = getErrorDescription(code);
        }
        else
        {
            code = "Not found";
            description = "";
        }

        String originalRecipient = null;
        Matcher matcherRecipient = originalRecipientPattern.matcher(text);

        if(matcherRecipient.find())
        {
            originalRecipient = matcherRecipient.group(2);
        }

        //Adds message to database
        addToDB(originalRecipient, msg.getReceivedDate(), code, description);
        checkAndNotify(msg);
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

        //Transient or permanent
        if(codes[0].equals("4"))
        {
            description = "Transient Persistent Failure: ";
        }
        else if(codes[0].equals("5"))
        {
            description = "Permanent Failure: ";
        }

        //Actual code explanation. VERY LONG
        switch(codes[1])
        {
        case "1.0":
            description += "Other address status";
            break;
        case "1.1":
            description += "Bad destination mailbox address";
            break;
        case "1.2":
            description += "Bad destination system address";
            break;
        case "1.3":
            description += "Bad destination mailbox address syntax";
            break;
        case "1.4":
            description += "Destination mailbox address ambiguous";
            break;
        case "1.5":
            description += "Destination mailbox address valid";
            break;
        case "1.6":
            description += "Mailbox has moved";
            break;
        case "1.7":
            description += "Bad sender's mailbox address syntax";
            break;
        case "1.8":
            description += "Bad sender's system address";
            break;
        case "2.0":
            description += "Other or undefined mailbox status";
            break;
        case "2.1":
            description += "Mailbox disabled, not accepting messages";
            break;
        case "2.2":
            description += "Mailbox full";
            break;
        case "2.3":
            description += "Message length exceeds administrative limit";
            break;
        case "2.4":
            description += "Mailing list expansion problem";
            break;
        case "3.0":
            description += "Other or undefined mail system status";
            break;
        case "3.1":
            description += "Mail system full";
            break;
        case "3.2":
            description += "System not accepting network messages";
            break;
        case "3.3":
            description += "System not capable of selected features";
            break;
        case "3.4":
            description += " Message too big for system";
            break;
        case "4.0":
            description += "Other or undefined network or routing status";
            break;
        case "4.1":
            description += "No answer from host";
            break;
        case "4.2":
            description += "Bad connection";
            break;
        case "4.3":
            description += "Routing server failure";
            break;
        case "4.4":
            description += "Unable to route";
            break;
        case "4.5":
            description += "Network congestion";
            break;
        case "4.6":
            description += "Routing loop detected";
            break;
        case "4.7":
            description += "Delivery time expired";
            break;
        case "5.0":
            description += "Other or undefined protocol status";
            break;
        case "5.1":
            description += "Invalid command";
            break;
        case "5.2":
            description += "Syntax error";
            break;
        case "5.3":
            description += "Too many recipients";
            break;
        case "5.4":
            description += "Invalid command arguments";
            break;
        case "5.5":
            description += "Wrong protocol version";
            break;
        case "6.0":
            description += "Other or undefined media error";
            break;
        case "6.1":
            description += "Media not supported";
            break;
        case "6.2":
            description += "Conversion required and prohibited";
            break;
        case "6.3":
            description += "Conversion required but not supported";
            break;
        case "6.4":
            description += "Conversion with loss performed";
            break;
        case "6.5":
            description += "Conversion failed";
            break;
        default:
            description += "Unspecified mailing error";
            break;
        }

        return description;
    }

    private void addToDB(String originalRecipient, Date date, String code, String description)
    {
        final String query = "INSERT INTO lw_bounces (address, timereceived, code, description) VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE timereceived = VALUES(timereceived), code = VALUES(code), description = VALUES(description)";
        try(PreparedStatement insert = learnweb.getConnection().prepareStatement(query))
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
                Date ts = rs.getTimestamp(1);
                if(ts != null)
                {
                    return ts;
                }
            }

        }
        catch(SQLException e)
        {
            log.error("Failed to get last bounce address. Setting current date as it. SQLException: ", e);
        }

        return new Date(new Date().getTime() - 1000 * 60 * 60 * 24 * 365); // one year before
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
    @SuppressWarnings("unused")
    private void checkBounceFolder() throws MessagingException
    {
        Properties props = new Properties();
        Session session = Session.getInstance(props, authenticator);
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

    /**
     * Checks whether a certain bounced message fits some filter and then notifies the observer.
     */
    private void checkAndNotify(Message msg)
    {
        //        //Insert your conditions here
        //        if(true)
        //        {
        //            notifyObservers(msg);
        //        }
    }

}
