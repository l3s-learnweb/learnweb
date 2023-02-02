package de.l3s.thumbmaker;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serial;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

public class ThumbMaker implements Serializable {
    @Serial
    private static final long serialVersionUID = -3951566663712339875L;
    private static final Logger log = LogManager.getLogger(ThumbMaker.class);
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private final String serverUrl;

    public ThumbMaker(final String serverUrl) {
        this.serverUrl = serverUrl;
    }

    private HttpRequest createRequest(final String endpoint, final Object object) {
        final URI requestUri = URI.create(this.serverUrl + endpoint);
        final String requestBody = GSON.toJson(object);

        log.debug("Preparing request {} : {}", requestUri.toString(), requestBody);
        return HttpRequest.newBuilder().POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .header("Content-type", "application/json")
            .uri(requestUri).build();
    }

    private InputStream sendRequest(final HttpRequest request) {
        try {
            long startTime = System.currentTimeMillis();
            HttpResponse<InputStream> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                long elapsedTime = System.currentTimeMillis() - startTime;
                log.debug("Request succeeded: {}ms", elapsedTime);
                return response.body();
            }

            ErrorResponse errorResponse = GSON.fromJson(new InputStreamReader(response.body(), StandardCharsets.UTF_8), ErrorResponse.class);
            log.error("Request failed: {} {}; {}", errorResponse.statusCode, errorResponse.error, errorResponse.message);
            throw new IllegalStateException("ThumbMaker request failed: " + errorResponse.error);
        } catch (IOException | InterruptedException e) {
            log.fatal("An error occurred during ThumbMaker request {}", request, e);
            return null;
        }
    }

    public InputStream makeScreenshot(final String url, final ScreenshotOptions options) {
        HttpRequest request = createRequest("/screenshot", new ScreenshotRequest(url, options));
        return sendRequest(request);
    }

    public InputStream makeFilePreview(final String downloadUrl, final FilePreviewOptions options) {
        HttpRequest request = createRequest("/filepreview", new FilePreviewRequest(downloadUrl, options));
        return sendRequest(request);
    }

    private record ErrorResponse(Integer statusCode, String error, String message) {}

    private record ScreenshotRequest(String url, ScreenshotOptions options) {}

    private record FilePreviewRequest(String downloadUrl, FilePreviewOptions options) {}
}
