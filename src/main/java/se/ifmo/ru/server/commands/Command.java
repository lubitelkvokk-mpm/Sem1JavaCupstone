package se.ifmo.ru.server.commands;

import se.ifmo.ru.common.Message;

public interface Command {
    void execute(Message msg);
}
