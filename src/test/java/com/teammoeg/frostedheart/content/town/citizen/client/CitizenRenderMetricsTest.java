/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.citizen.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CitizenRenderMetricsTest {

	@AfterEach
	void resetMetrics() {
		CitizenRenderMetrics.reset();
	}

	@Test
	void reportsLatestCountersAverageAndNearestRankP95() {
		CitizenRenderMetrics.reset();
		for (int frame = 1; frame <= 100; frame++) {
			CitizenRenderMetrics.recordFrame(frame * 1_000_000L, frame, 64, 36,
					30, 6, 7, 4, 0L);
		}

		CitizenRenderMetrics.Snapshot snapshot = CitizenRenderMetrics.snapshot();
		assertEquals(100_000_000L, snapshot.latestFrameNanos());
		assertEquals(50_500_000L, snapshot.averageFrameNanos());
		assertEquals(95_000_000L, snapshot.p95FrameNanos());
		assertEquals(100, snapshot.sampleCount());
		assertEquals(100, snapshot.cacheCount());
		assertEquals(64, snapshot.detailedCount());
		assertEquals(36, snapshot.frustumBatchCount());
		assertEquals(30, snapshot.bodyCount());
		assertEquals(6, snapshot.billboardCount());
		assertEquals(7, snapshot.drawCalls());
		assertEquals(4, snapshot.lightSamples());
		assertEquals(0L, snapshot.instanceDirtyBytes());
		assertEquals(0L, snapshot.peakInstanceDirtyBytes());
	}

	@Test
	void retainsPeakInstanceDirtyBytesUntilReset() {
		CitizenRenderMetrics.recordFrame(1L, 0, 0, 0, 0, 0, 0, 0, 46L);
		CitizenRenderMetrics.recordFrame(2L, 0, 0, 0, 0, 0, 0, 0, 0L);

		CitizenRenderMetrics.Snapshot snapshot = CitizenRenderMetrics.snapshot();
		assertEquals(0L, snapshot.instanceDirtyBytes());
		assertEquals(46L, snapshot.peakInstanceDirtyBytes());

		CitizenRenderMetrics.reset();
		assertEquals(0L, CitizenRenderMetrics.snapshot().peakInstanceDirtyBytes());
	}

	@Test
	void keepsOnlyTheLatestTwoHundredFiftySixFrames() {
		CitizenRenderMetrics.reset();
		for (int frame = 1; frame <= 300; frame++)
			CitizenRenderMetrics.recordFrame(frame, 0, 0, 0, 0, 0, 0, 0, 0L);

		CitizenRenderMetrics.Snapshot snapshot = CitizenRenderMetrics.snapshot();
		assertEquals(256, snapshot.sampleCount());
		assertEquals(300L, snapshot.latestFrameNanos());
		assertEquals(288L, snapshot.p95FrameNanos());
	}
}
