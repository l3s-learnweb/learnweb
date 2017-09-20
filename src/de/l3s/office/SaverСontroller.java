package de.l3s.office;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Scanner;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@WebServlet(name = "SaverСontroller", urlPatterns = { "/SaverСontrollerR" })
@MultipartConfig
public class SaverСontroller extends HttpServlet
{

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

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException
    {
        System.out.println("GO");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException
    {
        try(PrintWriter writer = response.getWriter())
        {
            try(Scanner scanner = new Scanner(request.getInputStream()))
            {
                scanner.useDelimiter(DELIMITER);
                String body = scanner.hasNext() ? scanner.next() : "";
                parseResponse(body);
            }
            request.getRemoteAddr();
            writer.write(ERROR_0);
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }

    }

    private void parseResponse(String body)
    {
        try
        {
            JSONObject jsonObj = (JSONObject) new JSONParser().parse(body);
            logger.info((long) jsonObj.get(STATUS));
            if((long) jsonObj.get(STATUS) == DocumentStatus.READY_FOR_SAVING.getStatus())
            {
                String downloadUri = (String) jsonObj.get(URL);
                logger.info(downloadUri);
            }
        }
        catch(ParseException e)
        {
            e.printStackTrace();
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
