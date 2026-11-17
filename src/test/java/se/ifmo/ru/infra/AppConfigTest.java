package se.ifmo.ru.infra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AppConfigTest {

    @Test
    void loadsDefaultsFromClasspath() {
        AppConfig config = AppConfig.load(Path.of("/nonexistent-dir"));
        assertEquals(8080, config.port());
        assertEquals("127.0.0.1", config.host());
        assertEquals(86_400, config.ttlSeconds());
        assertEquals(6, config.codeLength());
        assertEquals(100, config.defaultMaxVisits());
        assertEquals(1_000_000, config.maxVisitsLimit());
        assertEquals(60, config.purgeIntervalSeconds());
        assertEquals("clck.ru/", config.shortUrlPrefix());
    }

    @Test
    void loadsFromFileWhenPresent(@TempDir Path dir) throws IOException {
        Path configDir = dir.resolve("config");
        Files.createDirectories(configDir);
        Files.writeString(configDir.resolve("app.properties"), """
                server.port=9090
                link.ttl.seconds=3600
                link.code.length=8
                link.default-max-visits=42
                link.max-visits-limit=5000
                purge.interval.seconds=5
                short.url.prefix=short.local/
                """);
        AppConfig config = AppConfig.load(dir);
        assertEquals(9090, config.port());
        assertEquals(3600, config.ttlSeconds());
        assertEquals(8, config.codeLength());
        assertEquals(42, config.defaultMaxVisits());
        assertEquals(5000, config.maxVisitsLimit());
        assertEquals(5, config.purgeIntervalSeconds());
        assertEquals("short.local/", config.shortUrlPrefix());
    }

    @Test
    void missingKeysFallBackToDefaults(@TempDir Path dir) throws IOException {
        Path configDir = dir.resolve("config");
        Files.createDirectories(configDir);
        Files.writeString(configDir.resolve("app.properties"), "server.port=7070\n");
        AppConfig config = AppConfig.load(dir);
        assertEquals(7070, config.port());
        assertEquals(86_400, config.ttlSeconds());
    }

    @Test
    void invalidNumberRejected(@TempDir Path dir) throws IOException {
        Path configDir = dir.resolve("config");
        Files.createDirectories(configDir);
        Files.writeString(configDir.resolve("app.properties"), "server.port=abc\n");
        assertThrows(IllegalStateException.class, () -> AppConfig.load(dir));
    }
}
