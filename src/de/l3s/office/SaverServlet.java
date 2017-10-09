package de.l3s.office;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.sql.SQLException;
import java.util.Date;
import java.util.Scanner;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import de.l3s.learnweb.File;
import de.l3s.learnweb.Learnweb;

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

    private static final String SAMPLE_PPTX = "sample.pptx";

    private static final String SAMPLE_XLSX = "sample.xlsx";

    private static final String SAMPLE_DOCX = "sample.docx";

    private final static Logger logger = Logger.getLogger(SaverServlet.class);

    private static final String FILE_NAME = "fileName";

    private static final String FILE_TYPE = "fileType";

    private static final String ERROR_0 = "{\"error\":0}";

    private static final String URL = "url";

    private static final String STATUS = "status";

    private static final String DELIMITER = "\\A";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException
    {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException
    {
        try(PrintWriter writer = response.getWriter())
        {
            String fileId = request.getParameter("fileId");

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
                URL url = new URL(downloadUri);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                InputStream inputStream = connection.getInputStream();
                learnweb.getFileManager().save(file, inputStream);
                logger.info(downloadUri);
                inputStream.close();
                connection.disconnect();
            }
        }
        catch(ParseException e)
        {
            logger.error(e);
        }
        catch(NumberFormatException e)
        {
            logger.error(e);
        }
        catch(SQLException e)
        {
            logger.error(e);
            ;
        }
        catch(IOException e)
        {
            logger.error(e);
        }
    }

    private void readFromInputStream(HttpServletResponse response, InputStream is) throws IOException
    {
        try(ServletOutputStream out = response.getOutputStream())
        {
            int read;
            final byte[] bytes = new byte[1024];
            while((read = is.read(bytes)) != -1)
            {
                out.write(bytes, 0, read);
            }
            out.flush();
        }
    }

}
