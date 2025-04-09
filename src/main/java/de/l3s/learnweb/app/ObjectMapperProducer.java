package de.l3s.learnweb.app;

import java.io.Serial;
import java.io.Serializable;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@ApplicationScoped
public class ObjectMapperProducer implements Serializable {
    @Serial
    private static final long serialVersionUID = 1151315245069640668L;

    private final ObjectMapper objectMapper;

    public ObjectMapperProducer() {
        objectMapper = new ObjectMapper();
        // Register JavaTimeModule to handle Java 8 date/time types
        objectMapper.registerModule(new JavaTimeModule());
        // Disable writing dates as timestamps (use ISO format instead)
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Produces
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
