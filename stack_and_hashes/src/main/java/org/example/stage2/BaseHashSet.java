package org.example.stage2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class BaseHashSet<T> implements HashSet<T> {

    protected volatile List<T>[] table;

    protected AtomicInteger setSize = new AtomicInteger(0);

    public BaseHashSet(int capacity) {
        table = new List[capacity];
        for (int i = 0; i < capacity; i++) {
            table[i] = new ArrayList<>();
        }
    }

    public abstract void acquire(T item);

    public abstract void release(T item);

    public abstract void resize();

    public abstract boolean policy();

    @Override
    public boolean contains(T item) {
        acquire(item);
        try {
            int myBucket = item.hashCode() % table.length;
            return table[myBucket].contains(item);
        } finally {
            release(item);
        }
    }

    @Override
    public boolean add(T item) {
        boolean result = false;
        acquire(item);
        try {
            int myBucket = item.hashCode() % table.length;
            if (!table[myBucket].contains(item)) {
                table[myBucket].add(item);
                result = true;
                setSize.getAndIncrement();
            }
        } finally {
            release(item);
        }
        if (policy()) {
            resize();
        }
        return result;
    }

}
