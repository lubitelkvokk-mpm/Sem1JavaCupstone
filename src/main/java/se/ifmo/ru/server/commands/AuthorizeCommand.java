package se.ifmo.ru.server.commands;

import se.ifmo.ru.common.Message;
import se.ifmo.ru.common.ProtocolParser;
import se.ifmo.ru.common.storage.UrlUserStorage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.UUID;

public class AuthorizeCommand implements Command{
    @Override
    public void execute(Message msg) {
        BufferedWriter bufferedWriter = msg.getBufferedWriter();
        try{
            UUID uid = msg.getId();
            if ((uid = .getUserUUID(authJson).id()) == null) {
                UUID id = UUID.randomUUID();
                bufferedWriter.write(id.toString());
                UrlUserStorage.//put
                return id;
            }
            return uid;
        }
        catch (Exception e) {

        }
    }
}
