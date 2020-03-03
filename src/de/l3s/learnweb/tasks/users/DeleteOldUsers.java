package de.l3s.learnweb.tasks.users;

import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Period;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.user.UserManager;

/**
 * Deletes users that haven't logged in for a long time
 *
 * @author Kemkes
 *
 */
public class DeleteOldUsers
{
    private static Logger log = Logger.getLogger(DeleteOldUsers.class);

    /**
     * @param args
     * @throws SQLException
     * @throws IOException
     * @throws MalformedURLException
     * @throws ClassNotFoundException
     */
    public static void main(String[] args) throws SQLException, ClassNotFoundException
    {
        log.debug("start");

        Learnweb learnweb = Learnweb.createInstance("https://learnweb.l3s.uni-hannover.de");
        UserManager um = learnweb.getUserManager();

        int configYears = 5; // delete users that didn't login for more than defined number of years
        LocalDate now = LocalDate.now();
        now.minus(Period.ofYears(configYears));

        String query = "SELECT * FROM `lw_user` WHERE YEAR(`registration_date`) < " + 2015;
        log.debug("Query: " + query);
        ResultSet rs = learnweb.getConnection().createStatement().executeQuery(query);

        while(rs.next())
        {
            int userId = rs.getInt(1);
            log.debug(userId + " - " + um.getLastLoginDate(userId));
            /*
            now.isAfter(um.getLastLoginDate(userId).toInstant())

            if(.isBefore(otherInstant))
            + " - " + Duration.between(now, ).toDays());
            */
            // User user = um.getUser(userId);

            //log.debug(user.getEmail());
        }

        log.debug("done");

        learnweb.onDestroy();
    }
}
