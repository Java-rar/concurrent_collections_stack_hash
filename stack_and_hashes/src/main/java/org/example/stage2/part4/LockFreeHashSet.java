package org.example.stage2.part4;

import org.example.stage2.HashSet;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class LockFreeHashSet<T> implements HashSet<T> {

    private static int THRESHOLD = 4;

    protected BucketList<T>[] buckets;
    protected AtomicInteger bucketSize;
    protected AtomicInteger setSize;

    public LockFreeHashSet(int capacity) {
        buckets = new BucketList[capacity];
        buckets[0] = new BucketList<>();
        bucketSize = new AtomicInteger(2);
        setSize = new AtomicInteger(0);
    }

    @Override
    public boolean contains(T item) {
        int myBucket = BucketList.hashCode(item) % bucketSize.get();
        BucketList<T> bucket = getBucketList(myBucket);
        return bucket.contains(item);
    }

    @Override
    public boolean add(T item) {
        int myBucket = BucketList.hashCode(item) % bucketSize.get();
        BucketList<T> bucket = getBucketList(myBucket);
        if (!bucket.add(item)) {
            return false;
        }
        int setSizeNew = setSize.getAndIncrement();
        int bucketSizeNow = bucketSize.get();
        if (setSizeNew / bucketSizeNow > THRESHOLD && 2 * bucketSizeNow < buckets.length) {
            bucketSize.compareAndSet(bucketSizeNow, 2 * bucketSizeNow);
        }
        return true;
    }

    private BucketList<T> getBucketList(int myBucket) {
        if (buckets[myBucket] == null) {
            initializebucket(myBucket);
        }

        return buckets[myBucket];
    }

    private void initializebucket(int myBucket) {
        int parent = getParent(myBucket);
        if (buckets[parent] == null) {
            initializebucket(parent);
        }
        BucketList<T> bucket = buckets[parent].getSentinel(myBucket);
        if (bucket != null) {
            buckets[myBucket] = bucket;
        }
    }

    private int getParent(int myBucket) {
        int parent = bucketSize.get();
        do {
            parent = parent >> 1;
        } while (parent > myBucket);
        parent = myBucket - parent;
        return parent;
    }
}
