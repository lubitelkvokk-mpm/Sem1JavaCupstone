package se.ifmo.ru.core;

import java.security.SecureRandom;
import java.util.function.Predicate;

/**
 * Генератор уникальных base62-кодов коротких ссылок.
 */
public class ShortCodeGenerator {

    private static final String ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private final SecureRandom random = new SecureRandom();
    private final int length;

    public ShortCodeGenerator(int length) {
        if (length < 4) {
            throw new IllegalArgumentException("Длина кода должна быть не меньше 4");
        }
        this.length = length;
    }

    public int length() {
        return length;
    }

    /**
     * Генерирует код, который ещё не занят.
     *
     * @param exists предикат: true, если код уже существует
     */
    public String generateUnique(Predicate<String> exists) {
        while (true) {
            String code = randomCode();
            if (!exists.test(code)) {
                return code;
            }
        }
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
