package se.ifmo.ru.infra;

import se.ifmo.ru.core.ShortLink;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Временное (in-memory) хранилище ссылок.
 */
public class InMemoryLinkRepository implements LinkRepository {

    private final Map<String, ShortLink> links = new ConcurrentHashMap<>();
    private final Set<UUID> users = ConcurrentHashMap.newKeySet();

    @Override
    public void save(ShortLink link) {
        links.put(link.getCode(), link);
    }

    @Override
    public Optional<ShortLink> findByCode(String code) {
        return Optional.ofNullable(links.get(code));
    }

    @Override
    public List<ShortLink> findByOwner(UUID owner) {
        List<ShortLink> result = new ArrayList<>();
        for (ShortLink link : links.values()) {
            if (link.getOwner().equals(owner)) {
                result.add(link);
            }
        }
        return result;
    }

    @Override
    public boolean delete(String code) {
        return links.remove(code) != null;
    }

    @Override
    public boolean exists(String code) {
        return links.containsKey(code);
    }

    @Override
    public List<ShortLink> purgeExpired(Instant now) {
        List<ShortLink> expired = new ArrayList<>();
        links.entrySet().removeIf(entry -> {
            if (entry.getValue().isExpired(now)) {
                expired.add(entry.getValue());
                return true;
            }
            return false;
        });
        return expired;
    }

    @Override
    public void registerUser(UUID user) {
        users.add(user);
    }

    @Override
    public boolean isUser(UUID user) {
        return users.contains(user);
    }
}
