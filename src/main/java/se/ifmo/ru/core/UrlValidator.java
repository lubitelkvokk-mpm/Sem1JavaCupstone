package se.ifmo.ru.core;

import java.net.URI;
import java.net.URISyntaxException;

public final class UrlValidator {

    private UrlValidator() {
    }

    /**
     * Проверяет, что строка является корректным http/https URL.
     *
     * @throws InvalidUrlException если URL невалиден
     */
    public static void validate(String url) {
        if (url == null || url.isBlank()) {
            throw new InvalidUrlException("URL не может быть пустым");
        }
        String trimmed = url.trim();
        try {
            URI uri = new URI(trimmed);
            String scheme = uri.getScheme();
            if (scheme == null
                    || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                throw new InvalidUrlException("Поддерживаются только ссылки http и https");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new InvalidUrlException("В URL отсутствует доменное имя");
            }
        } catch (URISyntaxException e) {
            throw new InvalidUrlException("Некорректный формат URL: " + e.getMessage());
        }
    }
}
