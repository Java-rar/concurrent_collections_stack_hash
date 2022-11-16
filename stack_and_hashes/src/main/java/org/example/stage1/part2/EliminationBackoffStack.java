package org.example.stage1.part2;


import org.example.stage1.part1.LockFreeStack;

import java.util.concurrent.TimeoutException;

public class EliminationBackoffStack<T> extends LockFreeStack<T> {

    static final int capacity = 10;

    EliminationArray<T> eliminationArray = new EliminationArray<>(capacity);

    static ThreadLocal<RangePolicy> policy = ThreadLocal.withInitial(() -> new RangePolicy(capacity));


    @Override
    public void push(T value) {
        RangePolicy rangePolicy = policy.get();
        Node<T> node = new Node<>(value);
        while (true) {
            if (tryPush(node)) {
                return;
            } else try {
                T otherValue = eliminationArray.visit(value, rangePolicy.getRange());
                if (otherValue == null) {
                    rangePolicy.recordEliminationSuccess();
                    return;
                }
            } catch (TimeoutException ex) {
                rangePolicy.recordEliminationTimeout();
            }
        }
    }

    private static class RangePolicy {
        int maxRange;
        int currentRange = 1;

        RangePolicy(int maxRange) {
            this.maxRange = maxRange;
        }

        public void recordEliminationSuccess() {
            if (currentRange < maxRange)
                currentRange++;
        }

        public void recordEliminationTimeout() {
            if (currentRange > 1)
                currentRange--;
        }

        public int getRange() {
            return currentRange;
        }
    }
}
