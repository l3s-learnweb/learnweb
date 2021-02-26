package de.l3s.learnweb.resource.office;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

import de.l3s.learnweb.resource.File;
import de.l3s.learnweb.resource.office.converter.model.ConverterRequest;
import de.l3s.learnweb.resource.office.converter.model.ConverterResponse;
import de.l3s.util.UrlHelper;

public final class ConverterService {
    private static final Logger log = LogManager.getLogger(ConverterService.class);

    public static String convert(final String converterService, final File file) {
        return convert(converterService, createConverterRequest(file));
    }

    public static String convert(final String converterService, final ConverterRequest converterRequest) {
        try {
            Gson gson = new Gson();
            HttpClient client = HttpClient.newBuilder().sslContext(UrlHelper.getUnsafeSSLContext()).build();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(converterService))
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
        String fileName = file.getName();
        String fileExt = fileName.substring(fileName.lastIndexOf('.'));
        String key = FileUtility.generateRevisionId(file);
        return new ConverterRequest(fileExt, "png", fileName, file.getAbsoluteUrl(), key);
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
                return "Error during download";
            case -3:
                return "Error during convertation";
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
