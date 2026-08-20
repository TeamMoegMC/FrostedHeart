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

import java.util.Locale;

import org.joml.Vector3f;

import com.teammoeg.frostedheart.content.town.citizen.sim.CitizenState;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;

/** Client-only deterministic crowd used to compare citizen render backends. */
final class CitizenClientBenchmark {

	private static final int FIRST_BENCHMARK_ID = Integer.MAX_VALUE - 8192;
	private static final ClientCitizen[] EMPTY_CITIZENS = new ClientCitizen[0];
	private static final int[] EMPTY_POSITIONS = new int[0];

	private static ClientCitizen[] citizens = EMPTY_CITIZENS;
	private static int[] basePx = EMPTY_POSITIONS;
	private static int[] basePy = EMPTY_POSITIONS;
	private static int[] basePz = EMPTY_POSITIONS;
	private static ClientLevel level;
	private static Mode mode;
	private static int moveDir;
	private static int moveDx;
	private static int moveDz;
	private static long lastUpdateTick = Long.MIN_VALUE;

	private CitizenClientBenchmark() {
	}

	static int load(Minecraft minecraft, int count, Mode requestedMode) {
		if (!CitizenBenchmarkLayout.isSupportedCount(count))
			throw new IllegalArgumentException("count must be one of 32, 64, 256, or 1024");
		if (minecraft.level == null || minecraft.player == null)
			throw new IllegalStateException("join a world before loading the citizen benchmark");

		clear();
		level = minecraft.level;
		mode = requestedMode;
		citizens = new ClientCitizen[count];
		basePx = new int[count];
		basePy = new int[count];
		basePz = new int[count];

		Vector3f cameraLook = minecraft.gameRenderer.getMainCamera().getLookVector();
		double lookX = cameraLook.x;
		double lookZ = cameraLook.z;
		double horizontalLength = Math.sqrt(lookX * lookX + lookZ * lookZ);
		double forwardX = horizontalLength > 1.0e-4 ? lookX / horizontalLength : 0.0;
		double forwardZ = horizontalLength > 1.0e-4 ? lookZ / horizontalLength : 1.0;
		double rightX = forwardZ;
		double rightZ = -forwardX;
		moveDir = CitizenState.dirFromVector((int) Math.round(rightX * CitizenState.FIXED_SCALE),
				(int) Math.round(rightZ * CitizenState.FIXED_SCALE));
		moveDx = CitizenState.DIR_X_16[moveDir];
		moveDz = CitizenState.DIR_Z_16[moveDir];

		int playerPx = Mth.floor(minecraft.player.getX() * CitizenState.FIXED_SCALE);
		int playerPy = Mth.floor(minecraft.player.getY() * CitizenState.FIXED_SCALE);
		int playerPz = Mth.floor(minecraft.player.getZ() * CitizenState.FIXED_SCALE);
		long tick = level.getGameTime();
		int nextId = FIRST_BENCHMARK_ID;
		int installed = 0;
		for (int i = 0; i < count; i++) {
			while (ClientCitizenCache.get(nextId) != null)
				nextId--;
			int lateral = CitizenBenchmarkLayout.lateralOffsetFixed(i, count);
			int forward = CitizenBenchmarkLayout.forwardOffsetFixed(i, count);
			basePx[i] = playerPx + scale(forwardX, forward) + scale(rightX, lateral);
			basePy[i] = playerPy;
			basePz[i] = playerPz + scale(forwardZ, forward) + scale(rightZ, lateral);
			int dir = requestedMode == Mode.SLEEPING ? (i & 3) * 4
					: movementDirection(tick, i);
			int px = basePx[i];
			int pz = basePz[i];
			if (requestedMode == Mode.MOVING) {
				int movement = CitizenBenchmarkLayout.movementOffsetFixed(tick, i);
				px += moveDx * movement / CitizenState.FIXED_SCALE;
				pz += moveDz * movement / CitizenState.FIXED_SCALE;
			}
			byte stateDir = CitizenState.packStateDir(
					requestedMode == Mode.SLEEPING ? CitizenState.SLEEP : CitizenState.WANDER, dir);
			ClientCitizen citizen = new ClientCitizen(nextId--, px, basePy[i], pz, stateDir,
					"Benchmark " + (i + 1));
			if (!CitizenRenderCoordinator.installBenchmarkCitizen(citizen))
				throw new IllegalStateException("benchmark citizen id collision");
			citizens[i] = citizen;
			installed++;
		}
		lastUpdateTick = tick;
		return installed;
	}

	static void tick(Minecraft minecraft) {
		if (mode == null)
			return;
		if (minecraft.level == null || minecraft.level != level) {
			clear();
			return;
		}
		long tick = level.getGameTime();
		if (mode != Mode.MOVING || tick == lastUpdateTick || (tick & 3L) != 0)
			return;
		lastUpdateTick = tick;
		for (int i = 0; i < citizens.length; i++) {
			ClientCitizen citizen = citizens[i];
			if (citizen == null || ClientCitizenCache.get(citizen.id) != citizen)
				continue;
			int movement = CitizenBenchmarkLayout.movementOffsetFixed(tick, i);
			int px = basePx[i] + moveDx * movement / CitizenState.FIXED_SCALE;
			int pz = basePz[i] + moveDz * movement / CitizenState.FIXED_SCALE;
			citizen.update(px, basePy[i], pz,
					CitizenState.packStateDir(CitizenState.WANDER, movementDirection(tick, i)));
			CitizenRenderCoordinator.updateBenchmarkCitizen(citizen);
		}
	}

	static void clear() {
		for (ClientCitizen citizen : citizens) {
			if (citizen != null)
				CitizenRenderCoordinator.removeBenchmarkCitizen(citizen);
		}
		citizens = EMPTY_CITIZENS;
		basePx = EMPTY_POSITIONS;
		basePy = EMPTY_POSITIONS;
		basePz = EMPTY_POSITIONS;
		level = null;
		mode = null;
		lastUpdateTick = Long.MIN_VALUE;
	}

	static int activeCount() {
		return citizens.length;
	}

	static String status() {
		return mode == null ? "off" : mode.serializedName + ":" + citizens.length;
	}

	private static int movementDirection(long tick, int index) {
		return CitizenBenchmarkLayout.movingPositive(tick, index) ? moveDir : (moveDir + 8) & 15;
	}

	private static int scale(double unit, int fixedDistance) {
		return (int) Math.round(unit * fixedDistance);
	}

	enum Mode {
		MOVING("moving"),
		SLEEPING("sleeping");

		private final String serializedName;

		Mode(String serializedName) {
			this.serializedName = serializedName;
		}

		static Mode parse(String value) {
			String normalized = value.toLowerCase(Locale.ROOT);
			for (Mode candidate : values()) {
				if (candidate.serializedName.equals(normalized))
					return candidate;
			}
			throw new IllegalArgumentException("mode must be moving or sleeping");
		}
	}
}
