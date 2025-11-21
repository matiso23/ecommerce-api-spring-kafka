package dev.williamnogueira.ecommerce.domain.common;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BaseEntityTest {

    static class TestEntity extends BaseEntity {}

    @Test
    void onCreate_shouldSetCreatedAtAndUpdatedAt() {
        TestEntity entity = new TestEntity();

        entity.onCreate();

        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
        assertEquals(entity.getCreatedAt(), entity.getUpdatedAt());
    }

    @Test
    void onUpdate_shouldUpdateUpdatedAtOnly() throws InterruptedException {
        TestEntity entity = new TestEntity();

        entity.onCreate();
        LocalDateTime created = entity.getCreatedAt();
        LocalDateTime updatedBefore = entity.getUpdatedAt();

        Thread.sleep(5);
        entity.onUpdate();

        assertEquals(created, entity.getCreatedAt());
        assertTrue(entity.getUpdatedAt().isAfter(updatedBefore));
    }
}
