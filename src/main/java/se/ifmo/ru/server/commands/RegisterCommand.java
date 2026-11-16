package se.ifmo.ru.server.commands;

import se.ifmo.ru.common.Message;
import se.ifmo.ru.common.storage.UrlUserStorage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.UUID;

import static se.ifmo.ru.common.storage.UrlUserStorage.clientStorage;

public class RegisterCommand implements Command {
    @Override
    public void execute(Message msg) {
        BufferedWriter bufferedWriter = msg.getBufferedWriter();
        try {
            UUID id = UrlUserStorage.createUser();
            bufferedWriter.write(id.toString());
        } catch (IOException ex) {
            // обработочку добавить
            throw new RuntimeException(ex);
        }
    }

}
