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

import com.jozufozu.flywheel.api.Instancer;
import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.backend.instancing.InstanceManager;
import com.jozufozu.flywheel.backend.instancing.InstancedRenderDispatcher;
import com.jozufozu.flywheel.backend.instancing.instancing.InstancingEngine;
import com.jozufozu.flywheel.config.BackendType;
import com.jozufozu.flywheel.util.AnimationTickHolder;
import com.teammoeg.frostedheart.content.town.citizen.sim.CitizenState;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/**
 * Flywheel instancing backend. Network snapshots are uploaded once and interpolated,
 * extrapolated, oriented, and animated by the citizen vertex shader.
 */
final class FlywheelCitizenBackend implements CitizenRenderBackend, InstancingEngine.OriginShiftListener {

	static final int INSTANCE_STRIDE_BYTES = CitizenInstanceType.FORMAT.getStride();
	static final float ANIMATION_PERIOD_TICKS = 1_728_000.0f;
	private static final double SLEEP_LIGHT_Y = 0.58;
	private static final int LIGHT_SAMPLE_INTERVAL = 5;
	private static final Object[] BODY_MODEL_KEYS = createModelKeys();
	private static final Object[] BILLBOARD_MODEL_KEYS = createModelKeys();

	private final Int2ObjectOpenHashMap<Entry> entries = new Int2ObjectOpenHashMap<>();
	private final int[] bodySkinCounts = new int[CitizenSkins.count()];
	private final int[] billboardSkinCounts = new int[CitizenSkins.count()];
	private final BlockPos.MutableBlockPos lightPos = new BlockPos.MutableBlockPos();

	private ClientLevel level;
	private MaterialManager materialManager;
	private Instancer<CitizenInstanceData>[] bodyInstancers;
	private Instancer<CitizenInstanceData>[] billboardInstancers;
	private boolean initialized;
	private boolean closed;
	private boolean rebuildRequested;
	private int originX;
	private int originY;
	private int originZ;
	private int bodyCount;
	private int billboardCount;
	private int activeBatchCount;
	private int lightSamplesSinceFrame;
	private long dirtyBytesSinceFrame;

	@Override
	public String name() {
		return "flywheel_instancing";
	}

	@Override
	public boolean initialize() {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel currentLevel = minecraft.level;
		if (currentLevel == null || Backend.getBackendType() != BackendType.INSTANCING
				|| !Backend.canUseInstancing(currentLevel))
			return false;
		level = currentLevel;
		acquireResources();
		initialized = true;
		return true;
	}

	@Override
	public void onCitizenAdded(ClientCitizen citizen) {
		if (Minecraft.getInstance().level == level && !rebuildRequested)
			syncCitizen(citizen, Minecraft.getInstance().gameRenderer.getMainCamera().getPosition(), true);
	}

	@Override
	public void onCitizenUpdated(ClientCitizen citizen) {
		if (Minecraft.getInstance().level == level && !rebuildRequested)
			syncCitizen(citizen, Minecraft.getInstance().gameRenderer.getMainCamera().getPosition(), true);
	}

	@Override
	public void onCitizenRemoved(int citizenId) {
		removeEntry(citizenId);
	}

	@Override
	public void tick(Minecraft minecraft) {
		if (minecraft.level != level)
			throw new IllegalStateException("Flywheel citizen backend retained a stale client level");
		Vec3 cameraPos = minecraft.gameRenderer.getMainCamera().getPosition();
		ensureResources(cameraPos);

		Vec3i origin = materialManager.getOriginCoordinate();
		if (origin.getX() != originX || origin.getY() != originY || origin.getZ() != originZ)
			rebuildForOrigin(origin, cameraPos);

		for (ClientCitizen citizen : ClientCitizenCache.values())
			syncCitizen(citizen, cameraPos, false);
	}

	@Override
	public void onOriginShift() {
		Minecraft minecraft = Minecraft.getInstance();
		if (closed || materialManager == null || minecraft.level != level)
			return;
		rebuildForOrigin(materialManager.getOriginCoordinate(),
				minecraft.gameRenderer.getMainCamera().getPosition());
	}

	@Override
	public void render(RenderLevelStageEvent event) {
		long frameStart = System.nanoTime();
		long dirtyBytes = dirtyBytesSinceFrame;
		int lightSamples = lightSamplesSinceFrame;
		dirtyBytesSinceFrame = 0L;
		lightSamplesSinceFrame = 0;
		CitizenRenderMetrics.recordFrame(System.nanoTime() - frameStart, ClientCitizenCache.size(),
				FakeCitizenManager.activeCount(), entries.size(), bodyCount, billboardCount, activeBatchCount,
				lightSamples, dirtyBytes);
	}

	@Override
	public void clear() {
		releaseInstances();
	}

	@Override
	public void onResourceReload() {
		rebuildRequested = true;
	}

	@Override
	public boolean onClientLevelChanged(ClientLevel nextLevel) {
		if (closed)
			return false;
		// Flywheel may already have disposed the previous world's InstanceWorld.
		// Dropping detached handles avoids calling delete() through a dead manager.
		discardInvalidatedInstances();
		clearResourceReferences();
		level = nextLevel;
		initialized = false;
		rebuildRequested = false;
		if (nextLevel == null)
			return true;
		if (Backend.getBackendType() != BackendType.INSTANCING || !Backend.canUseInstancing(nextLevel))
			return false;
		acquireResources();
		initialized = true;
		return true;
	}

	@Override
	public boolean onRenderersReloaded(ClientLevel reloadedLevel) {
		if (reloadedLevel != level)
			return true;
		if (Backend.getBackendType() != BackendType.INSTANCING || !Backend.canUseInstancing(level))
			return false;
		MaterialManager nextManager = InstancedRenderDispatcher.getEntities(level).materialManager;
		rebuildForManager(nextManager, Minecraft.getInstance().gameRenderer.getMainCamera().getPosition());
		rebuildRequested = false;
		return true;
	}

	@Override
	public boolean isHealthy() {
		return initialized && !closed && level != null && Minecraft.getInstance().level == level
				&& Backend.getBackendType() == BackendType.INSTANCING && Backend.canUseInstancing(level);
	}

	@Override
	public void close() {
		if (closed)
			return;
		releaseInstances();
		clearResourceReferences();
		level = null;
		closed = true;
	}

	private void ensureResources(Vec3 cameraPos) {
		InstanceManager<Entity> entityManager = InstancedRenderDispatcher.getEntities(level);
		if (rebuildRequested || entityManager.materialManager != materialManager) {
			rebuildForManager(entityManager.materialManager, cameraPos);
			rebuildRequested = false;
		}
	}

	private void clearResourceReferences() {
		bodyInstancers = null;
		billboardInstancers = null;
		materialManager = null;
	}

	private void acquireResources() {
		acquireResources(InstancedRenderDispatcher.getEntities(level).materialManager);
	}

	@SuppressWarnings("unchecked")
	private void acquireResources(MaterialManager manager) {
		if (!(manager instanceof InstancingEngine<?> instancingEngine))
			throw new IllegalStateException("Flywheel citizen backend requires an instancing engine");
		materialManager = manager;
		instancingEngine.addListener(this);
		bodyInstancers = (Instancer<CitizenInstanceData>[]) new Instancer<?>[CitizenSkins.count()];
		billboardInstancers = (Instancer<CitizenInstanceData>[]) new Instancer<?>[CitizenSkins.count()];
		for (int skin = 0; skin < bodyInstancers.length; skin++) {
			RenderType renderType = CitizenBatchRenderLayout.skinRenderType(CitizenSkins.textureAt(skin));
			bodyInstancers[skin] = manager.cutout(renderType)
					.material(CitizenInstanceType.INSTANCE)
					.model(BODY_MODEL_KEYS[skin], CitizenFlywheelModels::createBody);
			billboardInstancers[skin] = manager.cutout(renderType)
					.material(CitizenInstanceType.INSTANCE)
					.model(BILLBOARD_MODEL_KEYS[skin], CitizenFlywheelModels::createBillboard);
		}
		Vec3i origin = manager.getOriginCoordinate();
		originX = origin.getX();
		originY = origin.getY();
		originZ = origin.getZ();
	}

	private void syncCitizen(ClientCitizen citizen, Vec3 cameraPos, boolean snapshotDirty) {
		syncCitizen(citizen, cameraPos, snapshotDirty, false);
	}

	private void syncCitizen(ClientCitizen citizen, Vec3 cameraPos, boolean snapshotDirty,
			boolean reuseCachedLight) {
		double[] pos = citizen.renderPos();
		double dx = pos[0] - cameraPos.x;
		double dy = pos[1] - cameraPos.y;
		double dz = pos[2] - cameraPos.z;
		Entry entry = entries.get(citizen.id);
		CitizenRenderOwner owner = CitizenRenderCoordinator.hasDetailedOwnership(citizen)
				? CitizenRenderOwner.DETAILED_ENTITY
				: CitizenRenderCoordinator.batchOwnerFor(citizen, dx * dx + dy * dy + dz * dz);
		if (owner != CitizenRenderOwner.BODY_BATCH && owner != CitizenRenderOwner.BILLBOARD_BATCH) {
			removeEntry(citizen.id);
			return;
		}

		if (entry == null || entry.owner != owner) {
			removeEntry(citizen.id);
			createEntry(citizen, owner, pos, reuseCachedLight);
			return;
		}

		boolean dirty = snapshotDirty;
		if (snapshotDirty)
			writeSnapshot(citizen, entry);
		if (sampleLight(citizen, entry.data, pos, false))
			dirty = true;
		if (dirty)
			markDirty(entry.data);
	}

	private void createEntry(ClientCitizen citizen, CitizenRenderOwner owner, double[] pos,
			boolean reuseCachedLight) {
		int skin = CitizenSkins.indexFor(citizen.id);
		Instancer<CitizenInstanceData> instancer = owner == CitizenRenderOwner.BODY_BATCH
				? bodyInstancers[skin] : billboardInstancers[skin];
		CitizenInstanceData data = instancer.createInstance();
		Entry entry = new Entry(data, skin, owner);
		entries.put(citizen.id, entry);
		incrementCounts(entry);
		writeSnapshot(citizen, entry);
		if (reuseCachedLight && citizen.lightBlockX != Integer.MIN_VALUE)
			writeCachedLight(citizen, data);
		else
			sampleLight(citizen, data, pos, true);
		markDirty(data);
	}

	private void removeEntry(int citizenId) {
		Entry entry = entries.remove(citizenId);
		if (entry == null)
			return;
		entry.data.delete();
		dirtyBytesSinceFrame += INSTANCE_STRIDE_BYTES;
		decrementCounts(entry);
	}

	private void releaseInstances() {
		for (Entry entry : entries.values()) {
			entry.data.delete();
			dirtyBytesSinceFrame += INSTANCE_STRIDE_BYTES;
		}
		entries.clear();
		for (int i = 0; i < bodySkinCounts.length; i++) {
			bodySkinCounts[i] = 0;
			billboardSkinCounts[i] = 0;
		}
		bodyCount = 0;
		billboardCount = 0;
		activeBatchCount = 0;
	}

	private void discardInvalidatedInstances() {
		dirtyBytesSinceFrame += (long) entries.size() * INSTANCE_STRIDE_BYTES;
		entries.clear();
		for (int i = 0; i < bodySkinCounts.length; i++) {
			bodySkinCounts[i] = 0;
			billboardSkinCounts[i] = 0;
		}
		bodyCount = 0;
		billboardCount = 0;
		activeBatchCount = 0;
	}

	private void incrementCounts(Entry entry) {
		if (entry.owner == CitizenRenderOwner.BODY_BATCH) {
			bodyCount++;
			if (bodySkinCounts[entry.skin]++ == 0)
				activeBatchCount++;
		} else {
			billboardCount++;
			if (billboardSkinCounts[entry.skin]++ == 0)
				activeBatchCount++;
		}
	}

	private void decrementCounts(Entry entry) {
		if (entry.owner == CitizenRenderOwner.BODY_BATCH) {
			bodyCount--;
			if (--bodySkinCounts[entry.skin] == 0)
				activeBatchCount--;
		} else {
			billboardCount--;
			if (--billboardSkinCounts[entry.skin] == 0)
				activeBatchCount--;
		}
	}

	private void rebuildForOrigin(Vec3i origin, Vec3 cameraPos) {
		// Flywheel clears every GPUInstancer slot before notifying origin listeners.
		// InstanceData.markDirty() cannot reinsert those detached handles.
		discardInvalidatedInstances();
		originX = origin.getX();
		originY = origin.getY();
		originZ = origin.getZ();
		recreateCachedCitizens(cameraPos);
	}

	private void rebuildForManager(MaterialManager nextManager, Vec3 cameraPos) {
		if (materialManager == nextManager)
			releaseInstances();
		else
			discardInvalidatedInstances();
		acquireResources(nextManager);
		recreateCachedCitizens(cameraPos);
	}

	private void recreateCachedCitizens(Vec3 cameraPos) {
		for (ClientCitizen citizen : ClientCitizenCache.values())
			syncCitizen(citizen, cameraPos, true, true);
	}

	private void writeSnapshot(ClientCitizen citizen, Entry entry) {
		CitizenInstanceData data = entry.data;
		data.pos0X = (float) (citizen.x0 - originX);
		data.pos0Y = (float) (citizen.y0 - originY);
		data.pos0Z = (float) (citizen.z0 - originZ);
		data.pos1X = (float) (citizen.x1 - originX);
		data.pos1Y = (float) (citizen.y1 - originY);
		data.pos1Z = (float) (citizen.z1 - originZ);

		double gameNow = ClientCitizen.currentTimeSeconds();
		// Keep snapshot timestamps in the same clock domain as shader uniform uTime.
		float animationNow = AnimationTickHolder.getRenderTime();
		data.snapshotTime = wrapAnimationTime(animationNow
				+ (float) ((citizen.snapshotStartSeconds() - gameNow) * 20.0));
		double interval = Mth.clamp(citizen.snapshotEndSeconds() - citizen.snapshotStartSeconds(), 0.05, 1.0);
		data.snapshotDuration = (float) (interval * 20.0);

		boolean moving = citizen.isMoving();
		if (moving) {
			float speed = CitizenState.SPEED[citizen.state & 0xFF] / (float) CitizenState.FIXED_SCALE;
			data.velocityX = CitizenState.DIR_X_16[citizen.dir] / (float) CitizenState.FIXED_SCALE * speed;
			data.velocityZ = CitizenState.DIR_Z_16[citizen.dir] / (float) CitizenState.FIXED_SCALE * speed;
		} else {
			data.velocityX = 0.0f;
			data.velocityZ = 0.0f;
		}

		int yawStart = citizen.visualYaw();
		int yawTarget = CitizenState.DIR_TO_YAW[citizen.dir & 15] & 0xFF;
		data.yawStart = yawStart;
		data.yawDelta = shortYawDelta(yawStart, yawTarget);
		data.yawTime = wrapAnimationTime(animationNow);
		data.moving = (byte) (moving ? 1 : 0);
		data.sleeping = (byte) ((citizen.state & 0xFF) == CitizenState.SLEEP ? 1 : 0);
		data.phase = encodeWalkPhase(citizen.walkPhase());
		data.reserved = 0;
	}

	private boolean sampleLight(ClientCitizen citizen, CitizenInstanceData data, double[] pos, boolean force) {
		long gameTime = level.getGameTime();
		boolean sleeping = (citizen.state & 0xFF) == CitizenState.SLEEP;
		int x = Mth.floor(pos[0]);
		int y = Mth.floor(pos[1] + (sleeping ? SLEEP_LIGHT_Y : 1.0));
		int z = Mth.floor(pos[2]);
		if (!force && x == citizen.lightBlockX && y == citizen.lightBlockY && z == citizen.lightBlockZ
				&& gameTime < citizen.nextLightSampleTick)
			return false;

		lightPos.set(x, y, z);
		int packedLight = LevelRenderer.getLightColor(level, lightPos);
		citizen.packedLight = packedLight;
		citizen.lightBlockX = x;
		citizen.lightBlockY = y;
		citizen.lightBlockZ = z;
		citizen.nextLightSampleTick = gameTime + LIGHT_SAMPLE_INTERVAL + (citizen.id & 3);
		lightSamplesSinceFrame++;
		byte blockLight = (byte) ((packedLight >> 4) & 0xF);
		byte skyLight = (byte) ((packedLight >> 20) & 0xF);
		if (data.blockLight == blockLight && data.skyLight == skyLight)
			return false;
		data.blockLight = blockLight;
		data.skyLight = skyLight;
		return true;
	}

	private static void writeCachedLight(ClientCitizen citizen, CitizenInstanceData data) {
		data.blockLight = (byte) ((citizen.packedLight >> 4) & 0xF);
		data.skyLight = (byte) ((citizen.packedLight >> 20) & 0xF);
	}

	private void markDirty(CitizenInstanceData data) {
		data.markDirty();
		dirtyBytesSinceFrame += INSTANCE_STRIDE_BYTES;
	}

	static float wrapAnimationTime(float time) {
		float wrapped = time % ANIMATION_PERIOD_TICKS;
		return wrapped < 0.0f ? wrapped + ANIMATION_PERIOD_TICKS : wrapped;
	}

	static int shortYawDelta(int start, int target) {
		int delta = (target - start) & 0xFF;
		return delta > 128 ? delta - 256 : delta;
	}

	static byte encodeWalkPhase(float phase) {
		int encoded = Mth.floor(CitizenBatchRenderLayout.wrapWalkPhase(phase) * 256.0f
				/ CitizenBatchRenderLayout.WALK_PHASE_PERIOD + 0.5f) & 0xFF;
		return (byte) encoded;
	}

	static com.jozufozu.flywheel.core.model.Model createBodyModel() {
		return CitizenFlywheelModels.createBody();
	}

	static com.jozufozu.flywheel.core.model.Model createBillboardModel() {
		return CitizenFlywheelModels.createBillboard();
	}

	private static Object[] createModelKeys() {
		Object[] keys = new Object[CitizenSkins.count()];
		for (int i = 0; i < keys.length; i++)
			keys[i] = new Object();
		return keys;
	}

	private static final class Entry {
		private final CitizenInstanceData data;
		private final int skin;
		private final CitizenRenderOwner owner;

		private Entry(CitizenInstanceData data, int skin, CitizenRenderOwner owner) {
			this.data = data;
			this.skin = skin;
			this.owner = owner;
		}
	}
}
