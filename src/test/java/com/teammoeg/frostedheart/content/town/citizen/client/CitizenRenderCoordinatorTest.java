/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.citizen.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.jozufozu.flywheel.event.ReloadRenderersEvent;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

class CitizenRenderCoordinatorTest {

	@BeforeEach
	void clearCache() {
		ClientCitizenCache.clear();
	}

	@AfterEach
	void restoreCpuBackend() {
		ClientCitizenCache.clear();
		CitizenRenderCoordinator.resetBackendForTests();
	}

	@Test
	void initializesCandidateBeforeAtomicallyClosingPreviousBackend() {
		TrackingBackend first = new TrackingBackend("first", true, true);
		TrackingBackend second = new TrackingBackend("second", true, true);

		assertTrue(CitizenRenderCoordinator.switchBackend(first));
		assertTrue(first.initialized);
		assertFalse(first.closed);
		assertTrue(CitizenRenderCoordinator.switchBackend(second));
		assertTrue(second.initialized);
		assertTrue(first.closed);
		assertFalse(second.closed);
		assertEquals("second", CitizenRenderCoordinator.backendName());
	}

	@Test
	void rejectsUninitializedCandidateWithoutClosingCurrentBackend() {
		TrackingBackend current = new TrackingBackend("current", true, true);
		TrackingBackend rejected = new TrackingBackend("rejected", false, true);
		assertTrue(CitizenRenderCoordinator.switchBackend(current));

		assertFalse(CitizenRenderCoordinator.switchBackend(rejected));
		assertTrue(rejected.closed);
		assertFalse(current.closed);
		assertEquals("current", CitizenRenderCoordinator.backendName());
	}

	@Test
	void resourceReloadFallsBackWhenBackendBecomesUnhealthy() {
		TrackingBackend backend = new TrackingBackend("unstable", true, true);
		assertTrue(CitizenRenderCoordinator.switchBackend(backend));
		backend.healthy = false;

		CitizenRenderCoordinator.onResourceReload();

		assertEquals(1, backend.reloads);
		assertTrue(backend.closed);
		assertEquals("cpu_batch", CitizenRenderCoordinator.backendName());
	}

	@Test
	void renderFailureSwitchesBackendWithoutDrawingCpuInTheSameFrame() {
		TrackingBackend backend = new TrackingBackend("throwing", true, true);
		backend.throwOnRender = true;
		assertTrue(CitizenRenderCoordinator.switchBackend(backend));

		CitizenRenderCoordinator.render(null);

		assertEquals(1, backend.renders);
		assertTrue(backend.closed);
		assertEquals("cpu_batch", CitizenRenderCoordinator.backendName());
	}

	@Test
	void worldClearKeepsSelectedBackendAndRebindsItsLevel() {
		TrackingBackend backend = new TrackingBackend("stateful", true, true);
		assertTrue(CitizenRenderCoordinator.switchBackend(backend));

		CitizenRenderCoordinator.clearWorld();

		assertEquals(1, backend.clears);
		assertEquals(1, backend.levelChanges);
		assertFalse(backend.closed);
		assertEquals("stateful", CitizenRenderCoordinator.backendName());
	}

	@Test
	void clientLevelRebindFailureFallsBackToCpu() {
		TrackingBackend backend = new TrackingBackend("stale-level", true, true);
		backend.levelChangeResult = false;
		assertTrue(CitizenRenderCoordinator.switchBackend(backend));

		CitizenRenderCoordinator.clearWorld();

		assertEquals(1, backend.levelChanges);
		assertTrue(backend.closed);
		assertEquals("cpu_batch", CitizenRenderCoordinator.backendName());
	}

	@Test
	void rendererReloadRebindsSelectedBackendImmediately() {
		TrackingBackend backend = new TrackingBackend("reloadable", true, true);
		assertTrue(CitizenRenderCoordinator.switchBackend(backend));

		CitizenRenderCoordinator.onRenderersReloaded(null);

		assertEquals(1, backend.rendererReloads);
		assertFalse(backend.closed);
		assertEquals("reloadable", CitizenRenderCoordinator.backendName());
	}

	@Test
	void rendererReloadFailureFallsBackToCpu() {
		TrackingBackend backend = new TrackingBackend("stale-manager", true, true);
		backend.rendererReloadResult = false;
		assertTrue(CitizenRenderCoordinator.switchBackend(backend));

		CitizenRenderCoordinator.onRenderersReloaded(null);

		assertEquals(1, backend.rendererReloads);
		assertTrue(backend.closed);
		assertEquals("cpu_batch", CitizenRenderCoordinator.backendName());
	}

	@Test
	void preferredFlywheelBackendRestoresAfterACompatibilityFallback() {
		TrackingBackend unavailable = new TrackingBackend("flywheel_instancing", false, false);
		TrackingBackend restored = new TrackingBackend("flywheel_instancing", true, true);
		AtomicInteger creations = new AtomicInteger();
		CitizenRenderCoordinator.setFlywheelBackendFactoryForTests(
				() -> creations.getAndIncrement() == 0 ? unavailable : restored);

		assertFalse(CitizenRenderCoordinator.useFlywheelBackend());
		assertEquals("cpu_batch", CitizenRenderCoordinator.backendName());
		assertEquals("flywheel_instancing", CitizenRenderCoordinator.requestedBackendName());
		assertTrue(unavailable.closed);

		CitizenRenderCoordinator.onRenderersReloaded(null);

		assertEquals(2, creations.get());
		assertEquals("flywheel_instancing", CitizenRenderCoordinator.backendName());
		assertEquals("flywheel_instancing", CitizenRenderCoordinator.requestedBackendName());
		assertFalse(restored.closed);
	}

	@Test
	void autoUsesFlywheelWhenAvailable() {
		TrackingBackend available = new TrackingBackend("flywheel_instancing", true, true);
		CitizenRenderCoordinator.setFlywheelBackendFactoryForTests(() -> available);

		assertTrue(CitizenRenderCoordinator.useAutoBackend());

		assertEquals("flywheel_instancing", CitizenRenderCoordinator.backendName());
		assertEquals("auto", CitizenRenderCoordinator.requestedBackendName());
		assertFalse(CitizenRenderCoordinator.isCompatibilityFallbackActive());
	}

	@Test
	void autoKeepsCpuWhenInstancingIsUnavailableAndRetriesAfterReload() {
		TrackingBackend unavailable = new TrackingBackend("flywheel_instancing", false, false);
		TrackingBackend restored = new TrackingBackend("flywheel_instancing", true, true);
		AtomicInteger creations = new AtomicInteger();
		CitizenRenderCoordinator.setFlywheelBackendFactoryForTests(
				() -> creations.getAndIncrement() == 0 ? unavailable : restored);

		assertFalse(CitizenRenderCoordinator.useAutoBackend());
		assertEquals("cpu_batch", CitizenRenderCoordinator.backendName());
		assertEquals("auto", CitizenRenderCoordinator.requestedBackendName());
		assertTrue(CitizenRenderCoordinator.isCompatibilityFallbackActive());

		CitizenRenderCoordinator.onRenderersReloaded(null);

		assertEquals(2, creations.get());
		assertEquals("flywheel_instancing", CitizenRenderCoordinator.backendName());
		assertEquals("auto", CitizenRenderCoordinator.requestedBackendName());
		assertFalse(CitizenRenderCoordinator.isCompatibilityFallbackActive());
	}

	@Test
	void autoKeepsCpuWhenTheFlywheelBackendCannotBeCreated() {
		CitizenRenderCoordinator.setFlywheelBackendFactoryForTests(
				() -> {
					throw new IllegalStateException("injected Flywheel construction failure");
				});

		assertFalse(CitizenRenderCoordinator.useAutoBackend());
		assertEquals("cpu_batch", CitizenRenderCoordinator.backendName());
		assertEquals("auto", CitizenRenderCoordinator.requestedBackendName());
		assertTrue(CitizenRenderCoordinator.isCompatibilityFallbackActive());
	}

	@Test
	void explicitCpuSelectionCancelsPendingAutoRestore() {
		TrackingBackend unavailable = new TrackingBackend("flywheel_instancing", false, false);
		TrackingBackend unexpectedRestore = new TrackingBackend("flywheel_instancing", true, true);
		AtomicInteger creations = new AtomicInteger();
		CitizenRenderCoordinator.setFlywheelBackendFactoryForTests(
				() -> creations.getAndIncrement() == 0 ? unavailable : unexpectedRestore);

		assertFalse(CitizenRenderCoordinator.useAutoBackend());
		assertTrue(CitizenRenderCoordinator.useCpuBackend());
		CitizenRenderCoordinator.onRenderersReloaded(null);

		assertEquals(1, creations.get());
		assertEquals("cpu_batch", CitizenRenderCoordinator.backendName());
		assertEquals("cpu_batch", CitizenRenderCoordinator.requestedBackendName());
		assertFalse(unexpectedRestore.initialized);
	}

	@Test
	void explicitCpuSelectionCancelsPendingFlywheelRestore() {
		TrackingBackend unavailable = new TrackingBackend("flywheel_instancing", false, false);
		TrackingBackend unexpectedRestore = new TrackingBackend("flywheel_instancing", true, true);
		AtomicInteger creations = new AtomicInteger();
		CitizenRenderCoordinator.setFlywheelBackendFactoryForTests(
				() -> creations.getAndIncrement() == 0 ? unavailable : unexpectedRestore);

		assertFalse(CitizenRenderCoordinator.useFlywheelBackend());
		assertTrue(CitizenRenderCoordinator.useCpuBackend());
		CitizenRenderCoordinator.onRenderersReloaded(null);

		assertEquals(1, creations.get());
		assertEquals("cpu_batch", CitizenRenderCoordinator.backendName());
		assertEquals("cpu_batch", CitizenRenderCoordinator.requestedBackendName());
		assertFalse(unexpectedRestore.initialized);
	}

	@Test
	void flywheelHealthFallbackKeepsTheRequestForTheNextRendererReload() {
		TrackingBackend activeFlywheel = new TrackingBackend("flywheel_instancing", true, true);
		TrackingBackend restoredFlywheel = new TrackingBackend("flywheel_instancing", true, true);
		AtomicInteger creations = new AtomicInteger();
		CitizenRenderCoordinator.setFlywheelBackendFactoryForTests(
				() -> creations.getAndIncrement() == 0 ? activeFlywheel : restoredFlywheel);

		assertTrue(CitizenRenderCoordinator.useFlywheelBackend());
		activeFlywheel.healthy = false;
		CitizenRenderCoordinator.onResourceReload();

		assertTrue(activeFlywheel.closed);
		assertEquals("cpu_batch", CitizenRenderCoordinator.backendName());
		assertEquals("flywheel_instancing", CitizenRenderCoordinator.requestedBackendName());

		CitizenRenderCoordinator.onRenderersReloaded(null);

		assertEquals(2, creations.get());
		assertEquals("flywheel_instancing", CitizenRenderCoordinator.backendName());
		assertFalse(restoredFlywheel.closed);
	}

	@Test
	void flywheelReloadListenerRunsAfterItsDefaultRendererReset() throws ReflectiveOperationException {
		SubscribeEvent annotation = CitizenClientEvents.class
				.getDeclaredMethod("onFlywheelRenderersReloaded", ReloadRenderersEvent.class)
				.getAnnotation(SubscribeEvent.class);

		assertEquals(EventPriority.LOWEST, annotation.priority());
	}

	@Test
	void coordinatorKeepsBatchLodHysteresisAcrossBackendCalls() {
		CitizenRenderCoordinator.resetBackendForTests();
		ClientCitizen citizen = new ClientCitizen(0x7FFF0001, 0, 0, 0, (byte) 0, "");

		assertEquals(CitizenRenderOwner.BODY_BATCH,
				CitizenRenderCoordinator.batchOwnerFor(citizen, 67.0 * 67.0));
		assertEquals(CitizenRenderOwner.BODY_BATCH,
				CitizenRenderCoordinator.batchOwnerFor(citizen, 70.0 * 70.0));
		assertEquals(CitizenRenderOwner.BILLBOARD_BATCH,
				CitizenRenderCoordinator.batchOwnerFor(citizen, 73.0 * 73.0));
		assertEquals(CitizenRenderOwner.BILLBOARD_BATCH,
				CitizenRenderCoordinator.batchOwnerFor(citizen, 70.0 * 70.0));
		assertEquals(CitizenRenderOwner.NONE,
				CitizenRenderCoordinator.batchOwnerFor(citizen, 97.0 * 97.0));
		assertEquals(CitizenRenderOwner.BILLBOARD_BATCH,
				CitizenRenderCoordinator.batchOwnerFor(citizen, 70.0 * 70.0));
		assertEquals(CitizenRenderOwner.BODY_BATCH,
				CitizenRenderCoordinator.batchOwnerFor(citizen, 67.0 * 67.0));
	}

	private static final class TrackingBackend implements CitizenRenderBackend {
		private final String name;
		private final boolean initializeResult;
		private boolean healthy;
		private boolean initialized;
		private boolean closed;
		private boolean throwOnRender;
		private boolean levelChangeResult = true;
		private boolean rendererReloadResult = true;
		private int clears;
		private int levelChanges;
		private int reloads;
		private int rendererReloads;
		private int renders;

		private TrackingBackend(String name, boolean initializeResult, boolean healthy) {
			this.name = name;
			this.initializeResult = initializeResult;
			this.healthy = healthy;
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public boolean initialize() {
			initialized = true;
			return initializeResult;
		}

		@Override
		public void render(RenderLevelStageEvent event) {
			renders++;
			if (throwOnRender)
				throw new IllegalStateException("injected render failure");
		}

		@Override
		public void clear() {
			clears++;
		}

		@Override
		public void onResourceReload() {
			reloads++;
		}

		@Override
		public boolean onClientLevelChanged(ClientLevel level) {
			levelChanges++;
			clear();
			return levelChangeResult;
		}

		@Override
		public boolean onRenderersReloaded(ClientLevel level) {
			rendererReloads++;
			return rendererReloadResult;
		}

		@Override
		public boolean isHealthy() {
			return healthy;
		}

		@Override
		public void close() {
			closed = true;
		}
	}
}
