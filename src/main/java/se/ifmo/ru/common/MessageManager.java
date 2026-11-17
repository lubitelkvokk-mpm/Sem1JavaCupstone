package se.ifmo.ru.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class MessageManager {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private MessageManager() {
    }

    public static String toJson(Message message) {
        try {
            return MAPPER.writeValueAsString(message);
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось сериализовать сообщение", e);
        }
    }

    public static Message fromJson(String json) {
        try {
            return MAPPER.readValue(json, Message.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Некорректный JSON сообщения", e);
        }
    }

    public static String payload(Object dto) {
        try {
            return MAPPER.writeValueAsString(dto);
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось сериализовать payload", e);
        }
    }

    public static <T> T payload(Message message, Class<T> type) {
        try {
            return MAPPER.readValue(message.payload(), type);
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось разобрать payload сообщения", e);
        }
    }
}
