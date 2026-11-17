package se.ifmo.ru.server.commands;

import se.ifmo.ru.common.Message;
import se.ifmo.ru.common.MessageType;
import se.ifmo.ru.server.ServerContext;

/**
 * Команда протокола: обрабатывает запрос и формирует ответ.
 */
public interface Command {

    MessageType type();

    Message handle(Message request, ServerContext ctx);
}
