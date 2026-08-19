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

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.town.citizen.sim.CitizenState;
import com.teammoeg.frostedheart.content.town.citizen.sync.S2CCitizenBatchPacket;
import com.teammoeg.frostedheart.content.town.citizen.sync.S2CCitizenSpawnPacket;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Single owner for citizen cache, proxy, backend, and client-world lifecycle transitions. */
@Mod.EventBusSubscriber(modid = FHMain.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CitizenRenderCoordinator {

	private static final Logger LOGGER = LogManager.getLogger("FrostedHeart/CitizenRender");
	private static CitizenRenderBackend backend = new CpuBatchCitizenBackend();
	private static BackendPreference requestedBackend = BackendPreference.CPU_BATCH;
	private static Supplier<CitizenRenderBackend> flywheelBackendFactory = FlywheelCitizenBackend::new;
	private static ClientLevel lastClientLevel;
	/** Client-only batch LOD state shared by CPU and M3; never synchronized. */
	private static final Int2ObjectOpenHashMap<CitizenRenderOwner> BATCH_OWNERS = new Int2ObjectOpenHashMap<>();

	private enum BackendPreference {
		CPU_BATCH("cpu_batch"),
		FLYWHEEL_M3("flywheel_m3_instancing");

		private final String backendName;

		BackendPreference(String backendName) {
			this.backendName = backendName;
		}
	}

	private CitizenRenderCoordinator() {
	}

	@SubscribeEvent
	public static void registerReloadListener(RegisterClientReloadListenersEvent event) {
		event.registerReloadListener((ResourceManagerReloadListener) resourceManager -> onResourceReload());
	}

	public static void applySpawn(List<S2CCitizenSpawnPacket.Entry> entries) {
		ClientCitizenCache.applySpawn(entries);
		for (S2CCitizenSpawnPacket.Entry entry : entries) {
			ClientCitizen citizen = ClientCitizenCache.get(entry.id());
			if (citizen != null)
				notifyAdded(citizen);
		}
	}

	public static void applyBatch(List<S2CCitizenBatchPacket.Group> groups) {
		ClientCitizenCache.applyBatch(groups);
		for (S2CCitizenBatchPacket.Group group : groups) {
			for (S2CCitizenBatchPacket.Entry entry : group.entries()) {
				ClientCitizen citizen = ClientCitizenCache.get(entry.id());
				if (citizen != null)
					notifyUpdated(citizen);
			}
		}
	}

	public static void applyDespawn(IntList ids) {
		for (int i = 0; i < ids.size(); i++) {
			int citizenId = ids.getInt(i);
			BATCH_OWNERS.remove(citizenId);
			notifyRemoved(citizenId);
			FakeCitizenManager.remove(citizenId);
		}
		ClientCitizenCache.applyDespawn(ids);
	}

	static boolean installBenchmarkCitizen(ClientCitizen citizen) {
		if (!ClientCitizenCache.installBenchmark(citizen))
			return false;
		notifyAdded(citizen);
		return true;
	}

	static void removeBenchmarkCitizen(ClientCitizen citizen) {
		if (ClientCitizenCache.get(citizen.id) != citizen)
			return;
		BATCH_OWNERS.remove(citizen.id);
		notifyRemoved(citizen.id);
		FakeCitizenManager.remove(citizen.id);
		ClientCitizenCache.removeBenchmark(citizen);
	}

	static void updateBenchmarkCitizen(ClientCitizen citizen) {
		if (ClientCitizenCache.get(citizen.id) == citizen)
			notifyUpdated(citizen);
	}

	public static void tick(Minecraft minecraft) {
		ensureClientLevel(minecraft.level);
		CitizenClientBenchmark.tick(minecraft);
		FakeCitizenManager.tick(minecraft);
		try {
			backend.tick(minecraft);
		} catch (RuntimeException exception) {
			handleBackendFailure("client tick", exception);
		}
	}

	public static void render(RenderLevelStageEvent event) {
		ensureClientLevel(currentClientLevel());
		ensureHealthyBackend();
		try {
			backend.render(event);
		} catch (RuntimeException exception) {
			handleBackendFailure("render", exception);
		}
	}

	static boolean hasDetailedOwnership(ClientCitizen citizen) {
		return citizen.state != CitizenState.SLEEP && FakeCitizenManager.has(citizen.id);
	}

	static CitizenRenderOwner batchOwnerFor(ClientCitizen citizen, double distance2) {
		boolean sleeping = (citizen.state & 0xFF) == CitizenState.SLEEP;
		CitizenRenderOwner owner = CitizenRenderOwnership.resolve(sleeping, false, distance2,
				BATCH_OWNERS.get(citizen.id));
		if (owner == CitizenRenderOwner.BODY_BATCH || owner == CitizenRenderOwner.BILLBOARD_BATCH)
			BATCH_OWNERS.put(citizen.id, owner);
		else
			BATCH_OWNERS.remove(citizen.id);
		return owner;
	}

	public static void clearWorld() {
		transitionClientLevel(null);
	}

	static void onRenderersReloaded(ClientLevel reloadedLevel) {
		if (reloadedLevel != currentClientLevel())
			return;
		if (lastClientLevel != reloadedLevel) {
			transitionClientLevel(reloadedLevel);
			return;
		}
		boolean compatibilityFallbackActive = backend instanceof CpuBatchCitizenBackend;
		try {
			if (!backend.onRenderersReloaded(reloadedLevel)) {
				handleBackendFailure("renderer reload", null);
				return;
			}
		} catch (RuntimeException exception) {
			handleBackendFailure("renderer reload", exception);
			return;
		}
		if (compatibilityFallbackActive)
			restoreRequestedBackend("renderer reload");
	}

	private static void ensureClientLevel(ClientLevel currentLevel) {
		if (lastClientLevel != currentLevel)
			transitionClientLevel(currentLevel);
	}

	private static ClientLevel currentClientLevel() {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft == null ? null : minecraft.level;
	}

	private static void transitionClientLevel(ClientLevel nextLevel) {
		lastClientLevel = nextLevel;
		try {
			if (!backend.onClientLevelChanged(nextLevel))
				handleBackendFailure("client level change", null);
		} catch (RuntimeException exception) {
			handleBackendFailure("client level change", exception);
		}
		CitizenClientBenchmark.clear();
		ClientCitizenCache.clear();
		BATCH_OWNERS.clear();
		FakeCitizenManager.clearAll();
		CitizenRenderMetrics.reset();
		CitizenDebugOverlay.invalidate();
		if (nextLevel != null)
			restoreRequestedBackend("client level change");
	}

	public static void onResourceReload() {
		try {
			backend.onResourceReload();
		} catch (RuntimeException exception) {
			handleBackendFailure("resource reload", exception);
			return;
		}
		ensureHealthyBackend();
	}

	public static String backendName() {
		return backend.name();
	}

	static String requestedBackendName() {
		return requestedBackend.backendName;
	}

	static boolean isCompatibilityFallbackActive() {
		return requestedBackend == BackendPreference.FLYWHEEL_M3
				&& backend instanceof CpuBatchCitizenBackend;
	}

	static boolean useCpuBackend() {
		requestedBackend = BackendPreference.CPU_BATCH;
		return switchBackend(new CpuBatchCitizenBackend());
	}

	static boolean useFlywheelPocBackend() {
		requestedBackend = BackendPreference.FLYWHEEL_M3;
		if (BackendPreference.FLYWHEEL_M3.backendName.equals(backend.name()))
			return true;
		return switchBackend(flywheelBackendFactory.get());
	}

	static boolean switchBackend(CitizenRenderBackend candidate) {
		Objects.requireNonNull(candidate, "candidate");
		if (candidate == backend)
			return true;
		try {
			if (!candidate.initialize()) {
				closeCandidate(candidate);
				return false;
			}
			for (ClientCitizen citizen : ClientCitizenCache.values())
				candidate.onCitizenAdded(citizen);
			if (!candidate.isHealthy()) {
				closeCandidate(candidate);
				return false;
			}
		} catch (RuntimeException exception) {
			LOGGER.error("Failed to initialize citizen render backend {}", candidate.name(), exception);
			closeCandidate(candidate);
			return false;
		}

		CitizenRenderBackend previous = backend;
		backend = candidate;
		closeCandidate(previous);
		return true;
	}

	static void resetBackendForTests() {
		requestedBackend = BackendPreference.CPU_BATCH;
		flywheelBackendFactory = FlywheelCitizenBackend::new;
		BATCH_OWNERS.clear();
		switchBackend(new CpuBatchCitizenBackend());
	}

	static void setFlywheelBackendFactoryForTests(Supplier<CitizenRenderBackend> factory) {
		flywheelBackendFactory = Objects.requireNonNull(factory, "factory");
	}

	private static void restoreRequestedBackend(String trigger) {
		if (requestedBackend != BackendPreference.FLYWHEEL_M3
				|| !(backend instanceof CpuBatchCitizenBackend))
			return;
		if (switchBackend(flywheelBackendFactory.get()))
			LOGGER.info("Restored requested citizen render backend {} after {}",
					backend.name(), trigger);
	}

	private static void ensureHealthyBackend() {
		boolean healthy;
		try {
			healthy = backend.isHealthy();
		} catch (RuntimeException exception) {
			handleBackendFailure("health check", exception);
			return;
		}
		if (!healthy)
			handleBackendFailure("health check", null);
	}

	private static void notifyAdded(ClientCitizen citizen) {
		try {
			backend.onCitizenAdded(citizen);
		} catch (RuntimeException exception) {
			handleBackendFailure("citizen add", exception);
		}
	}

	private static void notifyUpdated(ClientCitizen citizen) {
		try {
			backend.onCitizenUpdated(citizen);
		} catch (RuntimeException exception) {
			handleBackendFailure("citizen update", exception);
		}
	}

	private static void notifyRemoved(int citizenId) {
		try {
			backend.onCitizenRemoved(citizenId);
		} catch (RuntimeException exception) {
			handleBackendFailure("citizen remove", exception);
		}
	}

	private static void handleBackendFailure(String operation, RuntimeException cause) {
		if (backend instanceof CpuBatchCitizenBackend) {
			if (cause != null)
				throw cause;
			throw new IllegalStateException("Citizen CPU fallback failed its " + operation);
		}
		String failedName = backend.name();
		if (cause == null)
			LOGGER.error("Citizen render backend {} failed its {}; falling back to cpu_batch",
					failedName, operation);
		else
			LOGGER.error("Citizen render backend {} failed during {}; falling back to cpu_batch",
					failedName, operation, cause);
		if (!switchBackend(new CpuBatchCitizenBackend()))
			throw new IllegalStateException("Unable to initialize citizen CPU fallback backend", cause);
	}

	private static void closeCandidate(CitizenRenderBackend candidate) {
		try {
			candidate.close();
		} catch (RuntimeException exception) {
			LOGGER.warn("Failed to close citizen render backend {}", candidate.name(), exception);
		}
	}
}
