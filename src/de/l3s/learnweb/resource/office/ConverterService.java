package de.l3s.learnweb.resource.office;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.File;
import de.l3s.learnweb.resource.office.converter.model.ConverterRequest;
import de.l3s.learnweb.resource.office.converter.model.ConverterResponse;
import de.l3s.learnweb.resource.office.converter.model.OfficeThumbnailParams;

public class ConverterService {
    private static final Logger log = LogManager.getLogger(ConverterService.class);

    private final Learnweb learnweb;

    public ConverterService(Learnweb learnweb) {
        this.learnweb = learnweb;
    }

    public ConverterRequest createThumbnailConverterRequest(File file) {
        String fileExt = file.getName().substring(file.getName().lastIndexOf('.'));
        String key = FileUtility.generateRevisionId(file);
        return new ConverterRequest(fileExt, "png", file.getName(), file.getUrl(), key, new OfficeThumbnailParams());
    }

    private ConverterResponse sendRequestToConvertServer(ConverterRequest model) {
        Gson gson = new Gson();
        String stringResponse;
        ConverterResponse converterResponse = new ConverterResponse();

        try (CloseableHttpClient client = createUnsafeSSLClient()) { //HttpClients.createDefault())
            HttpPost httpPost = new HttpPost(learnweb.getProperties().getProperty("FILES.DOCSERVICE.URL.CONVERTER"));
            String json = gson.toJson(model);
            StringEntity entity = new StringEntity(json);
            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            CloseableHttpResponse response = client.execute(httpPost);
            if (response.getEntity() != null) {
                stringResponse = EntityUtils.toString(response.getEntity());
                converterResponse = gson.fromJson(stringResponse, ConverterResponse.class);
            }
        } catch (IOException e) {
            log.catching(e);
        }
        return converterResponse;
    }

    public InputStream convert(ConverterRequest request) throws ConverterException, IOException {
        String newFileUrl = getConvertedUri(request);
        URL url = new URL(newFileUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        InputStream stream = connection.getInputStream();
        if (stream == null) {
            throw new ConverterException("Error during conversion : stream is null");
        }
        return stream;
    }

    public String getConvertedUri(ConverterRequest request) throws ConverterException {
        ConverterResponse response = sendRequestToConvertServer(request);

        if (response == null) {
            throw new ConverterException("Invalid answer format");
        }

        if (response.getError() != null) {
            processConvertServiceResponseError(response.getError());
        }

        if (response.isEndConvert() == null || !response.isEndConvert()) {
            throw new ConverterException("Conversion is not finished");
        }

        if (response.getPercent() == 0) {
            throw new ConverterException("Percent is null");
        }

        if (response.getFileUrl() == null || response.getFileUrl().isEmpty()) {
            throw new ConverterException("FileUrl is null");
        }

        return response.getFileUrl();
    }

    private void processConvertServiceResponseError(int errorCode) throws ConverterException {
        String errorMessage = "";
        String errorMessageTemplate = "Error occurred in the ConverterService: ";

        switch (errorCode) {
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

    /**
     * Creates an HTTP client that ignores most SSL problems.
     */
    private static CloseableHttpClient createUnsafeSSLClient() {
        org.apache.http.ssl.SSLContextBuilder sslContextBuilder = SSLContextBuilder.create();
        try {
            sslContextBuilder.loadTrustMaterial(new org.apache.http.conn.ssl.TrustSelfSignedStrategy());

            SSLContext sslContext = sslContextBuilder.build();
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, new DefaultHostnameVerifier());

            HttpClientBuilder httpClientBuilder = HttpClients.custom().setSSLSocketFactory(sslSocketFactory);
            return httpClientBuilder.build();
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }

    public static class ConverterException extends Exception {
        private static final long serialVersionUID = 8151643724813680762L;

        public ConverterException(String errorMessage) {
            super(errorMessage);
        }
    }
}
