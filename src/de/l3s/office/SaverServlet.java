package de.l3s.office;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.Date;
import java.util.Scanner;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import de.l3s.learnweb.File;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.Resource;

/**
 * Servlet Class
 * 
 * @web.servlet name="saverServlet" display-name="Simple SaverServlet"
 *              description="Servlet for saving edited documents"
 * @web.servlet-mapping url-pattern="/save"
 */

public class SaverServlet extends HttpServlet
{
    private static final long serialVersionUID = 7296371511069054378L;

    private static final String FILE_ID = "fileId";

    private final static Logger logger = Logger.getLogger(SaverServlet.class);

    private static final String ERROR_0 = "{\"error\":0}";

    private static final String URL = "url";

    private static final String STATUS = "status";

    private static final String DELIMITER = "\\A";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException
    {
        try(PrintWriter writer = response.getWriter())
        {
            String fileId = request.getParameter(FILE_ID);
            try(Scanner scanner = new Scanner(request.getInputStream()))
            {
                scanner.useDelimiter(DELIMITER);
                String body = scanner.hasNext() ? scanner.next() : "";
                parseResponse(body, fileId);
            }
            request.getRemoteAddr();
            writer.write(ERROR_0);
        }
        catch(IOException e)
        {
            logger.error(e);
        }

    }

    private void parseResponse(String body, String fileId)
    {
        try
        {
            JSONObject jsonObj = (JSONObject) new JSONParser().parse(body);
            logger.info((long) jsonObj.get(STATUS));
            if((long) jsonObj.get(STATUS) == DocumentStatus.READY_FOR_SAVING.getStatus())
            {
                String downloadUri = (String) jsonObj.get(URL);
                Learnweb learnweb = Learnweb.getInstance();
                File file = learnweb.getFileManager().getFileById(Integer.parseInt(fileId));
                file.setLastModified(new Date());
                Resource resource = learnweb.getResourceManager().getResource(file.getResourceId());
                URL url = new URL(downloadUri);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = connection.getInputStream();
                learnweb.getFileManager().save(file, inputStream);
                learnweb.getResourcePreviewMaker().processResource(resource);
                logger.debug("Saved document url: " + downloadUri);
                inputStream.close();
                connection.disconnect();
            }
        }
        catch(ParseException | NumberFormatException e)
        {
            logger.error(e);
        }
        catch(SQLException | IOException e)
        {
            logger.error(e);
        }

    }

}
