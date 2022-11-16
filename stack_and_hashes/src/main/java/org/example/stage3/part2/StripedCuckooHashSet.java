package org.example.stage3.part2;

import org.example.stage3.part1.PhasedCuckooHashSet;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class StripedCuckooHashSet<T> extends PhasedCuckooHashSet<T> {

    final Lock[][] locks;

    public StripedCuckooHashSet(int capacity) {
        super(capacity);
        locks = new ReentrantLock[2][capacity];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < capacity; j++) {
                locks[i][j] = new ReentrantLock();
            }
        }
    }

    @Override
    public void acquire(T x) {
        locks[0][hash0(x) % locks[0].length].lock();
        locks[1][hash1(x) % locks[1].length].lock();
    }

    @Override
    public void release(T x) {
        locks[0][hash0(x) % locks[0].length].unlock();
        locks[1][hash1(x) % locks[1].length].unlock();
    }

    @Override
    public void resize() {
        int oldCapacity = capacity;
        for (Lock lock : locks[0]) {
            lock.lock();
        }
        try {
            if (capacity != oldCapacity) {
                return;
            }
            List<T>[][] oldTable = table;
            capacity = capacity * 2;
            table = (List<T>[][]) new ArrayList[2][capacity];
            for (List<T>[] row : table) {
                for (int i = 0; i < row.length; i++) {
                    row[i] = new ArrayList<>(PROBE_SIZE);
                }
            }
            for (List<T>[] row : oldTable) {
                for (List<T> set : row) {
                    for (T item : set) {
                        add(item);
                    }
                }
            }
        } finally {
            for (Lock lock : locks[0]) {
                lock.unlock();
            }
        }
    }
}
