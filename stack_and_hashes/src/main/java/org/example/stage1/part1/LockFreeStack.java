package org.example.stage1.part1;

import org.example.stage1.EmptyException;
import org.example.stage1.Stack;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

public class LockFreeStack<T> implements Stack<T> {

    static final int MIN_DELAY = 10;
    static final int MAX_DELAY = 250;

    private AtomicReference<Node<T>> top = new AtomicReference<>(null);
    private Backoff backoff = new Backoff(MIN_DELAY, MAX_DELAY);

    @Override
    public void push(T value) {
        Node<T> node = new Node<>(value);
        while (true) {
            if (tryPush(node)) {
                return;
            } else {
                backoff.backoff();
            }
        }
    }

    protected boolean tryPush(Node<T> node) {
        Node<T> oldTop = top.get();
        node.next = oldTop;
        return top.compareAndSet(oldTop, node);
    }

    @Override
    public T pop() {
        while (true) {
            Node<T> returnNode = tryPop();
            if (returnNode != null) {
                return returnNode.value;
            } else {
                backoff.backoff();
            }
        }
    }

    protected Node<T> tryPop() {
        Node<T> oldTop = top.get();
        if (oldTop == null) {
            throw new EmptyException();
        }
        Node<T> newTop = oldTop.next;
        if (top.compareAndSet(oldTop, newTop)) {
            return oldTop;
        }
        return null;

    }

    protected static class Node<T> {
        T value;
        Node<T> next;

        public Node(T value) {
            this.value = value;
            this.next = null;
        }
    }

    private static class Backoff {
        private final int minDelay, maxDelay;
        int limit;

        public Backoff(final int minDelay, final int maxDelay) {
            this.minDelay = minDelay;
            this.maxDelay = maxDelay;
            this.limit = minDelay;
        }

        public void backoff() {
            int delay = ThreadLocalRandom.current().nextInt(limit);
            limit = Math.min(maxDelay, 2 * limit);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                //ignore
            }

//            long waitTill = System.nanoTime() + TimeUnit.NANOSECONDS.convert(delay, TimeUnit.MILLISECONDS);
//            while (waitTill > System.nanoTime());
        }
    }


}
