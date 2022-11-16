package org.example.stage2.part4;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LockFreeHashSetTest {

    @Test
    void add() {

        int capacity = 2 << 16;
        LockFreeHashSet<Integer> set = new LockFreeHashSet<>(capacity);
        int times = 1000;
        for (int i = 1; i < times; i++) {
            assertFalse(set.contains(i));
            assertTrue(set.add(i));
            assertTrue(set.contains(i), "I in set "+i);
            assertFalse(set.add(i));
        }

        for (int i = 1; i < times; i++) {
            assertTrue(set.contains(i));
        }

    }
}