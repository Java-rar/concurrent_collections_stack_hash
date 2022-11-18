package org.example.stage2.part3;

import org.example.stage2.BaseHashSet;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RefinableHashSet<T> extends BaseHashSet<T> {

    private AtomicMarkableReference<Thread> owner;
    volatile Lock[] locks;

    public RefinableHashSet(int capacity) {
        super(capacity);
        locks = new ReentrantLock[capacity];
        for (int i = 0; i < capacity; i++) {
            locks[i] = new ReentrantLock();
        }
        owner = new AtomicMarkableReference<>(null, false);
    }

    @Override
    public void acquire(T item) {
        boolean[] mark = {true};
        Thread me = Thread.currentThread();
        Thread who;
        while (true) {
            do{
                who = owner.get(mark);
            }while (mark[0] || who != me);

            Lock[] oldLocks = locks;
            Lock oldLock = oldLocks[item.hashCode() % oldLocks.length];
            oldLock.lock();
            who = owner.get(mark);
            if(!mark[0] || who == me && locks == oldLocks){
                return;
            }else{
                oldLock.unlock();
            }
        }
    }

    @Override
    public void release(T item) {
        locks[item.hashCode() % locks.length].unlock();
    }

    @Override
    public void resize() {
        Thread me = Thread.currentThread();
        if (owner.compareAndSet(null, me, false, true)) {
            try {
                if (!policy()) {
                    return;
                }

                quiesce();
                int newCapacity = 2 * table.length;
                List<T>[] oldTable = table;
                table = new List[newCapacity];
                for (int i = 0; i < newCapacity; i++) {
                    table[i] = new ArrayList<>();
                }
                locks = new Lock[newCapacity];
                for (int i = 0; i < newCapacity; i++) {
                    locks[i] = new ReentrantLock();
                }
                initializeFrom(oldTable);

            } finally {
                owner.set(null, false);
            }
        }

    }

    private void quiesce() {
        for (Lock lock : locks) {
            while (((ReentrantLock) lock).isLocked());
        }
    }

    private void initializeFrom(List<T>[] oldTable) {
        for (List<T> bucket : oldTable) {
            for (T item : bucket) {
                table[item.hashCode() % table.length].add(item);
            }
        }
    }

    @Override
    public boolean policy() {
        return setSize.get() / table.length > 4;
    }
}
