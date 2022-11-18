package org.example.stage3.part3;

import org.example.stage3.part1.PhasedCuckooHashSet;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RefinableCuckooHashSet<T> extends PhasedCuckooHashSet<T> {

    final AtomicMarkableReference<Thread> owner;
    volatile ReentrantLock[][] locks;

    public RefinableCuckooHashSet(int capacity) {
        super(capacity);
        locks = new ReentrantLock[2][capacity];
        owner = new AtomicMarkableReference<>(null, false);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < capacity; j++) {
                locks[i][j] = new ReentrantLock();
            }
        }
    }

    @Override
    public void acquire(T x) {
        boolean[] mark = {true};
        Thread me = Thread.currentThread();
        Thread who;
        while (true) {
            do {
                who = owner.get(mark);
            } while (mark[0] && who != me);
            Lock[][] oldLocks = locks;
            Lock oldLock0 = oldLocks[0][hash0(x) % oldLocks[0].length];
            Lock oldLock1 = oldLocks[1][hash1(x) % oldLocks[1].length];
            oldLock0.lock();
            oldLock1.lock();
            who = owner.get(mark);
            if ((!mark[0] || who == me) && locks == oldLocks) {
                return;
            } else {
                oldLock0.unlock();
                oldLock1.unlock();
            }
        }
    }

    @Override
    public void release(T x) {
        locks[0][hash0(x) % locks[0].length].unlock();
        locks[1][hash1(x) % locks[1].length].unlock();
    }

    @Override
    public void resize() {
        int oldCapacity = capacity;
        Thread me = Thread.currentThread();
        if (owner.compareAndSet(null, me, false, true)) {
            try {
                if (capacity != oldCapacity) {
                    return;
                }
                quiesce();
                List<T>[][] oldTable = table;
                capacity = capacity * 2;
                table = (List<T>[][]) new ArrayList[2][capacity];
                locks = new ReentrantLock[2][capacity];
                for (int i = 0; i < 2; i++) {
                    for (int j = 0; j < capacity; j++) {
                        table[i][j] = new ArrayList<>(PROBE_SIZE);
                        locks[i][j] = new ReentrantLock();
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
                owner.set(null, false);
            }
        }
    }

    private void quiesce() {
        for (ReentrantLock lock : locks[0]) {
            while (lock.isLocked()) ;
        }
    }
}
