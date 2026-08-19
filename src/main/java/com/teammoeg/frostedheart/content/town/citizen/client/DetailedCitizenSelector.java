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

/**
 * Allocation-stable Top-K selector for vanilla-quality client citizen proxies.
 * Lower ranks are better; the heap root is always the worst selected candidate.
 */
final class DetailedCitizenSelector {

	static final double ENTER_DIST2 = 16.0 * 16.0;
	static final double EXIT_DIST2 = 20.0 * 20.0;
	private static final int DISTANCE_QUANTIZATION = 16;
	private static final int RETAIN_ADVANTAGE_Q = 4 * DISTANCE_QUANTIZATION;
	private static final long CROSSHAIR_RANK_BASE = Long.MIN_VALUE / 2;

	private int limit;
	private int candidateCount;
	private int[] candidateIds = new int[0];
	private double[] candidateDistances2 = new double[0];
	private boolean[] candidateRetained = new boolean[0];
	private int selectedCount;
	private int[] selectedIds = new int[0];
	private long[] selectedRanks = new long[0];

	void reset(int limit) {
		this.limit = Math.max(0, limit);
		candidateCount = 0;
		selectedCount = 0;
		ensureSelectedCapacity(this.limit);
	}

	static boolean isEligible(double distance2, boolean retained) {
		return distance2 <= (retained ? EXIT_DIST2 : ENTER_DIST2);
	}

	void addCandidate(int id, double distance2, boolean retained) {
		ensureCandidateCapacity(candidateCount + 1);
		candidateIds[candidateCount] = id;
		candidateDistances2[candidateCount] = distance2;
		candidateRetained[candidateCount] = retained;
		candidateCount++;
	}

	void select(int crosshairId, int interactingId) {
		selectedCount = 0;
		if (limit == 0)
			return;
		for (int i = 0; i < candidateCount; i++) {
			int id = candidateIds[i];
			int priority = id == interactingId ? 2 : id == crosshairId ? 1 : 0;
			offer(id, rank(id, candidateDistances2[i], candidateRetained[i], priority));
		}
	}

	int selectedCount() {
		return selectedCount;
	}

	int selectedIdAt(int index) {
		return selectedIds[index];
	}

	private static long rank(int id, double distance2, boolean retained, int priority) {
		long unsignedId = id & 0xFFFFFFFFL;
		if (priority == 2)
			return Long.MIN_VALUE + unsignedId;
		if (priority == 1)
			return CROSSHAIR_RANK_BASE + unsignedId;
		int distanceQ = (int) Math.min(Integer.MAX_VALUE,
				Math.sqrt(Math.max(0.0, distance2)) * DISTANCE_QUANTIZATION);
		if (retained)
			distanceQ = Math.max(0, distanceQ - RETAIN_ADVANTAGE_Q);
		return ((long) distanceQ << 32) | unsignedId;
	}

	private void offer(int id, long rank) {
		if (selectedCount < limit) {
			int child = selectedCount++;
			while (child > 0) {
				int parent = (child - 1) >>> 1;
				if (rank <= selectedRanks[parent])
					break;
				selectedIds[child] = selectedIds[parent];
				selectedRanks[child] = selectedRanks[parent];
				child = parent;
			}
			selectedIds[child] = id;
			selectedRanks[child] = rank;
			return;
		}
		if (rank >= selectedRanks[0])
			return;
		int parent = 0;
		while (true) {
			int left = parent * 2 + 1;
			if (left >= selectedCount)
				break;
			int right = left + 1;
			int worse = right < selectedCount && selectedRanks[right] > selectedRanks[left] ? right : left;
			if (selectedRanks[worse] <= rank)
				break;
			selectedIds[parent] = selectedIds[worse];
			selectedRanks[parent] = selectedRanks[worse];
			parent = worse;
		}
		selectedIds[parent] = id;
		selectedRanks[parent] = rank;
	}

	private void ensureCandidateCapacity(int capacity) {
		if (candidateIds.length >= capacity)
			return;
		int newCapacity = Math.max(capacity, Math.max(16, candidateIds.length * 2));
		candidateIds = java.util.Arrays.copyOf(candidateIds, newCapacity);
		candidateDistances2 = java.util.Arrays.copyOf(candidateDistances2, newCapacity);
		candidateRetained = java.util.Arrays.copyOf(candidateRetained, newCapacity);
	}

	private void ensureSelectedCapacity(int capacity) {
		if (selectedIds.length >= capacity)
			return;
		selectedIds = java.util.Arrays.copyOf(selectedIds, capacity);
		selectedRanks = java.util.Arrays.copyOf(selectedRanks, capacity);
	}
}
