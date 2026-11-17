package se.ifmo.ru.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import se.ifmo.ru.client.ClientSession;
import se.ifmo.ru.common.LinkStatus;
import se.ifmo.ru.common.Message;
import se.ifmo.ru.common.MessageManager;
import se.ifmo.ru.common.MessageType;
import se.ifmo.ru.common.RedirectStatus;
import se.ifmo.ru.common.dto.AuthResponseDto;
import se.ifmo.ru.common.dto.DeleteRequestDto;
import se.ifmo.ru.common.dto.DeleteResponseDto;
import se.ifmo.ru.common.dto.ListResponseDto;
import se.ifmo.ru.common.dto.NotificationsResponseDto;
import se.ifmo.ru.common.dto.RedirectRequestDto;
import se.ifmo.ru.common.dto.RedirectResponseDto;
import se.ifmo.ru.common.dto.ShortenRequestDto;
import se.ifmo.ru.common.dto.ShortenResponseDto;
import se.ifmo.ru.common.dto.UpdateRequestDto;
import se.ifmo.ru.common.dto.UpdateResponseDto;
import se.ifmo.ru.core.ShortLink;
import se.ifmo.ru.infra.AppConfig;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Интеграционный тест: реальные серверы + реальные сокет-клиенты,
 * сценарии из ТЗ (уникальность, лимит, TTL, права владельца, уведомления).
 *
 * <p>Два сервера: основной (TTL 60 с) — для сценариев работы со ссылками,
 * и с коротким TTL (2 с) — для сценариев истечения срока жизни.
 * Клиенты подключаются последовательно, каждый тест закрывает свою сессию.</p>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IntegrationTest {

    private static final String URL = "https://www.baeldung.com/java-9-http-client";

    private UrlShortenerServer mainServer;
    private UrlShortenerServer shortTtlServer;
    private int port;
    private int shortTtlPort;
    private UUID userA;
    private UUID userB;
    private String codeA1;
    private String codeA2;
    private String codeB1;

    @BeforeAll
    void startServers() throws IOException {
        mainServer = new UrlShortenerServer(
                new AppConfig(0, "127.0.0.1", 60, 6, 100, 1_000_000, 60, "clck.ru/"));
        mainServer.start();
        port = mainServer.actualPort();
        startAcceptor(mainServer);

        shortTtlServer = new UrlShortenerServer(
                new AppConfig(0, "127.0.0.1", 2, 6, 100, 1_000_000, 60, "clck.ru/"));
        shortTtlServer.start();
        shortTtlPort = shortTtlServer.actualPort();
        startAcceptor(shortTtlServer);
    }

    @AfterAll
    void stopServers() {
        mainServer.close();
        shortTtlServer.close();
    }

    private void startAcceptor(UrlShortenerServer server) {
        Thread acceptor = new Thread(server::eventLoop, "test-acceptor");
        acceptor.setDaemon(true);
        acceptor.start();
    }

    private ClientSession connect() throws IOException {
        return new ClientSession("127.0.0.1", port);
    }

    private ClientSession connectShortTtl() throws IOException {
        return new ClientSession("127.0.0.1", shortTtlPort);
    }

    private UUID auth(ClientSession session, UUID stored) throws IOException {
        Message response = session.send(new Message(UUID.randomUUID(), stored,
                MessageType.AUTH_REQ, null));
        assertEquals(MessageType.AUTH_RESP, response.type());
        return MessageManager.payload(response, AuthResponseDto.class).userUuid();
    }

    private UUID auth(ClientSession session) throws IOException {
        return auth(session, null);
    }

    private ShortenResponseDto shorten(ClientSession session, UUID user, String url,
                                       Integer maxVisits) throws IOException {
        Message response = session.send(new Message(UUID.randomUUID(), user,
                MessageType.SHORTEN_REQ,
                MessageManager.payload(new ShortenRequestDto(url, maxVisits))));
        assertEquals(MessageType.SHORTEN_RESP, response.type(),
                "ожидался SHORTEN_RESP, получен: " + response.payload());
        return MessageManager.payload(response, ShortenResponseDto.class);
    }

    private RedirectResponseDto open(ClientSession session, UUID user, String code)
            throws IOException {
        Message response = session.send(new Message(UUID.randomUUID(), user,
                MessageType.REDIRECT_REQ,
                MessageManager.payload(new RedirectRequestDto(code))));
        assertEquals(MessageType.REDIRECT_RESP, response.type());
        return MessageManager.payload(response, RedirectResponseDto.class);
    }

    private NotificationsResponseDto notifications(ClientSession session, UUID user)
            throws IOException {
        Message response = session.send(new Message(UUID.randomUUID(), user,
                MessageType.NOTIFICATIONS_REQ, null));
        assertEquals(MessageType.NOTIFICATIONS_RESP, response.type());
        return MessageManager.payload(response, NotificationsResponseDto.class);
    }

    @Test
    @Order(1)
    void usersGetUniqueUuids() throws IOException {
        try (ClientSession a = connect()) {
            userA = auth(a);
        }
        try (ClientSession b = connect()) {
            userB = auth(b);
        }
        assertNotNull(userA);
        assertNotNull(userB);
        assertNotEquals(userA, userB);
    }

    @Test
    @Order(2)
    void sameUrlShortenedByDifferentUsersGivesUniqueCodes() throws IOException {
        try (ClientSession a = connect()) {
            assertEquals(userA, auth(a, userA), "сохранённый UUID должен подтверждаться");
            ShortenResponseDto first = shorten(a, userA, URL, 2);
            ShortenResponseDto second = shorten(a, userA, URL, 10);
            ShortenResponseDto third = shorten(a, userA, URL, null);
            codeA1 = first.shortCode();
            codeA2 = second.shortCode();
            assertEquals("clck.ru/" + codeA1, first.shortUrl());
            assertEquals(2, first.maxVisits());
            assertEquals(10, second.maxVisits());
            assertEquals(100, third.maxVisits(), "лимит по умолчанию из конфига");
            assertNotEquals(codeA1, codeA2);
            // третью ссылку удаляем, чтобы не мешать последующим тестам
            Message del = a.send(new Message(UUID.randomUUID(), userA,
                    MessageType.DELETE_REQ,
                    MessageManager.payload(new DeleteRequestDto(third.shortCode()))));
            DeleteResponseDto delDto = MessageManager.payload(del, DeleteResponseDto.class);
            assertEquals(true, delDto.deleted());
        }
        try (ClientSession b = connect()) {
            auth(b);
            ShortenResponseDto fromB = shorten(b, userB, URL, 5);
            codeB1 = fromB.shortCode();
            Set<String> codes = Set.of(codeA1, codeA2, codeB1);
            assertEquals(3, codes.size(), "коды должны быть уникальны у всех пользователей");
        }
    }

    @Test
    @Order(3)
    void redirectWorksAndCountsVisits() throws IOException {
        try (ClientSession a = connect()) {
            auth(a);
            RedirectResponseDto first = open(a, userA, codeA1);
            assertEquals(RedirectStatus.OK, first.status());
            assertEquals(URL, first.originalUrl());
            assertEquals(1, first.remainingVisits());

            RedirectResponseDto second = open(a, userA, codeA1);
            assertEquals(RedirectStatus.OK, second.status());
            assertEquals(0, second.remainingVisits());
        }
    }

    @Test
    @Order(4)
    void redirectBlockedAfterLimitExhausted() throws IOException {
        try (ClientSession a = connect()) {
            auth(a);
            RedirectResponseDto blocked = open(a, userA, codeA1);
            assertEquals(RedirectStatus.LIMIT_EXCEEDED, blocked.status());
        }
    }

    @Test
    @Order(5)
    void ownerReceivesLimitNotification() throws IOException {
        try (ClientSession a = connect()) {
            auth(a);
            NotificationsResponseDto dto = notifications(a, userA);
            assertTrue(dto.notifications().stream()
                            .anyMatch(n -> n.text().contains("исчерпан")),
                    "ожидалось уведомление о лимите, получено: " + dto.notifications());
            // повторный запрос — уведомление уже прочитано
            NotificationsResponseDto dto2 = notifications(a, userA);
            assertTrue(dto2.notifications().stream()
                            .noneMatch(n -> n.text().contains("исчерпан")));
        }
    }

    @Test
    @Order(6)
    void listShowsOnlyOwnLinksWithStatus() throws IOException {
        try (ClientSession a = connect()) {
            auth(a);
            Message response = a.send(new Message(UUID.randomUUID(), userA,
                    MessageType.LIST_REQ, null));
            assertEquals(MessageType.LIST_RESP, response.type());
            ListResponseDto dto = MessageManager.payload(response, ListResponseDto.class);
            assertEquals(2, dto.links().size());
            assertTrue(dto.links().stream().anyMatch(l ->
                    l.shortCode().equals(codeA1)
                            && l.status() == LinkStatus.LIMIT_EXCEEDED
                            && l.visits() == 2));
            assertTrue(dto.links().stream().anyMatch(l ->
                    l.shortCode().equals(codeA2) && l.status() == LinkStatus.ACTIVE));
        }
        try (ClientSession b = connect()) {
            auth(b);
            Message response = b.send(new Message(UUID.randomUUID(), userB,
                    MessageType.LIST_REQ, null));
            ListResponseDto dto = MessageManager.payload(response, ListResponseDto.class);
            assertEquals(1, dto.links().size());
            assertEquals(codeB1, dto.links().get(0).shortCode());
        }
    }

    @Test
    @Order(7)
    void onlyOwnerCanUpdateAndDelete() throws IOException {
        try (ClientSession b = connect()) {
            auth(b);
            Message update = b.send(new Message(UUID.randomUUID(), userB,
                    MessageType.UPDATE_REQ,
                    MessageManager.payload(new UpdateRequestDto(codeA1, 50))));
            UpdateResponseDto updateDto = MessageManager.payload(update, UpdateResponseDto.class);
            assertNotNull(updateDto.error(), "чужую ссылку менять нельзя");

            Message delete = b.send(new Message(UUID.randomUUID(), userB,
                    MessageType.DELETE_REQ,
                    MessageManager.payload(new DeleteRequestDto(codeA1))));
            DeleteResponseDto deleteDto = MessageManager.payload(delete, DeleteResponseDto.class);
            assertEquals(false, deleteDto.deleted(), "чужую ссылку удалять нельзя");
        }
        try (ClientSession a = connect()) {
            auth(a);
            Message update = a.send(new Message(UUID.randomUUID(), userA,
                    MessageType.UPDATE_REQ,
                    MessageManager.payload(new UpdateRequestDto(codeA1, 50))));
            UpdateResponseDto updateDto = MessageManager.payload(update, UpdateResponseDto.class);
            assertEquals(null, updateDto.error());
            assertEquals(50, updateDto.link().maxVisits());

            // после увеличения лимита переходы снова возможны
            RedirectResponseDto redirect = open(a, userA, codeA1);
            assertEquals(RedirectStatus.OK, redirect.status());

            Message delete = a.send(new Message(UUID.randomUUID(), userA,
                    MessageType.DELETE_REQ,
                    MessageManager.payload(new DeleteRequestDto(codeA2))));
            DeleteResponseDto deleteDto = MessageManager.payload(delete, DeleteResponseDto.class);
            assertEquals(true, deleteDto.deleted());

            Message list = a.send(new Message(UUID.randomUUID(), userA,
                    MessageType.LIST_REQ, null));
            ListResponseDto listDto = MessageManager.payload(list, ListResponseDto.class);
            assertEquals(1, listDto.links().size());
            assertEquals(codeA1, listDto.links().get(0).shortCode());
        }
    }

    @Test
    @Order(8)
    void invalidUrlReturnsError() throws IOException {
        try (ClientSession a = connect()) {
            auth(a);
            Message response = a.send(new Message(UUID.randomUUID(), userA,
                    MessageType.SHORTEN_REQ,
                    MessageManager.payload(new ShortenRequestDto("не url", null))));
            assertEquals(MessageType.ERROR, response.type());
        }
    }

    @Test
    @Order(9)
    void expiredLinkIsBlockedAndNotifiesOwner() throws InterruptedException, IOException {
        UUID owner;
        String code;
        try (ClientSession a = connectShortTtl()) {
            owner = auth(a);
            ShortenResponseDto dto = shorten(a, owner, "https://example.com/ttl", 10);
            code = dto.shortCode();
        }
        Thread.sleep(2500); // TTL = 2 с
        try (ClientSession a = connectShortTtl()) {
            auth(a);
            RedirectResponseDto result = open(a, owner, code);
            assertEquals(RedirectStatus.EXPIRED, result.status());
        }
        try (ClientSession a = connectShortTtl()) {
            auth(a);
            NotificationsResponseDto dto = notifications(a, owner);
            assertTrue(dto.notifications().stream().anyMatch(n -> n.text().contains("истекла")),
                    "ожидалось уведомление об истечении TTL");
        }
    }

    @Test
    @Order(10)
    void unknownCodeReturnsNotFound() throws IOException {
        try (ClientSession a = connectShortTtl()) {
            auth(a);
            assertEquals(RedirectStatus.NOT_FOUND, open(a, userA, "ZZZZZZ").status());
        }
    }

    @Test
    @Order(11)
    void purgeExpiredRemovesExpiredLinks() throws InterruptedException, IOException {
        UUID owner;
        String code;
        try (ClientSession a = connectShortTtl()) {
            owner = auth(a);
            ShortenResponseDto dto = shorten(a, owner, "https://example.com/purge", 10);
            code = dto.shortCode();
        }
        Thread.sleep(2500); // TTL = 2 с, переходов по ссылке не было
        List<ShortLink> removed = shortTtlServer.service().purgeExpired();
        assertTrue(removed.stream().anyMatch(l -> l.getCode().equals(code)),
                "purge должен удалить протухшую ссылку");
        try (ClientSession a = connectShortTtl()) {
            auth(a);
            assertEquals(RedirectStatus.NOT_FOUND, open(a, owner, code).status());
        }
    }
}
