package se.ifmo.ru.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class UrlValidatorTest {

    @Test
    void acceptsHttpUrl() {
        assertDoesNotThrow(() -> UrlValidator.validate("http://example.com"));
    }

    @Test
    void acceptsHttpsUrlWithPathAndQuery() {
        assertDoesNotThrow(() ->
                UrlValidator.validate("https://www.baeldung.com/java-9-http-client?x=1#top"));
    }

    @Test
    void acceptsUrlWithPort() {
        assertDoesNotThrow(() -> UrlValidator.validate("https://example.com:8443/path"));
    }

    @Test
    void rejectsNull() {
        assertThrows(InvalidUrlException.class, () -> UrlValidator.validate(null));
    }

    @Test
    void rejectsBlank() {
        assertThrows(InvalidUrlException.class, () -> UrlValidator.validate("   "));
    }

    @Test
    void rejectsUrlWithoutScheme() {
        assertThrows(InvalidUrlException.class, () -> UrlValidator.validate("example.com/page"));
    }

    @Test
    void rejectsUnsupportedScheme() {
        assertThrows(InvalidUrlException.class, () -> UrlValidator.validate("ftp://example.com/file"));
    }

    @Test
    void rejectsUrlWithoutHost() {
        assertThrows(InvalidUrlException.class, () -> UrlValidator.validate("http:///only/path"));
    }

    @Test
    void rejectsUrlWithSpaces() {
        assertThrows(InvalidUrlException.class, () -> UrlValidator.validate("ht tp://example.com"));
    }
}
