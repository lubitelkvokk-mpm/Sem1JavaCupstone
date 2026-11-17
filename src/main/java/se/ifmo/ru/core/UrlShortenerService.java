package se.ifmo.ru.core;

import se.ifmo.ru.infra.AppConfig;
import se.ifmo.ru.infra.LinkRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Основная бизнес-логика сервиса сокращения ссылок.
 */
public class UrlShortenerService {

    private final LinkRepository repository;
    private final NotificationService notifications;
    private final ShortCodeGenerator codeGenerator;
    private final AppConfig config;

    public UrlShortenerService(LinkRepository repository,
                               NotificationService notifications,
                               ShortCodeGenerator codeGenerator,
                               AppConfig config) {
        this.repository = repository;
        this.notifications = notifications;
        this.codeGenerator = codeGenerator;
        this.config = config;
    }

    public NotificationService notifications() {
        return notifications;
    }

    public AppConfig config() {
        return config;
    }

    /**
     * Аутентификация: если UUID уже известен системе — возвращаем его,
     * иначе генерируем новый (первый запрос пользователя).
     */
    public UUID authenticate(UUID presented) {
        if (presented != null && repository.isUser(presented)) {
            return presented;
        }
        UUID id = UUID.randomUUID();
        repository.registerUser(id);
        return id;
    }

    /**
     * Создаёт короткую ссылку. Код всегда уникален в системе, поэтому
     * разные пользователи получают разные короткие ссылки даже на один URL.
     *
     * @throws InvalidUrlException     если URL невалиден
     * @throws IllegalArgumentException если лимит переходов вне допустимых границ
     */
    public ShortLink createLink(UUID user, String url, Integer maxVisits) {
        UrlValidator.validate(url);
        int limit = normalizeLimit(maxVisits);
        String code = codeGenerator.generateUnique(repository::exists);
        Instant now = Instant.now();
        ShortLink link = new ShortLink(code, user, url.trim(), limit, now,
                now.plusSeconds(config.ttlSeconds()));
        repository.save(link);
        return link;
    }

    /**
     * Переход по короткой ссылке: проверяет срок жизни и лимит,
     * атомарно учитывает переход.
     */
    public ResolveResult resolve(String code) {
        if (code == null || code.isBlank()) {
            return ResolveResult.notFound();
        }
        Instant now = Instant.now();
        ShortLink link = repository.findByCode(code.trim()).orElse(null);
        if (link == null) {
            return ResolveResult.notFound();
        }
        if (link.isExpired(now)) {
            repository.delete(link.getCode());
            notifications.notify(link.getOwner(),
                    "Ссылка " + shortUrl(link) + " истекла и была удалена.");
            return ResolveResult.expired();
        }
        if (link.tryIncrementVisit()) {
            int remaining = link.getMaxVisits() - link.getVisitCount();
            if (remaining == 0) {
                notifications.notify(link.getOwner(),
                        "Лимит переходов по ссылке " + shortUrl(link)
                                + " исчерпан, ссылка заблокирована.");
            }
            return ResolveResult.ok(link.getUrl(), remaining);
        }
        return ResolveResult.limitExceeded();
    }

    public List<ShortLink> listLinks(UUID user) {
        return repository.findByOwner(user);
    }

    /**
     * Удаление ссылки. Доступно только владельцу.
     *
     * @return true, если ссылка удалена
     */
    public boolean deleteLink(UUID user, String code) {
        ShortLink link = repository.findByCode(code).orElse(null);
        if (link == null || !link.getOwner().equals(user)) {
            return false;
        }
        return repository.delete(code);
    }

    /**
     * Изменение лимита переходов. Доступно только владельцу.
     *
     * @return обновлённая ссылка или empty, если ссылка не найдена
     * или принадлежит другому пользователю
     */
    public Optional<ShortLink> updateLink(UUID user, String code, Integer maxVisits) {
        ShortLink link = repository.findByCode(code).orElse(null);
        if (link == null || !link.getOwner().equals(user)) {
            return Optional.empty();
        }
        int limit = normalizeLimit(maxVisits);
        ShortLink updated = new ShortLink(link.getCode(), link.getOwner(), link.getUrl(),
                limit, link.getVisitCount(), link.getCreatedAt(), link.getExpiresAt());
        repository.save(updated);
        return Optional.of(updated);
    }

    /**
     * Автоудаление протухших ссылок (вызывается фоновым планировщиком).
     *
     * @return удалённые ссылки
     */
    public List<ShortLink> purgeExpired() {
        List<ShortLink> removed = repository.purgeExpired(Instant.now());
        removed.forEach(link -> notifications.notify(link.getOwner(),
                "Ссылка " + shortUrl(link) + " истекла и была удалена."));
        return removed;
    }

    public String shortUrl(ShortLink link) {
        return config.shortUrlPrefix() + link.getCode();
    }

    private int normalizeLimit(Integer maxVisits) {
        if (maxVisits == null) {
            return config.defaultMaxVisits();
        }
        if (maxVisits < 1) {
            throw new IllegalArgumentException("Лимит переходов должен быть не меньше 1");
        }
        if (maxVisits > config.maxVisitsLimit()) {
            throw new IllegalArgumentException(
                    "Лимит переходов не может превышать " + config.maxVisitsLimit());
        }
        return maxVisits;
    }
}
