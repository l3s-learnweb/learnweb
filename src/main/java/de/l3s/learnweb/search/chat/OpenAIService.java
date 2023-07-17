package de.l3s.learnweb.search.chat;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

import de.l3s.learnweb.app.ConfigProvider;

@ApplicationScoped
public class OpenAIService {
    private static final Logger log = LogManager.getLogger(OpenAIService.class);
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private final String openAiEndpoint;
    private final String openAiDeployment;
    private final String openAiVersion = "2023-03-15-preview";
    private final String openAiKey;

    @Inject
    public OpenAIService(ConfigProvider config) {
        openAiEndpoint = config.getProperty("azure_openai_endpoint");
        openAiDeployment = config.getProperty("azure_openai_deployment");
        openAiKey = config.getProperty("azure_openai_key");
    }

    public ChatMessage completeChat(List<ChatMessage> messages) throws OpenAIException {
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setMessages(messages);

        HttpRequest httpRequest = createRequest(request);
        ChatCompletionResponse response = sendRequest(httpRequest);

        if (response.choices.isEmpty()) {
            throw new OpenAIException("No response from OpenAI");
        }

        ChatMessage answer = response.choices.get(0).message;
        answer.setTime(Instant.ofEpochSecond(response.created));
        return answer;
    }

    private HttpRequest createRequest(final ChatCompletionRequest data) {
        final URI requestUri = URI.create(openAiEndpoint + "openai/deployments/" + openAiDeployment + "/chat/completions?api-version=" + openAiVersion);
        final String requestBody = GSON.toJson(data);

        return HttpRequest.newBuilder()
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .uri(requestUri)
            .header("Content-Type", "application/json")
            .header("api-key", openAiKey)
            .build();
    }

    private ChatCompletionResponse sendRequest(final HttpRequest request) throws OpenAIException {
        try {
            long startTime = System.currentTimeMillis();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                long elapsedTime = System.currentTimeMillis() - startTime;
                log.debug("Request succeeded: {}ms", elapsedTime);
                ChatCompletionResponse responseObj = GSON.fromJson(response.body(), ChatCompletionResponse.class);
                return responseObj;
            }

            ErrorResponse errorResponse = GSON.fromJson(response.body(), ErrorResponse.class);
            log.error("Request failed: {}", response.body());
            throw new OpenAIException("OpenAI request failed: " + errorResponse);
        } catch (IOException | InterruptedException e) {
            log.fatal("An error occurred during OpenAI request {}", request, e);
            throw new OpenAIException("A network error occurred during OpenAI request.", e);
        }
    }
}
