package de.l3s.learnweb.loginprotection.entity;

/**
 * Holds response from ProtectionManager regarding an IP-username pair. Returns a quick-encoded status and accompanying message.
 * <br>
 * Status meanings: k - all okay; c - requires captcha; w - warning, whether about imminent ban or suspicion level; b - bantime
 *
 * @author Kate
 *
 */
public class ManagerResponse
{
    private char status;
    private String message;

    public ManagerResponse(char s, String m)
    {
        status = s;
        message = m;
    }

    public char getStatus()
    {
        return status;
    }

    public String getMessage()
    {
        return message;
    }

}
