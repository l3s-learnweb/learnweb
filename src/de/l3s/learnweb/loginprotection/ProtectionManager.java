package de.l3s.learnweb.loginprotection;

import java.util.List;

/**
 * Interface for the brute force protection manager class. Maintains separate bans on IP and usernames, records failed login attempts and can remove
 * bans as well as add them.
 *
 * @author Kate
 *
 */
public interface ProtectionManager
{
    public BanData getIPData(String IP);

    public BanData getUsernameData(String username);

    public List<BanData> getBanlist();

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
     * @param accData AccessData of the username\IP that will be banned
     * @param bantime Duration of the ban (in hours). Negative bantime values equals permaban.
     * @param isIP Whether the given name is an IP (true) or username (false)
     */
    void ban(BanData accData, int bantime, boolean isIP);

    /**
     * Bans selected IP or username by name for a given amount of time (or unlimited time) and updates the relevant database table.
     *
     * @param name IP that will be banned
     * @param bantime Duration of the ban (in hours). Negative bantime values equals permaban.
     * @param isIP Whether the given name is an IP (true) or username (false)
     */
    void ban(String name, int bantime, boolean isIP);

    /**
     * Deletes all bans that have already expired at least a few days ago. Specific date is dependant on implementation.
     */
    public void cleanUpOutdatedBans();
}
