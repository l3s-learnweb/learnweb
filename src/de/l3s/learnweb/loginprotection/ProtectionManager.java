package de.l3s.learnweb.loginprotection;

/**
 * Interface for the brute force protection manager class. Maintains separate bans on IP and usernames, records failed login attempts and can remove
 * bans as well as add them.
 *
 * @author Kate
 *
 */
public interface ProtectionManager
{
    public AccessData getIPData(String IP);

    public AccessData getUsernameData(String username);

    /**
     * Records a failed attempt to log in. Depending on the specific implementation, also updates bantimes.
     */
    public void updateFailedAttempts(String IP, String username);

    /**
     * Unbans a given name or IP.
     *
     * @param name Name\Address that will be cleared of their sins
     */
    public void unban(String name);

    /**
     * Clears all and every ban. Use for debugging purposes only.
     */
    public void clearBans();

    /**
     * Bans selected IP or username for a given amount of time (or unlimited time) and updates the relevant database table.
     *
     * @param name IP that will be banned
     * @param bantime Duration of the ban (in hours). Negative bantime values equals permaban.
     * @param isIP Whether the given name is an IP (true) or username (false)
     * @param permaban Whether the ban is temporary or permament
     */
    void ban(AccessData accData, String name, int bantime, boolean isIP);
}
