package se.ifmo.ru.infra;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Конфигурация приложения.
 *
 * <p>Загружается из файла {@code config/app.properties} в рабочей директории;
 * если файла нет — из ресурсов класса (значения по умолчанию).</p>
 *
 * @param port                 порт сервера (0 — выбрать свободный)
 * @param host                 адрес сервера
 * @param ttlSeconds           время жизни ссылки, сек (задаётся системой)
 * @param codeLength           длина кода короткой ссылки
 * @param defaultMaxVisits     лимит переходов по умолчанию
 * @param maxVisitsLimit       максимально допустимый лимит переходов
 * @param purgeIntervalSeconds периодичность автоудаления протухших ссылок, сек
 * @param shortUrlPrefix       префикс короткой ссылки (например, clck.ru/)
 */
public record AppConfig(
        int port,
        String host,
        long ttlSeconds,
        int codeLength,
        int defaultMaxVisits,
        int maxVisitsLimit,
        long purgeIntervalSeconds,
        String shortUrlPrefix) {

    public static final String FILE_NAME = "app.properties";

    public static AppConfig load() {
        return load(Path.of("."));
    }

    public static AppConfig load(Path workDir) {
        Properties props = new Properties();
        Path file = workDir.resolve("config").resolve(FILE_NAME);
        if (Files.isRegularFile(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                props.load(in);
            } catch (IOException e) {
                throw new IllegalStateException("Не удалось прочитать " + file, e);
            }
        } else {
            try (InputStream in = AppConfig.class.getResourceAsStream("/config/" + FILE_NAME)) {
                if (in == null) {
                    throw new IllegalStateException("Файл конфигурации не найден: " + FILE_NAME);
                }
                props.load(in);
            } catch (IOException e) {
                throw new IllegalStateException("Не удалось прочитать конфигурацию из ресурсов", e);
            }
        }
        return new AppConfig(
                intProp(props, "server.port", 8080),
                props.getProperty("server.host", "127.0.0.1"),
                longProp(props, "link.ttl.seconds", 86_400),
                intProp(props, "link.code.length", 6),
                intProp(props, "link.default-max-visits", 100),
                intProp(props, "link.max-visits-limit", 1_000_000),
                longProp(props, "purge.interval.seconds", 60),
                props.getProperty("short.url.prefix", "clck.ru/"));
    }

    private static int intProp(Properties props, String key, int def) {
        String v = props.getProperty(key);
        if (v == null || v.isBlank()) {
            return def;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Параметр " + key + " должен быть целым числом, получено: " + v);
        }
    }

    private static long longProp(Properties props, String key, long def) {
        String v = props.getProperty(key);
        if (v == null || v.isBlank()) {
            return def;
        }
        try {
            return Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Параметр " + key + " должен быть целым числом, получено: " + v);
        }
    }
}
