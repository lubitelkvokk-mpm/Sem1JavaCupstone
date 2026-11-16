package se.ifmo.ru.common.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UrlUserStorage {
    private static final Map<UUID, Map<String, Integer>> clientStorage = new HashMap<>();

    public static UUID createUser(){
        UUID id =  UUID.randomUUID();
        clientStorage.put(id, new HashMap<>());
        return id;
    }

    public static Map<String, Integer> getUrlsCounterByUser(UUID id){
        return clientStorage.get(id);
    }
}
