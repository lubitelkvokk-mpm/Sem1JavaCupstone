package se.ifmo.ru.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ShortCodeGeneratorTest {

    @Test
    void generatesCodeOfRequestedLength() {
        ShortCodeGenerator generator = new ShortCodeGenerator(8);
        String code = generator.generateUnique(c -> false);
        assertEquals(8, code.length());
    }

    @Test
    void usesOnlyBase62Alphabet() {
        ShortCodeGenerator generator = new ShortCodeGenerator(6);
        for (int i = 0; i < 200; i++) {
            String code = generator.generateUnique(c -> false);
            assertTrue(code.matches("[A-Za-z0-9]{6}"), "Недопустимые символы: " + code);
        }
    }

    @Test
    void generatesUniqueCodesUnderLoad() {
        ShortCodeGenerator generator = new ShortCodeGenerator(6);
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 5000; i++) {
            assertTrue(seen.add(generator.generateUnique(seen::contains)),
                    "Коллизия кодов на итерации " + i);
        }
    }

    @Test
    void retriesOnCollision() {
        ShortCodeGenerator generator = new ShortCodeGenerator(4);
        Set<String> taken = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            taken.add(generator.generateUnique(taken::contains));
        }
        assertEquals(100, taken.size());
    }

    @Test
    void rejectsTooShortLength() {
        assertThrows(IllegalArgumentException.class, () -> new ShortCodeGenerator(2));
    }

    @Test
    void differentCallsProduceDifferentCodes() {
        ShortCodeGenerator generator = new ShortCodeGenerator(6);
        assertNotEquals(generator.generateUnique(c -> false),
                generator.generateUnique(c -> false));
    }
}
