package se.ifmo.ru.server;

import se.ifmo.ru.infra.AppConfig;

import java.io.IOException;

public class MainServer {

    public static void main(String[] args) throws IOException {
        AppConfig config = AppConfig.load();
        try (UrlShortenerServer server = new UrlShortenerServer(config)) {
            server.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Остановка сервера...");
                server.close();
            }, "shutdown"));
            // NIO-цикл событий в основном потоке
            server.eventLoop();
        }
    }
}
