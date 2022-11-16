package org.example.stage2.part1;

import org.example.stage2.BaseHashSet;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CoarseHashSet<T> extends BaseHashSet<T> {

    final Lock lock;

    public CoarseHashSet(int capacity) {
        super(capacity);
        this.lock = new ReentrantLock();
    }

    @Override
    public void acquire(T item) {
        lock.lock();
    }

    @Override
    public void release(T item) {
        lock.unlock();
    }

    @Override
    public void resize() {
        lock.lock();
        try {
            if (!policy()) {
                return;
            }
            int newCapacity = 2 * table.length;
            List<T>[] oldTable = table;
            table = new List[newCapacity];
            for (int i = 0; i < newCapacity; i++) {
                table[i] = new ArrayList<>();
            }
            for (List<T> bucket : oldTable) {
                for (T item : bucket) {
                    table[item.hashCode() % newCapacity].add(item);
                }
            }

        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean policy() {
        return setSize.get() / table.length > 4;
    }
}
