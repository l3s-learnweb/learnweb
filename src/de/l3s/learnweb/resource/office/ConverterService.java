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
import org.apache.log4j.Logger;

import com.google.gson.Gson;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.File;
import de.l3s.learnweb.resource.office.converter.model.ConverterRequest;
import de.l3s.learnweb.resource.office.converter.model.ConverterResponse;
import de.l3s.learnweb.resource.office.converter.model.OfficeThumbnailParams;

public class ConverterService
{

    private final static Logger log = Logger.getLogger(ConverterService.class);
    private final Learnweb learnweb;

    public ConverterRequest createThumbnailConverterRequest(File file)
    {
        String fileExt = file.getName().substring(file.getName().lastIndexOf("."));
        String key = FileUtility.generateRevisionId(file);
        return new ConverterRequest(fileExt, "png", file.getName(), file.getName(), key, new OfficeThumbnailParams());
    }

    public ConverterService(Learnweb learnweb)
    {
        this.learnweb = learnweb;
    }

    private ConverterResponse sendRequestToConvertServer(ConverterRequest model)
    {
        Gson gson = new Gson();
        String stringResponse = null;
        ConverterResponse converterResponse = new ConverterResponse();
        try(CloseableHttpClient client = HttpClients.createDefault();)
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

    public InputStream convert(ConverterRequest request) throws IOException
    {
        try
        {
            String newFileUrl = getConvertedUri(request);
            URL url = new URL(newFileUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            InputStream stream = connection.getInputStream();
            if(stream == null)
            {
                throw new Exception("Error during conversion : stream is null");
            }
            return stream;
        }
        catch(Exception ex)
        {
            log.error("Error during conversion; URL: " + request.getUrl() + "; " + ex);
        }
        return null;
    }

    public String getConvertedUri(ConverterRequest request) throws Exception
    {
        ConverterResponse response = sendRequestToConvertServer(request);

        if(response == null)
            throw new Exception("Invalid answer format");

        if(response.getError() != null)
            processConvertServiceResponseError(response.getError());

        if(response.isEndConvert() == null || !response.isEndConvert())
            throw new Exception("Conversion is not finished");

        if(response.getPercent() == 0)
            throw new Exception("Percent is null");

        if(response.getFileUrl() == null || response.getFileUrl().length() == 0)
            throw new Exception("FileUrl is null");

        return response.getFileUrl();
    }

    private void processConvertServiceResponseError(int errorCode) throws Exception
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
        throw new Exception(errorMessage);
    }

}
