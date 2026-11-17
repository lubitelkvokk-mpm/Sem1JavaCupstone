package se.ifmo.ru.infra;

import se.ifmo.ru.core.ShortLink;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт хранения коротких ссылок и пользователей.
 */
public interface LinkRepository {

    void save(ShortLink link);

    Optional<ShortLink> findByCode(String code);

    List<ShortLink> findByOwner(UUID owner);

    boolean delete(String code);

    boolean exists(String code);

    /**
     * Удаляет все ссылки, срок жизни которых истёк.
     *
     * @return удалённые ссылки
     */
    List<ShortLink> purgeExpired(Instant now);

    void registerUser(UUID user);

    boolean isUser(UUID user);
}
