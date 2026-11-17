package se.ifmo.ru.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import se.ifmo.ru.common.dto.RedirectRequestDto;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class MessageManagerTest {

    @Test
    void roundTripMessageWithPayload() {
        UUID id = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        Message original = new Message(id, user, MessageType.REDIRECT_REQ,
                MessageManager.payload(new RedirectRequestDto("ABC123")));
        Message parsed = MessageManager.fromJson(MessageManager.toJson(original));
        assertEquals(id, parsed.id());
        assertEquals(user, parsed.userUuid());
        assertEquals(MessageType.REDIRECT_REQ, parsed.type());
        assertEquals("ABC123", MessageManager.payload(parsed, RedirectRequestDto.class).code());
    }

    @Test
    void roundTripMessageWithNullPayload() {
        Message original = new Message(UUID.randomUUID(), null, MessageType.LIST_REQ, null);
        Message parsed = MessageManager.fromJson(MessageManager.toJson(original));
        assertEquals(MessageType.LIST_REQ, parsed.type());
        assertNull(parsed.userUuid());
        assertNull(parsed.payload());
    }

    @Test
    void invalidJsonThrows() {
        assertThrows(IllegalArgumentException.class, () -> MessageManager.fromJson("{не json"));
    }

    @Test
    void unknownFieldsAreIgnored() {
        Message parsed = MessageManager.fromJson(
                "{\"id\":\"" + UUID.randomUUID() + "\",\"type\":\"LIST_REQ\",\"extra\":123}");
        assertEquals(MessageType.LIST_REQ, parsed.type());
    }
}
