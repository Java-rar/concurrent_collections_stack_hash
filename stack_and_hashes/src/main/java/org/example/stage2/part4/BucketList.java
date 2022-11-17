package org.example.stage2.part4;


import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Integer.reverse;

public class BucketList<T> implements Set<T> {

    static final int HI_MASK = 0x80000000;
    static final int MASK = 0x00FFFFFF;
    private final Node<T> head;

    public BucketList() {
        head = new Node<>(0);
        Node<T> tail = new Node<>(Integer.MAX_VALUE);
        head.next.set(tail, false);
    }

    public BucketList(final Node<T> head) {
        this.head = head;
    }

    public int makeOrdinaryKey(T item) {
        int code = item.hashCode() & MASK;
        return reverse(code | HI_MASK);
    }

    private static int makeSentinelKey(int key) {
        return reverse(key & MASK);
    }

    public BucketList<T> getSentinel(int index) {
        int key = makeSentinelKey(index);
        boolean splice;
        while (true) {
            Window<T> window = Window.find(head, key);
            Node<T> pred = window.pred;
            Node<T> curr = window.curr;
            if (curr.key == key) {
                return new BucketList<T>(curr);
            } else {
                Node<T> node = new Node<>(key);
                node.next.set(pred.next.getReference(), false);
                splice = pred.next.compareAndSet(curr, node, false, false);
                if (splice) {
                    return new BucketList<T>(node);
                } else {
                    continue;
                }
            }
        }
    }

    @Override
    public boolean add(T item) {
        int key = makeOrdinaryKey(item);//different hashCode
        while (true) {
            Window<T> window = Window.find(head, key);
            Node<T> pred = window.pred, curr = window.curr;
            if (curr.key == key) {
                return false;
            }
            Node<T> node = new Node<>(item, key);
            node.next = new AtomicMarkableReference<>(curr, false);
            if (pred.next.compareAndSet(curr, node, false, false)) {
                return true;
            }
        }
    }

    @Override
    public boolean remove(T item) {
        int key = makeOrdinaryKey(item);//different hashCode
        boolean snip;
        while (true) {
            Window<T> window = Window.find(head, key);
            Node<T> pred = window.pred, curr = window.curr;
            if (curr.key != key) {
                return false;
            }
            Node<T> succ = curr.next.getReference();
            snip = curr.next.compareAndSet(succ, succ, false, true);
            if (!snip) {
                continue;
            }
            pred.next.compareAndSet(curr, succ, false, false);
            return true;
        }
    }

    @Override
    public boolean contains(T item) {
        int key = makeOrdinaryKey(item); //different hashCode
        Window<T> window = Window.find(head, key);
        Node<T> curr = window.curr;
        return curr.key == key && !curr.next.isMarked();
    }


    private static class Window<T> {
        final Node<T> pred, curr;

        private Window(final Node<T> pred, final Node<T> curr) {
            this.pred = pred;
            this.curr = curr;
        }


        static <T> Window<T> find(Node<T> head, int key) {
            Node<T> pred, curr, succ;
            boolean[] marked = new boolean[1];
            boolean snip;


            retry:
            while (true) {
                pred = head;
                curr = pred.next.getReference();
                while (true) {
                    succ = curr.next.get(marked);
                    while (marked[0]) {
                        snip = pred.next.compareAndSet(curr, succ, false, false);
                        if (!snip) {
                            continue retry;
                        }
                        curr = succ;
                        succ = curr.next.get(marked);
                    }
                    if (curr.key >= key) {
                        return new Window<>(pred, curr);
                    }
                    pred = curr;
                    curr = succ;
                }
            }
        }
    }

    private static class Node<T> {
        T item;
        final int key;

        AtomicMarkableReference<Node<T>> next;

        public Node(final int key) {
            this.key = key;
            this.next = new AtomicMarkableReference<>(null, false);
        }

        public Node(final T item, final int key) {
            this.item = item;
            this.key = key;
        }

    }

    public static int hashCode(Object obj) {
        return obj.hashCode();
    }
}

