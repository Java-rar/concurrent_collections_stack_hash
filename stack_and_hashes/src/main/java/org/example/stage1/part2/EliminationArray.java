package org.example.stage1.part2;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class EliminationArray<T> {

    private static final int duration = 50;

    private final LockFreeExchanger<T>[] exchangers;

    @SuppressWarnings("unchecked")
    public EliminationArray(final int capacity) {
        exchangers = new LockFreeExchanger[capacity];
        for (int i = 0; i < capacity; i++) {
            exchangers[i] = new LockFreeExchanger<>();
        }
    }

    public T visit(T value, int range) throws TimeoutException {
        int slot = ThreadLocalRandom.current().nextInt(range) % exchangers.length;
        return exchangers[slot].exchange(value, duration, TimeUnit.MILLISECONDS);
    }

}
