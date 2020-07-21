package de.l3s.learnweb.resource.office;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.File;
import de.l3s.learnweb.resource.office.converter.model.ConverterRequest;
import de.l3s.learnweb.resource.office.converter.model.ConverterResponse;

public final class ConverterService {
    private static final Logger log = LogManager.getLogger(ConverterService.class);

    private static final TrustManager[] trustAllCerts = {
        new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
            }
        }
    };

    public static String convert(final Learnweb learnweb, final File file) {
        return convert(learnweb, createConverterRequest(file));
    }

    public static String convert(final Learnweb learnweb, final ConverterRequest converterRequest) {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            Gson gson = new Gson();
            // previously we used unsafe ssl client, but now it seems that certificate is valid and all right
            HttpClient client = HttpClient.newBuilder().sslContext(sslContext).build();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(learnweb.getProperties().getProperty("FILES.DOCSERVICE.URL.CONVERTER")))
                .header("Content-type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(converterRequest)))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            ConverterResponse converterResponse = gson.fromJson(response.body(), ConverterResponse.class);
            return getConvertedUrl(converterResponse);
        } catch (IOException | InterruptedException | NoSuchAlgorithmException | KeyManagementException e) {
            log.error("Can't process request: {}", converterRequest, e);
            return null;
        }
    }

    private static ConverterRequest createConverterRequest(final File file) {
        String fileExt = file.getName().substring(file.getName().lastIndexOf('.'));
        String key = FileUtility.generateRevisionId(file);
        return new ConverterRequest(fileExt, "png", file.getName(), file.getUrl(), key);
    }

    private static String getConvertedUrl(final ConverterResponse response) {
        if (response == null) {
            throw new IllegalStateException("Invalid answer format");
        }

        if (response.getError() != null) {
            throw new IllegalStateException("Error occurred in the ConverterService: " + getErrorResponseMessage(response.getError()));
        }

        if (response.isEndConvert() == null || !response.isEndConvert()) {
            throw new IllegalStateException("Conversion is not finished");
        }

        if (response.getPercent() == 0) {
            throw new IllegalStateException("Percent is null");
        }

        if (response.getFileUrl() == null || response.getFileUrl().isEmpty()) {
            throw new IllegalStateException("FileUrl is null");
        }

        return response.getFileUrl();
    }

    private static String getErrorResponseMessage(final int errorCode) {
        switch (errorCode) {
            case -8:
                return "Error document VKey";
            case -7:
                return "Error document request";
            case -6:
                return "Error database";
            case -5:
                return "Error unexpected guid";
            case -4:
                return "Error download error";
            case -3:
                return "Error convertation error";
            case -2:
                return "Error convertation timeout";
            case -1:
                return "Error convertation unknown";
            case 0:
                return null;
            default:
                return "ErrorCode = " + errorCode;
        }
    }
}
