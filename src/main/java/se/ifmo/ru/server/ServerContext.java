package se.ifmo.ru.server;

import se.ifmo.ru.core.UrlShortenerService;
import se.ifmo.ru.infra.AppConfig;

/**
 * Контекст, доступный всем командам сервера.
 */
public class ServerContext {

    private final UrlShortenerService service;
    private final AppConfig config;

    public ServerContext(UrlShortenerService service, AppConfig config) {
        this.service = service;
        this.config = config;
    }

    public UrlShortenerService service() {
        return service;
    }

    public AppConfig config() {
        return config;
    }
}
