package se.ifmo.ru.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Локальное сохранение UUID пользователя между запусками клиента.
 */
public final class UserUuidStore {

    private static final Path FILE =
            Path.of(System.getProperty("user.home"), ".urlshortener-uuid");

    private UserUuidStore() {
    }

    public static UUID load() {
        try {
            if (Files.isRegularFile(FILE)) {
                String content = Files.readString(FILE).trim();
                if (!content.isEmpty()) {
                    return UUID.fromString(content);
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Не удалось прочитать сохранённый UUID, будет создан новый");
        }
        return null;
    }

    public static void save(UUID uuid) {
        try {
            Path parent = FILE.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(FILE, uuid.toString());
        } catch (IOException e) {
            System.err.println("Не удалось сохранить UUID: " + e.getMessage());
        }
    }
}
