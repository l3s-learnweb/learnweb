package de.l3s.learnweb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

import de.l3s.util.Misc;
import de.l3s.util.StringHelper;

public class CDXClient
{
    private static final Logger log = Logger.getLogger(CDXClient.class);

    private final SimpleDateFormat waybackDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

    public CDXClient()
    {
    }

    public Date getFirstCaptureDate(String url) throws ParseException, IOException
    {
        return getCaptureDate(url, 1);
    }

    public Date getLastCaptureDate(String url) throws ParseException, IOException
    {
        return getCaptureDate(url, -1);
    }

    private Date getCaptureDate(String url, int limit) throws ParseException, IOException
    {
        for(int retry = 2; retry >= 0; retry--) // retry 2 times if we get retrieve "Unexpected end of file from server"
        {
            try
            {
                URLConnection connection = new java.net.URL("http://web.archive.org/cdx/search/cdx?url=" + StringHelper.urlEncode(url) + "&fl=timestamp&limit=" + limit).openConnection();
                connection.setRequestProperty("User-Agent", "L3S-CDX-Client/1.0 (User=http://alexandria-project.eu/archivesearch/)");

                StringBuilder sb = new StringBuilder();
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                while((inputLine = in.readLine()) != null)
                    sb.append(inputLine);
                in.close();

                String response = sb.toString();
                if(response.trim().length() == 0)
                    return null;

                synchronized(this)
                {
                    return waybackDateFormat.parse(response);
                }
            }
            catch(MalformedURLException e)
            {
                throw new RuntimeException(e);
            }
            catch(ParseException | IOException e)
            {
                String msg = e.getMessage();
                if(msg.contains("HTTP response code: 403")) // blocked by robots
                    return null;
                else if((msg.equals("Unexpected end of file from server") || msg.startsWith("Server returned HTTP response code: 50")) && retry >= 0) // hit request limit
                {
                    Misc.sleep(600);
                    log.debug("To many api requests => Sleep a while");
                }
                else
                    throw e;
            }
        }
        throw new IllegalStateException();
    }

    /**
     * @param args
     * @throws IOException
     * @throws MalformedURLException
     * @throws ParseException
     */
    public static void main(String[] args) throws MalformedURLException, IOException, ParseException
    {

        CDXClient cdxClient = new CDXClient();
        System.out.println(cdxClient.getCaptureDate("www.facebook.com/theultrasoft", 1));

    }

}
