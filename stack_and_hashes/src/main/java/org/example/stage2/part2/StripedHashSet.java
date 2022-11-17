package org.example.stage2.part2;

import org.example.stage2.BaseHashSet;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class StripedHashSet<T> extends BaseHashSet<T> {

    final Lock[] locks;

    public StripedHashSet(int capacity) {
        super(capacity);
        locks = new Lock[capacity];
        for (int i = 0; i < capacity; i++) {
            locks[i] = new ReentrantLock();
        }
    }

    @Override
    public final void acquire(T item) {
        locks[item.hashCode() % locks.length].lock();
    }

    @Override
    public void release(T item) {
        locks[item.hashCode() % locks.length].unlock();
    }

    @Override
    public void resize() {

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

        }
    }

    @Override
    public boolean policy() {
        return setSize.get() / table.length > 4;
    }
}
