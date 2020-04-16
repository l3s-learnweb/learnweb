package de.l3s.learnweb.resource.office;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.File;
import de.l3s.learnweb.resource.office.converter.model.ConverterRequest;
import de.l3s.learnweb.resource.office.converter.model.ConverterResponse;
import de.l3s.learnweb.resource.office.converter.model.OfficeThumbnailParams;

public class ConverterService
{

    private static final Logger log = LogManager.getLogger(ConverterService.class);
    private final Learnweb learnweb;

    public ConverterRequest createThumbnailConverterRequest(File file)
    {
        String fileExt = file.getName().substring(file.getName().lastIndexOf('.'));
        String key = FileUtility.generateRevisionId(file);
        return new ConverterRequest(fileExt, "png", file.getName(), file.getUrl(), key, new OfficeThumbnailParams());
    }

    public ConverterService(Learnweb learnweb)
    {
        this.learnweb = learnweb;
    }

    private ConverterResponse sendRequestToConvertServer(ConverterRequest model)
    {
        Gson gson = new Gson();
        String stringResponse;
        ConverterResponse converterResponse = new ConverterResponse();
        try(CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost httpPost = new HttpPost(learnweb.getProperties().getProperty("FILES.DOCSERVICE.URL.CONVERTER"));
            String json = gson.toJson(model);
            StringEntity entity = new StringEntity(json);
            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            CloseableHttpResponse response = client.execute(httpPost);
            if(response.getEntity() != null)
            {
                stringResponse = EntityUtils.toString(response.getEntity());
                converterResponse = gson.fromJson(stringResponse, ConverterResponse.class);
            }
        }
        catch(IOException e)
        {
            log.error(e);
        }
        return converterResponse;
    }

    public InputStream convert(ConverterRequest request) throws ConverterException, IOException
    {/* test: handle exception in resource preview maker
     
     try
     {*/
        String newFileUrl = getConvertedUri(request);
        URL url = new URL(newFileUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        InputStream stream = connection.getInputStream();
        if(stream == null)
        {
            throw new ConverterException("Error during conversion : stream is null");
        }
        return stream;
        /*
        }
        catch(Exception ex)
        {
        log.error("Error during conversion; URL: " + request.getUrl() + "; " , ex);
        }
        return null;*/
    }

    public String getConvertedUri(ConverterRequest request) throws ConverterException
    {
        ConverterResponse response = sendRequestToConvertServer(request);

        if(response == null)
            throw new ConverterException("Invalid answer format");

        if(response.getError() != null)
            processConvertServiceResponseError(response.getError());

        if(response.isEndConvert() == null || !response.isEndConvert())
            throw new ConverterException("Conversion is not finished");

        if(response.getPercent() == 0)
            throw new ConverterException("Percent is null");

        if(response.getFileUrl() == null || response.getFileUrl().isEmpty())
            throw new ConverterException("FileUrl is null");

        return response.getFileUrl();
    }

    private void processConvertServiceResponseError(int errorCode) throws ConverterException
    {
        String errorMessage = "";
        String errorMessageTemplate = "Error occurred in the ConverterService: ";

        switch(errorCode)
        {
        case -8:
            errorMessage = errorMessageTemplate + "Error document VKey";
            break;
        case -7:
            errorMessage = errorMessageTemplate + "Error document request";
            break;
        case -6:
            errorMessage = errorMessageTemplate + "Error database";
            break;
        case -5:
            errorMessage = errorMessageTemplate + "Error unexpected guid";
            break;
        case -4:
            errorMessage = errorMessageTemplate + "Error download error";
            break;
        case -3:
            errorMessage = errorMessageTemplate + "Error convertation error";
            break;
        case -2:
            errorMessage = errorMessageTemplate + "Error convertation timeout";
            break;
        case -1:
            errorMessage = errorMessageTemplate + "Error convertation unknown";
            break;
        case 0:
            break;
        default:
            errorMessage = "ErrorCode = " + errorCode;
            break;
        }
        throw new ConverterException(errorMessage);
    }

    public static class ConverterException extends Exception
    {
        public ConverterException(String errorMessage)
        {
            super(errorMessage);
        }

        private static final long serialVersionUID = 8151643724813680762L;
    }
}
