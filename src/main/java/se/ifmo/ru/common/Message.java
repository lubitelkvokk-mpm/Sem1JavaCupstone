package se.ifmo.ru.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.UUID;

@Data
public class Message {
    private UUID id;
    private MessageType messageType;
    private String message;
    private BufferedWriter bufferedWriter;
    private BufferedReader bufferedReader;

    public Message(UUID id, MessageType messageType) {
        this.id = id;
        this.messageType = messageType;
    }
}
