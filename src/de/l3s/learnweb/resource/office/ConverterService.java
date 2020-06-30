package de.l3s.learnweb.resource.office;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

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
        ConverterResponse converterResponse = new ConverterResponse();

        try {
            // previously we used unsafe ssl client, but now it seems that certificate is valid and all right
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(learnweb.getProperties().getProperty("FILES.DOCSERVICE.URL.CONVERTER")))
                .header("Content-type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(model)))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.body() != null) {
                converterResponse = gson.fromJson(response.body(), ConverterResponse.class);
            }
        } catch (IOException | InterruptedException e) {
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

    public static class ConverterException extends Exception {
        private static final long serialVersionUID = 8151643724813680762L;

        public ConverterException(String errorMessage) {
            super(errorMessage);
        }
    }
}
