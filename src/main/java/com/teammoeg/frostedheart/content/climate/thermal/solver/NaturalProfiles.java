/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 主线程发布、worker 在步与步之间读取的逐格自然温度。它是背景数据：晚一步生效只等于自然温度刷新晚一步。
 * <p>
 * Per-block natural temperatures published by the main thread and read by the worker between steps. This is
 * background data: taking effect one step late only delays a natural refresh by one step.
 */
public final class NaturalProfiles {
    private final ConcurrentHashMap<Long, NaturalProfile> bySection = new ConcurrentHashMap<>();
    private final AtomicLong revision = new AtomicLong();

    public void put(long section, NaturalProfile profile) {
        bySection.put(section, profile);
        revision.incrementAndGet();
    }

    public void remove(long section) {
        if (bySection.remove(section) != null) revision.incrementAndGet();
    }

    public NaturalProfile get(long section) {
        return bySection.get(section);
    }

    public long revision() {
        return revision.get();
    }

    public void clear() {
        bySection.clear();
        revision.incrementAndGet();
    }
}
