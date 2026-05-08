package com.beaglescheduler.cmpe172project.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class BookingMetricsService {

    private final AtomicLong totalBookings = new AtomicLong(0);
    private final AtomicLong failedBookings = new AtomicLong(0);
    private final ConcurrentLinkedQueue<Long> recentLatenciesMs = new ConcurrentLinkedQueue<>();

    private static final int MAX_LATENCY_SAMPLES = 100;

    public void recordSuccess(long latencyMs) {
        totalBookings.incrementAndGet();
        addLatency(latencyMs);
    }

    public void recordFailure() {
        failedBookings.incrementAndGet();
    }

    private void addLatency(long latencyMs) {
        recentLatenciesMs.offer(latencyMs);
        // Keep queue bounded at MAX_LATENCY_SAMPLES
        while (recentLatenciesMs.size() > MAX_LATENCY_SAMPLES) {
            recentLatenciesMs.poll();
        }
    }

    public long getTotalBookings() {
        return totalBookings.get();
    }

    public long getFailedBookings() {
        return failedBookings.get();
    }

    public double getAvgBookingLatencyMs() {
        Long[] samples = recentLatenciesMs.toArray(new Long[0]);
        if (samples.length == 0) return 0.0;
        long sum = 0;
        for (long s : samples) sum += s;
        return (double) sum / samples.length;
    }
}
