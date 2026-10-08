package com.diamante.order.ratelimit;

import java.util.ArrayDeque;
import java.util.Deque;

public class SlidingWindowRateLimiter {

    private final int maxRequests;
    private final long windowNanos;
    private final Deque<Long> timestamps = new ArrayDeque<>();

    public SlidingWindowRateLimiter(int maxRequests, long windowMillis) {
        this.maxRequests = maxRequests;
        this.windowNanos = windowMillis * 1_000_000L;
    }

    public synchronized boolean tryAcquire() {
        return tryAcquire(System.nanoTime());
    }

    public synchronized boolean tryAcquire(long nowNanos) {
        while (!timestamps.isEmpty() && nowNanos - timestamps.peekFirst() >= windowNanos) {
            timestamps.pollFirst();
        }
        if (timestamps.size() >= maxRequests) {
            return false;
        }
        timestamps.addLast(nowNanos);
        return true;
    }
}
