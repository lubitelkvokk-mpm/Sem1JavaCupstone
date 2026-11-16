package se.ifmo.ru.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import se.ifmo.ru.server.commands.AuthResponseCommand;
import se.ifmo.ru.server.commands.AuthorizeCommand;
import se.ifmo.ru.server.commands.Command;
import se.ifmo.ru.server.commands.UrlCutCommand;

import java.io.BufferedReader;
import java.io.BufferedWriter;

public class MessageManager {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static String generateMessage(Message message) throws JsonProcessingException {
        String json = mapper.writeValueAsString(message);
        System.out.println(json);
        return json;
    }

    private static Message parseMessage(String message) throws JsonProcessingException {
        return mapper.readValue(message, Message.class);
    }

    public static void processMessage(String message, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        try {
            Message msg = parseMessage(message);
            msg.setBufferedReader(bufferedReader);
            msg.setBufferedWriter(bufferedWriter);
            Command cmd = switch (msg.getMessageType()) {
                case MessageType.AUTH_REQ -> new AuthorizeCommand();
                case AUTH_RESPONSE -> new AuthResponseCommand();
                case STRING_CUT -> new UrlCutCommand();
            };
            cmd.execute(msg);
        } catch (Exception e) {
            System.err.println("MessageManager: Ошибка парсинга сообщения");
        }
    }

}
