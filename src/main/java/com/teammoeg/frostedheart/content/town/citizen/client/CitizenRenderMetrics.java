/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.town.citizen.client;

import java.util.Arrays;

/** Allocation-free frame recording for citizen render diagnostics. */
final class CitizenRenderMetrics {

	private static final int HISTORY_SIZE = 256;
	private static final long[] FRAME_NANOS = new long[HISTORY_SIZE];

	private static int historyCount;
	private static int historyCursor;
	private static long latestFrameNanos;
	private static int cacheCount;
	private static int detailedCount;
	private static int frustumBatchCount;
	private static int bodyCount;
	private static int billboardCount;
	private static int drawCalls;
	private static int lightSamples;
	private static long instanceDirtyBytes;
	private static long peakInstanceDirtyBytes;

	private CitizenRenderMetrics() {
	}

	static void recordFrame(long frameNanos, int cache, int detailed, int frustumBatch,
			int bodies, int billboards, int draws, int sampledLights, long dirtyBytes) {
		latestFrameNanos = Math.max(0L, frameNanos);
		cacheCount = cache;
		detailedCount = detailed;
		frustumBatchCount = frustumBatch;
		bodyCount = bodies;
		billboardCount = billboards;
		drawCalls = draws;
		lightSamples = sampledLights;
		instanceDirtyBytes = Math.max(0L, dirtyBytes);
		peakInstanceDirtyBytes = Math.max(peakInstanceDirtyBytes, instanceDirtyBytes);
		FRAME_NANOS[historyCursor] = latestFrameNanos;
		historyCursor = (historyCursor + 1) % HISTORY_SIZE;
		if (historyCount < HISTORY_SIZE)
			historyCount++;
	}

	static Snapshot snapshot() {
		long[] sorted = Arrays.copyOf(FRAME_NANOS, historyCount);
		Arrays.sort(sorted);
		long p95 = percentile95(sorted);
		long sum = 0L;
		for (long value : sorted)
			sum += value;
		long average = sorted.length == 0 ? 0L : sum / sorted.length;
		return new Snapshot(latestFrameNanos, average, p95, historyCount, cacheCount, detailedCount,
				frustumBatchCount, bodyCount, billboardCount, drawCalls, lightSamples, instanceDirtyBytes,
				peakInstanceDirtyBytes);
	}

	static void reset() {
		Arrays.fill(FRAME_NANOS, 0L);
		historyCount = 0;
		historyCursor = 0;
		latestFrameNanos = 0L;
		cacheCount = 0;
		detailedCount = 0;
		frustumBatchCount = 0;
		bodyCount = 0;
		billboardCount = 0;
		drawCalls = 0;
		lightSamples = 0;
		instanceDirtyBytes = 0L;
		peakInstanceDirtyBytes = 0L;
	}

	private static long percentile95(long[] sorted) {
		if (sorted.length == 0)
			return 0L;
		int index = Math.max(0, (int) Math.ceil(sorted.length * 0.95) - 1);
		return sorted[index];
	}

	record Snapshot(long latestFrameNanos, long averageFrameNanos, long p95FrameNanos, int sampleCount,
			int cacheCount, int detailedCount, int frustumBatchCount, int bodyCount, int billboardCount,
			int drawCalls, int lightSamples, long instanceDirtyBytes, long peakInstanceDirtyBytes) {
	}
}
