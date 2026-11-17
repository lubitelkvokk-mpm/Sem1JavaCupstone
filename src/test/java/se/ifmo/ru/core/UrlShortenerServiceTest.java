package se.ifmo.ru.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import se.ifmo.ru.common.RedirectStatus;
import se.ifmo.ru.infra.AppConfig;
import se.ifmo.ru.infra.InMemoryLinkRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UrlShortenerServiceTest {

    private static final UUID USER_A = UUID.randomUUID();
    private static final UUID USER_B = UUID.randomUUID();

    private UrlShortenerService service;
    private NotificationService notifications;

    @BeforeEach
    void setUp() {
        notifications = new NotificationService();
        service = newService(3600);
    }

    private UrlShortenerService newService(long ttlSeconds) {
        AppConfig config = new AppConfig(0, "127.0.0.1", ttlSeconds, 6, 100, 1_000_000, 60, "clck.ru/");
        return new UrlShortenerService(new InMemoryLinkRepository(), notifications,
                new ShortCodeGenerator(6), config);
    }

    @Test
    void createLinkReturnsLinkWithCodeAndTtl() {
        ShortLink link = service.createLink(USER_A, "https://example.com/a", null);
        assertEquals(6, link.getCode().length());
        assertEquals(USER_A, link.getOwner());
        assertEquals("https://example.com/a", link.getUrl());
        assertEquals(100, link.getMaxVisits());
        assertTrue(link.getExpiresAt().isAfter(link.getCreatedAt()));
    }

    @Test
    void differentUsersGetDifferentCodesForSameUrl() {
        ShortLink a = service.createLink(USER_A, "https://example.com/same", null);
        ShortLink b = service.createLink(USER_B, "https://example.com/same", null);
        assertNotEquals(a.getCode(), b.getCode());
    }

    @Test
    void sameUserGetsDifferentCodesForRepeatedShortening() {
        ShortLink first = service.createLink(USER_A, "https://example.com/same", null);
        ShortLink second = service.createLink(USER_A, "https://example.com/same", null);
        assertNotEquals(first.getCode(), second.getCode());
    }

    @Test
    void redirectIncrementsVisitCount() {
        ShortLink link = service.createLink(USER_A, "https://example.com/a", 10);
        ResolveResult result = service.resolve(link.getCode());
        assertEquals(RedirectStatus.OK, result.status());
        assertEquals("https://example.com/a", result.originalUrl());
        assertEquals(9, result.remainingVisits());
        assertEquals(1, link.getVisitCount());
    }

    @Test
    void redirectAfterLimitExhaustedIsBlocked() {
        ShortLink link = service.createLink(USER_A, "https://example.com/a", 2);
        assertEquals(RedirectStatus.OK, service.resolve(link.getCode()).status());
        assertEquals(RedirectStatus.OK, service.resolve(link.getCode()).status());
        assertEquals(RedirectStatus.LIMIT_EXCEEDED, service.resolve(link.getCode()).status());
        assertEquals(2, link.getVisitCount());
    }

    @Test
    void exhaustedLimitNotifiesOwner() {
        ShortLink link = service.createLink(USER_A, "https://example.com/a", 1);
        service.resolve(link.getCode());
        List<Notification> unread = notifications.markAsRead(USER_A);
        assertEquals(1, unread.size());
        assertTrue(unread.get(0).getText().contains("исчерпан"));
    }

    @Test
    void expiredLinkIsBlockedAndDeleted() throws InterruptedException {
        UrlShortenerService shortTtlService = newService(1);
        ShortLink link = shortTtlService.createLink(USER_A, "https://example.com/a", 10);
        Thread.sleep(1100);
        ResolveResult result = shortTtlService.resolve(link.getCode());
        assertEquals(RedirectStatus.EXPIRED, result.status());
        assertEquals(RedirectStatus.NOT_FOUND, shortTtlService.resolve(link.getCode()).status());
    }

    @Test
    void expiredLinkNotifiesOwner() throws InterruptedException {
        UrlShortenerService shortTtlService = newService(1);
        ShortLink link = shortTtlService.createLink(USER_A, "https://example.com/a", 10);
        Thread.sleep(1100);
        shortTtlService.resolve(link.getCode());
        List<Notification> unread = notifications.markAsRead(USER_A);
        assertEquals(1, unread.size());
        assertTrue(unread.get(0).getText().contains("истекла"));
    }

    @Test
    void unknownCodeReturnsNotFound() {
        assertEquals(RedirectStatus.NOT_FOUND, service.resolve("NOPE").status());
    }

    @Test
    void deleteByOwnerSucceeds() {
        ShortLink link = service.createLink(USER_A, "https://example.com/a", 10);
        assertTrue(service.deleteLink(USER_A, link.getCode()));
        assertEquals(RedirectStatus.NOT_FOUND, service.resolve(link.getCode()).status());
    }

    @Test
    void deleteByOtherUserFails() {
        ShortLink link = service.createLink(USER_A, "https://example.com/a", 10);
        assertFalse(service.deleteLink(USER_B, link.getCode()));
        assertEquals(RedirectStatus.OK, service.resolve(link.getCode()).status());
    }

    @Test
    void updateByOwnerChangesLimit() {
        ShortLink link = service.createLink(USER_A, "https://example.com/a", 5);
        Optional<ShortLink> updated = service.updateLink(USER_A, link.getCode(), 50);
        assertTrue(updated.isPresent());
        assertEquals(50, updated.get().getMaxVisits());
        assertEquals(link.getVisitCount(), updated.get().getVisitCount());
    }

    @Test
    void updateByOtherUserFails() {
        ShortLink link = service.createLink(USER_A, "https://example.com/a", 5);
        assertTrue(service.updateLink(USER_B, link.getCode(), 50).isEmpty());
    }

    @Test
    void updateWithInvalidLimitRejected() {
        ShortLink link = service.createLink(USER_A, "https://example.com/a", 5);
        assertThrows(IllegalArgumentException.class,
                () -> service.updateLink(USER_A, link.getCode(), 0));
        assertThrows(IllegalArgumentException.class,
                () -> service.updateLink(USER_A, link.getCode(), 2_000_000));
    }

    @Test
    void listReturnsOnlyOwnLinks() {
        service.createLink(USER_A, "https://example.com/a", 10);
        service.createLink(USER_A, "https://example.com/b", 10);
        service.createLink(USER_B, "https://example.com/c", 10);
        assertEquals(2, service.listLinks(USER_A).size());
        assertEquals(1, service.listLinks(USER_B).size());
    }

    @Test
    void purgeExpiredRemovesOnlyExpired() throws InterruptedException {
        UrlShortenerService shortTtlService = newService(1);
        ShortLink expired = shortTtlService.createLink(USER_A, "https://example.com/x", 10);
        Thread.sleep(1100);
        // новая ссылка в том же хранилище ещё жива
        ShortLink fresh = shortTtlService.createLink(USER_A, "https://example.com/y", 10);
        List<ShortLink> removed = shortTtlService.purgeExpired();
        assertEquals(1, removed.size());
        assertEquals(expired.getCode(), removed.get(0).getCode());
        assertEquals(RedirectStatus.OK, shortTtlService.resolve(fresh.getCode()).status());
    }

    @Test
    void invalidUrlRejected() {
        assertThrows(InvalidUrlException.class,
                () -> service.createLink(USER_A, "not a url", 10));
        assertThrows(InvalidUrlException.class,
                () -> service.createLink(USER_A, "ftp://example.com", 10));
    }

    @Test
    void maxVisitsAboveLimitRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createLink(USER_A, "https://example.com/a", 2_000_000));
    }

    @Test
    void authenticateReturnsSameUuidForKnownUser() {
        UUID first = service.authenticate(null);
        UUID second = service.authenticate(first);
        assertEquals(first, second);
    }

    @Test
    void authenticateCreatesNewUuidForUnknownUser() {
        UUID first = service.authenticate(null);
        UUID other = service.authenticate(UUID.randomUUID());
        assertNotEquals(first, other);
    }

    @Test
    void shortUrlUsesConfiguredPrefix() {
        ShortLink link = service.createLink(USER_A, "https://example.com/a", 10);
        assertEquals("clck.ru/" + link.getCode(), service.shortUrl(link));
    }
}
