/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.PagePublication;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PageSignatures;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftSignatureCapture;
import com.teammoeg.frostedheart.content.climate.thermal.radiation.minecraft.MinecraftRadiationOcclusion;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalCompletion;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.PhysicalSourceSpatialIndex;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.ChunkPos;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.IdentityHashMap;
import java.util.UUID;

/**
 * 服务器主线程上的 Page 生命周期所有者。
 *
 * <p>它把玩家、source 和 continuation interest 合并为 Page admission，
 * 将方块 mutation 收敛为局部几何输入，并在 retirement 前生成 dormant
 * checkpoint。{@link ThermalPageHandle} 是它与 worker 共享的唯一 Page 身份；
 * 已提交拓扑不在这里保存。</p>
 */
public final class MinecraftPageManager implements AutoCloseable {
    private static final int PRIMARY = 0;
    private static final int SOURCE = 1;
    private static final int CONTINUATION = 2;
    private static final int PRIORITY_COUNT = 3;
    private static final int MAX_ADMISSIONS_PER_TICK = 1;
    private static final int MAX_FULL_RESYNCS_PER_TICK = 1;
    private static final int MAX_CENTERS_PER_TICK = 256;
    private static final int MAX_ADMISSION_ATTEMPTS_PER_PRIORITY = 8;
    private static final int MAX_SOURCE_RECOVERY_CHUNKS_PER_CUT = 64;
    private static final int SPARSE_CENTER_LIMIT = 1024;
    private static final int CENTER_BITMAP_THRESHOLD = 32;
    private static final int INITIAL_CENTER_CAPACITY = 8;
    private static final int UNCAPTURED_SIGNATURE = Integer.MIN_VALUE;
    private static final long PLAYER_LEASE_TICKS = 40L;
    private static final long PREVIOUS_PLAYER_LEASE_TICKS = 20L;
    private static final long WORK_LIMIT_RETRY_TICKS = 200L;
    private static final int WHEEL_SIZE = 64;

    private final ServerLevel level;
    private final MinecraftThermalInput input;
    private final Thread mainThread;
    private DimensionInputAccumulator accumulator;
    private final MinecraftSignatureCapture signatures;
    private final MinecraftEnvironmentCapture environment;
    private final Long2ObjectOpenHashMap<PageEntry> pages =
            new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<LongOpenHashSet> pagesByChunk =
            new Long2ObjectOpenHashMap<>();
    private final LongLinkedOpenHashSet[] admissionQueues;
    private final LongLinkedOpenHashSet captureQueue =
            new LongLinkedOpenHashSet();
    private final LongLinkedOpenHashSet readyCaptures =
            new LongLinkedOpenHashSet();
    private final Object2ObjectOpenHashMap<UUID, PlayerLease> playerLeases =
            new Object2ObjectOpenHashMap<>();
    private final PlayerLease[] playerWheel = new PlayerLease[WHEEL_SIZE];
    private final Long2ObjectOpenHashMap<SectionOwner> ownersBySection =
            new Long2ObjectOpenHashMap<>();
    private final IdentityHashMap<LevelChunkSection, SectionOwner>
            ownersByIdentity = new IdentityHashMap<>();
    private final ConcurrentLinkedQueue<SectionOwner> dirtyOwners =
            new ConcurrentLinkedQueue<>();
    private final MutationScratch mutationScratch = new MutationScratch();
    private final LongLinkedOpenHashSet sourceScannedChunks =
            new LongLinkedOpenHashSet();

    private PhysicalSourceSpatialIndex physicalSources;
    private MinecraftRadiationOcclusion radiationOcclusion;
    private long nextLifecycleGeneration = 1L;
    private int sourceRecoveryRemaining;
    private boolean closed;

    @SuppressWarnings("unchecked")
    public MinecraftPageManager(
            MinecraftThermalInput input,
            ServerLevel level,
            DimensionInputAccumulator accumulator,
            MinecraftSignatureCapture signatures,
            MinecraftEnvironmentCapture environment
    ) {
        this.input = input;
        this.level = level;
        mainThread = Thread.currentThread();
        this.accumulator = accumulator;
        this.signatures = signatures;
        this.environment = environment;
        admissionQueues = new LongLinkedOpenHashSet[PRIORITY_COUNT];
        for (int index = 0; index < admissionQueues.length; index++) {
            admissionQueues[index] = new LongLinkedOpenHashSet();
        }
    }

    public void attachMutationConsumers(
            PhysicalSourceSpatialIndex sources,
            MinecraftRadiationOcclusion occlusion
    ) {
        physicalSources = sources;
        radiationOcclusion = occlusion;
    }

    public void requestPlayerPage(
            UUID playerId,
            BlockPos position,
            long gameTick
    ) {
        requireMainThread();
        long sectionKey = SectionPos.asLong(
                SectionPos.blockToSectionCoord(position.getX()),
                SectionPos.blockToSectionCoord(position.getY()),
                SectionPos.blockToSectionCoord(position.getZ()));
        PlayerLease lease = playerLeases.get(playerId);
        if (lease == null) {
            lease = new PlayerLease(playerId);
            playerLeases.put(playerId, lease);
            lease.currentSection = sectionKey;
            retain(sectionKey, PRIMARY);
        } else if (lease.currentSection != sectionKey) {
            if (lease.previousSection != Long.MIN_VALUE) {
                release(lease.previousSection, PRIMARY);
            }
            lease.previousSection = lease.currentSection;
            lease.previousExpiry =
                    gameTick + PREVIOUS_PLAYER_LEASE_TICKS;
            lease.currentSection = sectionKey;
            retain(sectionKey, PRIMARY);
        }
        lease.currentExpiry = gameTick + PLAYER_LEASE_TICKS;
        schedulePlayer(lease);
    }

    public void releasePlayer(UUID playerId) {
        requireMainThread();
        PlayerLease lease = playerLeases.remove(playerId);
        if (lease == null) {
            return;
        }
        unlinkPlayer(lease);
        release(lease.currentSection, PRIMARY);
        if (lease.previousSection != Long.MIN_VALUE) {
            release(lease.previousSection, PRIMARY);
        }
    }

    public void updateSourcePage(long sectionKey, boolean retained) {
        requireMainThread();
        if (retained) retain(sectionKey, SOURCE);
        else release(sectionKey, SOURCE);
    }

    ThermalPageHandle handle(long sectionKey) {
        PageEntry page = pages.get(sectionKey);
        return page == null ? null : page.handle;
    }

    /**
     * Collects Page lifecycles with at least one coherent publication in the
     * fixed 9x9x9 infrared region.
     */
    public int collectInfraredPages(
            int centerChunkX,
            int centerSectionY,
            int centerChunkZ,
            ThermalPageHandle[] handles,
            short[] localIndexes,
            long[] presence
    ) {
        requireMainThread();
        if (handles == null || handles.length < 729
                || localIndexes == null || localIndexes.length < 729
                || presence == null || presence.length < 12) {
            throw new IllegalArgumentException("infrared Page scratch is too small");
        }
        Arrays.fill(handles, null);
        Arrays.fill(presence, 0L);
        int count = 0;
        for (int dz = -4; dz <= 4; dz++) {
            for (int dx = -4; dx <= 4; dx++) {
                LongOpenHashSet indexed = pagesByChunk.get(ChunkPos.asLong(
                        centerChunkX + dx, centerChunkZ + dz));
                if (indexed == null) {
                    continue;
                }
                for (long sectionKey : indexed) {
                    int dy = SectionPos.y(sectionKey) - centerSectionY;
                    if (dy < -4 || dy > 4) {
                        continue;
                    }
                    PageEntry page = pages.get(sectionKey);
                    ThermalPageHandle handle = page == null ? null : page.handle;
                    if (handle == null || handle.lastPublication() == null) {
                        continue;
                    }
                    int localIndex = ((dy + 4) * 9 + (dz + 4)) * 9 + dx + 4;
                    handles[count] = handle;
                    localIndexes[count] = (short) localIndex;
                    presence[localIndex >>> 6] |= 1L << (localIndex & 63);
                    count++;
                }
            }
        }
        return count;
    }

    public void acknowledgeResync(
            ThermalPageHandle.GeometryResyncToken[] tokens
    ) {
        for (ThermalPageHandle.GeometryResyncToken token : tokens) {
            PageEntry page = pages.get(token.sectionKey());
            if (page != null && page.handle != null) {
                page.handle.acknowledgeFullGeometryResync(token);
            }
        }
    }

    public void reseedAll(DimensionInputAccumulator next) {
        accumulator = next;
        captureQueue.clear();
        readyCaptures.clear();
        for (PageEntry page : pages.values()) {
            ThermalPageHandle handle = page.handle;
            if (handle == null) {
                continue;
            }
            page.resetCapture();
            MinecraftEnvironmentCapture.Captured captured =
                    environment.current(handle);
            if (captured == null) {
                continue;
            }
            SectionOwner owner = ownersBySection.get(page.sectionKey);
            PageSignatures snapshot = signatures.capturePage(
                    page.sectionKey,
                    owner == null ? null : owner.section);
            accumulator.admit(
                    handle,
                    handle.liveGeometryRevision(),
                    snapshot,
                    captured.naturalTemperatureC(),
                    captured.firstExposedLocalY(),
                    input.dormantAdmissionCut(
                            handle.sectionKey(),
                            captured.naturalTemperatureC(),
                            level.getGameTime()));
            ThermalPageHandle.GeometryResyncToken resync =
                    handle.pendingFullGeometryResync();
            if (resync != null) {
                handle.acknowledgeFullGeometryResync(resync);
            }
        }
    }

    public void retryWorkLimited(ThermalInputBatch batch, long gameTick) {
        for (ThermalInputBatch.PageAdmission admission : batch.admissions()) {
            PageEntry page = pages.get(admission.page().sectionKey());
            if (page != null && page.handle == admission.page()) {
                environment.untrack(page.handle);
                readyCaptures.remove(page.sectionKey);
                page.handle = null;
                publishPageHandle(page.sectionKey, null);
                page.resetCapture();
                page.retryAfterTick = gameTick + WORK_LIMIT_RETRY_TICKS;
                enqueue(page);
            }
        }
        for (ThermalInputBatch.PageRetirement retirement
                : batch.retirements()) {
            accumulator.retire(retirement.page());
        }
        for (int index = 0; index < batch.geometry().size(); index++) {
            ThermalPageHandle handle = batch.geometry().page(index);
            PageEntry page = pages.get(handle.sectionKey());
            if (page != null && page.handle == handle) {
                retire(page);
                page.retryAfterTick = gameTick + WORK_LIMIT_RETRY_TICKS;
                enqueue(page);
            }
        }
        for (ThermalInputBatch.PageEnvironmentUpdate update
                : batch.environmentUpdates()) {
            PageEntry page = pages.get(update.page().sectionKey());
            if (page == null || page.handle != update.page()) {
                continue;
            }
            accumulator.requeueEnvironment(update);
        }
    }

    public void applyContinuation(
            ThermalCompletion.PageContinuation continuation
    ) {
        PageEntry page = pages.get(continuation.sectionKey());
        if (page == null || page.handle == null
                || page.handle.lifecycleGeneration()
                        != continuation.lifecycleGeneration()) {
            return;
        }
        PagePublication publication = page.handle.currentPublication();
        if (publication == null
                || publication.geometryRevision()
                        != continuation.geometryRevision()
                || publication.topologyGeneration()
                        != continuation.topologyGeneration()) {
            return;
        }
        boolean primary = page.playerReferences != 0
                || page.sourceReferences != 0;
        int nextMask = primary
                ? Byte.toUnsignedInt(continuation.faceMask()) : 0;
        int changed = page.continuationFaceMask ^ nextMask;
        for (int face = 0; face < 6; face++) {
            int bit = 1 << face;
            if ((changed & bit) == 0) {
                continue;
            }
            long neighbor = neighbor(page.sectionKey, face);
            if ((nextMask & bit) != 0) {
                retain(neighbor, CONTINUATION);
            } else {
                release(neighbor, CONTINUATION);
            }
        }
        page.continuationFaceMask = nextMask;
    }

    public void tick(long gameTick) {
        requireMainThread();
        expirePlayers(gameTick);
        drainMutations(gameTick);
        processCaptures(gameTick);
        processAdmissions(gameTick);
        environment.tick(gameTick, this);
    }

    public void flushCapturedGeometry() {
        while (!readyCaptures.isEmpty()) {
            long sectionKey = readyCaptures.removeFirstLong();
            PageEntry page = pages.get(sectionKey);
            if (page == null || page.handle == null) {
                continue;
            }
            if (page.fullSignatureCut != null) {
                accumulator.geometry().addFullResync(
                        page.handle,
                        page.captureRevision,
                        page.resyncReason,
                        page.fullSignatureCut);
            } else {
                for (int index = 0; index < page.centerCount; index++) {
                    accumulator.geometry().addResolvedCenter(
                            page.handle,
                            page.captureRevision,
                            Short.toUnsignedInt(page.centers[index]),
                            page.signatureIds[index]);
                }
            }
            page.resetCapture();
        }
    }

    public boolean recoverPhysicalSourceCapacity() {
        requireMainThread();
        if (physicalSources == null
                || !physicalSources.capacityRecoveryPending()
                || !physicalSources.hasAvailableCapacity()) {
            return false;
        }
        if (sourceScannedChunks.isEmpty()) {
            physicalSources.beginCapacityRecoveryPass();
            sourceRecoveryRemaining = 0;
            return false;
        }
        if (sourceRecoveryRemaining == 0) {
            sourceRecoveryRemaining = sourceScannedChunks.size();
            physicalSources.beginCapacityRecoveryPass();
        }
        int remaining = Math.min(
                sourceRecoveryRemaining,
                MAX_SOURCE_RECOVERY_CHUNKS_PER_CUT);
        boolean scanned = false;
        while (remaining-- > 0
                && physicalSources.hasAvailableCapacity()
                && !sourceScannedChunks.isEmpty()) {
            long chunkKey = sourceScannedChunks.removeFirstLong();
            sourceScannedChunks.add(chunkKey);
            sourceRecoveryRemaining--;
            LevelChunk chunk = level.getChunkSource().getChunkNow(
                    (int) chunkKey,
                    (int) (chunkKey >>> Integer.SIZE));
            if (chunk != null) {
                physicalSources.onChunkLoad(chunk);
                scanned = true;
            }
        }
        if (sourceRecoveryRemaining != 0) {
            physicalSources.continueCapacityRecoveryPass();
        }
        return scanned;
    }

    void refreshSkyColumn(
            int worldX,
            int worldZ,
            int exposedWorldY
    ) {
        long chunkKey = ChunkPos.asLong(
                SectionPos.blockToSectionCoord(worldX),
                SectionPos.blockToSectionCoord(worldZ));
        LongOpenHashSet indexed = pagesByChunk.get(chunkKey);
        if (indexed == null) {
            return;
        }
        for (long sectionKey : indexed) {
            PageEntry page = pages.get(sectionKey);
            if (page != null && page.handle != null) {
                environment.updateSkyColumn(
                        page.handle, worldX, worldZ, exposedWorldY);
            }
        }
    }

    public void onChunkLoad(LevelChunk chunk) {
        requireMainThread();
        for (int index = 0; index < chunk.getSections().length; index++) {
            attachSection(
                    chunk,
                    index,
                    chunk.getSections()[index]);
        }
        LongOpenHashSet indexed = pagesByChunk.get(chunk.getPos().toLong());
        if (indexed != null) {
            for (long sectionKey : indexed) {
                PageEntry page = pages.get(sectionKey);
                if (page != null && page.handle == null) {
                    enqueue(page);
                }
            }
        }
    }

    public void onChunkUnload(LevelChunk chunk) {
        requireMainThread();
        sourceScannedChunks.remove(chunk.getPos().toLong());
        sourceRecoveryRemaining = 0;
        LongOpenHashSet indexed = pagesByChunk.get(chunk.getPos().toLong());
        if (indexed != null) {
            long[] sections = indexed.toLongArray();
            for (long sectionKey : sections) {
                PageEntry page = pages.get(sectionKey);
                if (page != null && page.handle != null) {
                    retire(page);
                }
            }
        }
        for (LevelChunkSection section : chunk.getSections()) {
            SectionOwner owner = ownersByIdentity.remove(section);
            if (owner != null) {
                ownersBySection.remove(owner.sectionKey);
                owner.invalidate();
            }
        }
    }

    public void onSectionIdentityReplaced(
            LevelChunk chunk,
            int sectionIndex,
            LevelChunkSection previous,
            LevelChunkSection replacement
    ) {
        requireMainThread();
        SectionOwner old = ownersByIdentity.remove(previous);
        if (old != null) {
            ownersBySection.remove(old.sectionKey);
            old.invalidate();
        }
        attachSection(chunk, sectionIndex, replacement);
        long key = SectionPos.asLong(
                chunk.getPos().x,
                chunk.getSectionYFromSectionIndex(sectionIndex),
                chunk.getPos().z);
        SectionOwner next = ownersBySection.get(key);
        if (next != null) {
            next.recordFullResync(
                    ThermalPageHandle.GeometryResyncReason.SECTION_REPLACED);
        }
    }

    public void onBlockMutation(
            SectionOwner owner,
            int localX,
            int localY,
            int localZ,
            boolean topologyRelevant,
            boolean sourceRelevant
    ) {
        if (owner == null || !owner.valid) {
            return;
        }
        owner.record(
                localX, localY, localZ,
                topologyRelevant, sourceRelevant);
    }

    private void processAdmissions(long gameTick) {
        int remaining = MAX_ADMISSIONS_PER_TICK;
        for (int priority = 0;
             priority < PRIORITY_COUNT && remaining > 0;
             priority++) {
            LongLinkedOpenHashSet queue = admissionQueues[priority];
            int attempts = Math.min(queue.size(), MAX_ADMISSION_ATTEMPTS_PER_PRIORITY);
            while (remaining > 0 && attempts-- > 0 && !queue.isEmpty()) {
                long sectionKey = queue.removeFirstLong();
                PageEntry page = pages.get(sectionKey);
                if (page == null || page.handle != null
                        || !page.interested()) {
                    continue;
                }
                if (page.retryAfterTick > gameTick) {
                    queue.add(sectionKey);
                    continue;
                }
                page.retryAfterTick = 0L;
                page.queuedPriority = -1;
                if (page.priority() != priority || !admit(page, gameTick)) {
                    enqueue(page);
                    continue;
                }
                remaining--;
            }
        }
    }

    private boolean admit(PageEntry page, long gameTick) {
        int sectionX = SectionPos.x(page.sectionKey);
        int sectionY = SectionPos.y(page.sectionKey);
        int sectionZ = SectionPos.z(page.sectionKey);
        LevelChunk chunk = level.getChunkSource().getChunkNow(
                sectionX, sectionZ);
        if (chunk == null) {
            return false;
        }
        int sectionIndex = chunk.getSectionIndexFromSectionY(sectionY);
        if (sectionIndex < 0
                || sectionIndex >= chunk.getSections().length) {
            return false;
        }
        LevelChunkSection section = chunk.getSections()[sectionIndex];
        PageSignatures signatureCut =
                signatures.capturePage(page.sectionKey, section);
        long lifecycleGeneration = nextLifecycleGeneration;
        nextLifecycleGeneration = Math.incrementExact(
                nextLifecycleGeneration);
        ThermalPageHandle handle = new ThermalPageHandle(
                page.sectionKey, lifecycleGeneration);
        MinecraftEnvironmentCapture.Captured captured =
                environment.capture(page.sectionKey, chunk);
        accumulator.admit(
                handle,
                handle.liveGeometryRevision(),
                signatureCut,
                captured.naturalTemperatureC(),
                captured.firstExposedLocalY(),
                input.dormantAdmissionCut(
                        page.sectionKey,
                        captured.naturalTemperatureC(),
                        gameTick));
        page.handle = handle;
        environment.track(handle, captured, gameTick);
        pagesByChunk.computeIfAbsent(
                chunk.getPos().toLong(),
                ignored -> new LongOpenHashSet()).add(page.sectionKey);
        ensureSectionOwner(chunk, sectionIndex, section);
        publishPageHandle(page.sectionKey, handle);
        if (physicalSources != null
                && sourceScannedChunks.add(chunk.getPos().toLong())) {
            physicalSources.onChunkLoad(chunk);
        }
        return true;
    }

    private void processCaptures(long gameTick) {
        int fullRemaining = MAX_FULL_RESYNCS_PER_TICK;
        int centerRemaining = MAX_CENTERS_PER_TICK;
        int attempts = captureQueue.size();
        while (attempts-- > 0 && !captureQueue.isEmpty()
                && (fullRemaining > 0 || centerRemaining > 0)) {
            long sectionKey = captureQueue.removeFirstLong();
            PageEntry page = pages.get(sectionKey);
            if (page == null || page.handle == null) {
                continue;
            }
            if (page.lastMutationTick == gameTick
                    && gameTick % ThermalInputBatch.CUT_INTERVAL_TICKS != 0L) {
                captureQueue.add(sectionKey);
                continue;
            }
            if (page.fullResync) {
                if (fullRemaining == 0) {
                    captureQueue.add(sectionKey);
                    continue;
                }
                SectionOwner owner = ownersBySection.get(sectionKey);
                PageSignatures snapshot = signatures.capturePage(
                        sectionKey, owner == null ? null : owner.section);
                page.fullResync = false;
                page.fullSignatureCut = snapshot;
                readyCaptures.add(sectionKey);
                fullRemaining--;
                continue;
            }
            while (centerRemaining > 0) {
                int center = page.nextUnresolvedCenter();
                if (center < 0) {
                    break;
                }
                int minX = SectionPos.sectionToBlockCoord(
                        SectionPos.x(sectionKey));
                int minY = SectionPos.sectionToBlockCoord(
                        SectionPos.y(sectionKey));
                int minZ = SectionPos.sectionToBlockCoord(
                        SectionPos.z(sectionKey));
                page.signatureIds[center] = signatures.resolveSignatureId(
                        minX + (page.centers[center] & 15),
                        minY + (page.centers[center] >>> 8 & 15),
                        minZ + (page.centers[center] >>> 4 & 15));
                centerRemaining--;
            }
            if (page.nextUnresolvedCenter() >= 0) {
                captureQueue.add(sectionKey);
                continue;
            }
            readyCaptures.add(sectionKey);
        }
    }

    private void drainMutations(long gameTick) {
        SectionOwner owner;
        while ((owner = dirtyOwners.poll()) != null) {
            owner.enqueued.set(false);
            boolean deferredGeometry = owner.takeDirty(mutationScratch);
            boolean sourceRelevant = mutationScratch.sourceRelevant;
            if (!owner.valid) {
                mutationScratch.clear();
                continue;
            }
            boolean fullResync = owner.fullResync.getAndSet(false);
            if (fullResync) {
                ThermalPageHandle.GeometryResyncReason reason =
                        owner.fullResyncReason;
                ThermalPageHandle handle = owner.page;
                if (owner.deferredFullResync.compareAndSet(true, false)) {
                    if (handle != null) {
                        handle.requireFullGeometryResync(reason);
                    }
                }
                if (handle != null) {
                    PageEntry page = pages.get(handle.sectionKey());
                    if (page != null && page.handle == handle) {
                        page.requireFullCapture(
                                handle.liveGeometryRevision(),
                                gameTick,
                                reason);
                        readyCaptures.remove(page.sectionKey);
                        captureQueue.add(page.sectionKey);
                    }
                }
                if (physicalSources != null) {
                    physicalSources.resyncSection(
                            owner.sectionX,
                            owner.sectionY,
                            owner.sectionZ,
                            owner.section);
                }
            }
            boolean geometryChanged = false;
            for (int word = 0; word < mutationScratch.changed.length; word++) {
                long remaining = mutationScratch.changed[word];
                long geometry = remaining
                        & ~mutationScratch.nonGeometry[word];
                while (remaining != 0L) {
                    int bit = Long.numberOfTrailingZeros(remaining);
                    int index = word << 6 | bit;
                    int localX = index & 15;
                    int localY = index >>> 8 & 15;
                    int localZ = index >>> 4 & 15;
                    int worldX = SectionPos.sectionToBlockCoord(owner.sectionX)
                            + localX;
                    int worldY = SectionPos.sectionToBlockCoord(owner.sectionY)
                            + localY;
                    int worldZ = SectionPos.sectionToBlockCoord(owner.sectionZ)
                            + localZ;
                    if (deferredGeometry
                            && (geometry & 1L << bit) != 0L) {
                        owner.invalidatePage();
                    }
                    if (sourceRelevant && physicalSources != null) {
                        physicalSources.resyncBlock(
                                worldX, worldY, worldZ,
                                owner.section.getBlockState(
                                        localX, localY, localZ));
                    }
                    if ((geometry & 1L << bit) != 0L) {
                        environment.markSkyColumn(worldX, worldZ);
                        if (!fullResync) collectCenter(owner, index, gameTick);
                        geometryChanged = true;
                    }
                    remaining &= remaining - 1L;
                }
                mutationScratch.changed[word] = 0L;
                mutationScratch.nonGeometry[word] = 0L;
            }
            if ((geometryChanged || fullResync)
                    && radiationOcclusion != null) {
                radiationOcclusion.onSectionMutation(
                        owner.sectionX, owner.sectionY, owner.sectionZ);
            }
        }
    }

    private void collectCenter(
            SectionOwner owner,
            int changedIndex,
            long gameTick
    ) {
        PageEntry page = pages.get(owner.sectionKey);
        if (page == null || page.handle == null) {
            return;
        }
        ThermalPageHandle.GeometryResyncToken resync =
                page.handle.pendingFullGeometryResync();
        if (resync != null) {
            page.requireFullCapture(
                    resync.requiredRevision(), gameTick, resync.reason());
        } else {
            page.markCenter(
                    changedIndex,
                    page.handle.liveGeometryRevision(),
                    gameTick);
        }
        readyCaptures.remove(page.sectionKey);
        captureQueue.add(page.sectionKey);
    }

    private void retain(long sectionKey, int priority) {
        PageEntry page = entry(sectionKey);
        if (priority == PRIMARY) {
            page.playerReferences++;
        } else if (priority == SOURCE) {
            page.sourceReferences++;
        } else {
            page.continuationReferences++;
        }
        enqueue(page);
    }

    private void release(long sectionKey, int priority) {
        PageEntry page = pages.get(sectionKey);
        if (page == null) return;
        if (priority == PRIMARY && page.playerReferences > 0) {
            page.playerReferences--;
        } else if (priority == SOURCE && page.sourceReferences > 0) {
            page.sourceReferences--;
        } else if (priority == CONTINUATION
                && page.continuationReferences > 0) {
            page.continuationReferences--;
        }
        if (page.playerReferences == 0 && page.sourceReferences == 0) {
            clearContinuations(page);
        }
        if (!page.interested()) {
            if (page.handle != null) retire(page);
            if (page.queuedPriority >= 0) {
                admissionQueues[page.queuedPriority].remove(page.sectionKey);
            }
            pages.remove(page.sectionKey);
            LongOpenHashSet indexed = pagesByChunk.get(chunkKey(page.sectionKey));
            if (indexed != null) {
                indexed.remove(page.sectionKey);
                if (indexed.isEmpty()) {
                    pagesByChunk.remove(chunkKey(page.sectionKey));
                }
            }
        } else {
            enqueue(page);
        }
    }

    private void retire(PageEntry page) {
        input.captureDormantPage(page.handle, true);
        clearContinuations(page);
        accumulator.retire(page.handle);
        environment.untrack(page.handle);
        captureQueue.remove(page.sectionKey);
        readyCaptures.remove(page.sectionKey);
        page.handle = null;
        page.resetCapture();
        publishPageHandle(page.sectionKey, null);
    }

    public void checkpointChunk(
            LevelChunk chunk,
            boolean markDirty,
            boolean refreshSupport
    ) {
        requireMainThread();
        LongOpenHashSet indexed = pagesByChunk.get(chunk.getPos().toLong());
        if (indexed != null) {
            for (long sectionKey : indexed) {
                PageEntry page = pages.get(sectionKey);
                if (page != null && page.handle != null) {
                    input.captureDormantPage(page.handle, chunk, markDirty);
                }
            }
        }
        if (refreshSupport) {
            input.finishDormantCheckpoint(chunk, markDirty);
        }
    }

    public void checkpointAll(boolean markDirty, boolean refreshSupport) {
        requireMainThread();
        long[] chunks = pagesByChunk.keySet().toLongArray();
        for (long chunkKey : chunks) {
            LevelChunk chunk = level.getChunkSource().getChunkNow(
                    (int) chunkKey,
                    (int) (chunkKey >>> Integer.SIZE));
            if (chunk != null) {
                checkpointChunk(chunk, markDirty, refreshSupport);
            }
        }
    }

    private void clearContinuations(PageEntry page) {
        int mask = page.continuationFaceMask;
        page.continuationFaceMask = 0;
        for (int face = 0; face < 6; face++) {
            if ((mask & 1 << face) != 0) {
                release(neighbor(page.sectionKey, face), CONTINUATION);
            }
        }
    }

    private void enqueue(PageEntry page) {
        if (page.handle != null || !page.interested()) {
            return;
        }
        int priority = page.priority();
        if (page.queuedPriority == priority) {
            return;
        }
        if (page.queuedPriority >= 0) {
            admissionQueues[page.queuedPriority].remove(page.sectionKey);
        }
        admissionQueues[priority].add(page.sectionKey);
        page.queuedPriority = priority;
    }

    private PageEntry entry(long sectionKey) {
        PageEntry page = pages.get(sectionKey);
        if (page != null) {
            return page;
        }
        page = new PageEntry(sectionKey);
        pages.put(sectionKey, page);
        pagesByChunk.computeIfAbsent(
                chunkKey(sectionKey),
                ignored -> new LongOpenHashSet()).add(sectionKey);
        return page;
    }

    private void expirePlayers(long tick) {
        int bucket = (int) tick & (WHEEL_SIZE - 1);
        PlayerLease lease = playerWheel[bucket];
        playerWheel[bucket] = null;
        while (lease != null) {
            PlayerLease next = lease.next;
            lease.previous = null;
            lease.next = null;
            lease.bucket = -1;
            if (lease.previousSection != Long.MIN_VALUE
                    && lease.previousExpiry <= tick) {
                release(lease.previousSection, PRIMARY);
                lease.previousSection = Long.MIN_VALUE;
            }
            if (lease.currentExpiry <= tick) {
                playerLeases.remove(lease.playerId);
                release(lease.currentSection, PRIMARY);
            } else {
                schedulePlayer(lease);
            }
            lease = next;
        }
    }

    private void schedulePlayer(PlayerLease lease) {
        unlinkPlayer(lease);
        long expiry = lease.previousSection == Long.MIN_VALUE
                ? lease.currentExpiry
                : Math.min(lease.currentExpiry, lease.previousExpiry);
        int bucket = (int) expiry & (WHEEL_SIZE - 1);
        lease.next = playerWheel[bucket];
        if (lease.next != null) {
            lease.next.previous = lease;
        }
        playerWheel[bucket] = lease;
        lease.bucket = bucket;
    }

    private void unlinkPlayer(PlayerLease lease) {
        if (lease.bucket < 0) {
            return;
        }
        if (lease.previous == null) {
            playerWheel[lease.bucket] = lease.next;
        } else {
            lease.previous.next = lease.next;
        }
        if (lease.next != null) {
            lease.next.previous = lease.previous;
        }
        lease.previous = null;
        lease.next = null;
        lease.bucket = -1;
    }

    private void attachSection(
            LevelChunk chunk,
            int sectionIndex,
            LevelChunkSection section
    ) {
        int sectionY = chunk.getSectionYFromSectionIndex(sectionIndex);
        long key = SectionPos.asLong(
                chunk.getPos().x, sectionY, chunk.getPos().z);
        SectionOwner owner = new SectionOwner(
                this,
                chunk,
                section,
                key,
                chunk.getPos().x,
                sectionY,
                chunk.getPos().z);
        SectionOwner previous = ownersByIdentity.put(section, owner);
        if (previous != null) {
            ownersBySection.remove(previous.sectionKey);
            previous.invalidate();
        }
        ownersBySection.put(key, owner);
        attachment(section).frostedheart$setThermalInputOwner(owner);
        PageEntry page = pages.get(key);
        owner.page = page == null ? null : page.handle;
    }

    private void ensureSectionOwner(
            LevelChunk chunk,
            int sectionIndex,
            LevelChunkSection section
    ) {
        if (!ownersByIdentity.containsKey(section)) {
            attachSection(chunk, sectionIndex, section);
        }
    }

    private void publishPageHandle(
            long sectionKey,
            ThermalPageHandle page
    ) {
        SectionOwner owner = ownersBySection.get(sectionKey);
        if (owner != null) {
            owner.page = page;
        }
    }

    public SectionOwner loadedSectionOrAttach(long sectionKey) {
        requireMainThread();
        SectionOwner owner = ownersBySection.get(sectionKey);
        if (owner != null && owner.valid) {
            return owner;
        }
        LevelChunk chunk = level.getChunkSource().getChunkNow(
                SectionPos.x(sectionKey), SectionPos.z(sectionKey));
        if (chunk == null) {
            return null;
        }
        int sectionIndex = chunk.getSectionIndexFromSectionY(
                SectionPos.y(sectionKey));
        if (sectionIndex < 0
                || sectionIndex >= chunk.getSections().length) {
            return null;
        }
        ensureSectionOwner(
                chunk, sectionIndex, chunk.getSections()[sectionIndex]);
        return ownersBySection.get(sectionKey);
    }

    private static MinecraftThermalSectionAttachment attachment(
            LevelChunkSection section
    ) {
        return (MinecraftThermalSectionAttachment) (Object) section;
    }

    private static long chunkKey(long sectionKey) {
        return ChunkPos.asLong(
                SectionPos.x(sectionKey), SectionPos.z(sectionKey));
    }

    private static long neighbor(long sectionKey, int face) {
        int x = SectionPos.x(sectionKey);
        int y = SectionPos.y(sectionKey);
        int z = SectionPos.z(sectionKey);
        return switch (face) {
            case 0 -> SectionPos.asLong(x - 1, y, z);
            case 1 -> SectionPos.asLong(x + 1, y, z);
            case 2 -> SectionPos.asLong(x, y - 1, z);
            case 3 -> SectionPos.asLong(x, y + 1, z);
            case 4 -> SectionPos.asLong(x, y, z - 1);
            default -> SectionPos.asLong(x, y, z + 1);
        };
    }

    private void requireMainThread() {
        if (Thread.currentThread() != mainThread || closed) {
            throw new IllegalStateException(closed
                    ? "Minecraft Page manager is closed"
                    : "Minecraft Page manager requires the level thread");
        }
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        for (SectionOwner owner : ownersByIdentity.values()) {
            owner.invalidate();
        }
        ownersByIdentity.clear();
        ownersBySection.clear();
        pages.clear();
        pagesByChunk.clear();
        captureQueue.clear();
        readyCaptures.clear();
        playerLeases.clear();
        sourceScannedChunks.clear();
        sourceRecoveryRemaining = 0;
        for (LongLinkedOpenHashSet queue : admissionQueues) {
            queue.clear();
        }
    }

    public static final class SectionOwner {
        private final MinecraftPageManager manager;
        private final LevelChunk chunk;
        private final LevelChunkSection section;
        private final long sectionKey;
        private final int sectionX;
        private final int sectionY;
        private final int sectionZ;
        private final AtomicBoolean enqueued = new AtomicBoolean();
        private final AtomicBoolean fullResync = new AtomicBoolean();
        private final AtomicBoolean deferredFullResync = new AtomicBoolean();
        private volatile ThermalPageHandle.GeometryResyncReason
                fullResyncReason =
                ThermalPageHandle.GeometryResyncReason.EXPLICIT_INVALIDATION;
        private long[] pending;
        private long[] pendingNonGeometry;
        private boolean pendingSourceMutation;
        private volatile ThermalPageHandle page;
        private volatile boolean valid = true;

        private SectionOwner(
                MinecraftPageManager manager,
                LevelChunk chunk,
                LevelChunkSection section,
                long sectionKey,
                int sectionX,
                int sectionY,
                int sectionZ
        ) {
            this.manager = manager;
            this.chunk = chunk;
            this.section = section;
            this.sectionKey = sectionKey;
            this.sectionX = sectionX;
            this.sectionY = sectionY;
            this.sectionZ = sectionZ;
        }

        public LevelChunk chunk() {
            return chunk;
        }

        public ThermalPageHandle page() {
            return page;
        }

        void record(
                int localX,
                int localY,
                int localZ,
                boolean topologyRelevant,
                boolean sourceRelevant
        ) {
            if (!valid) {
                return;
            }
            int index = localX | localZ << 4 | localY << 8;
            int word = index >>> 6;
            long bit = 1L << (index & 63);
            boolean invalidate = false;
            synchronized (this) {
                if (pending == null) {
                    pending = new long[64];
                }
                boolean firstForPosition = (pending[word] & bit) == 0L;
                pending[word] |= bit;
                pendingSourceMutation |= sourceRelevant;
                if (topologyRelevant) {
                    boolean geometryPending = !firstForPosition
                            && (pendingNonGeometry == null
                            || (pendingNonGeometry[word] & bit) == 0L);
                    if (!geometryPending) {
                        if (pendingNonGeometry != null) {
                            pendingNonGeometry[word] &= ~bit;
                        }
                        if (Thread.currentThread() == manager.mainThread) {
                            invalidate = true;
                        } else {
                            deferredGeometryInvalidation = true;
                        }
                    }
                } else if (firstForPosition) {
                    if (pendingNonGeometry == null) {
                        pendingNonGeometry = new long[64];
                    }
                    pendingNonGeometry[word] |= bit;
                }
            }
            if (invalidate) {
                invalidatePage();
            }
            if (enqueued.compareAndSet(false, true)) {
                manager.dirtyOwners.add(this);
            }
        }

        public void recordFullResync(
                ThermalPageHandle.GeometryResyncReason reason
        ) {
            if (!valid) {
                return;
            }
            fullResyncReason = reason;
            fullResync.set(true);
            if (Thread.currentThread() == manager.mainThread) {
                ThermalPageHandle current = page;
                if (current != null) {
                    current.requireFullGeometryResync(reason);
                }
            } else {
                deferredFullResync.set(true);
            }
            if (enqueued.compareAndSet(false, true)) {
                manager.dirtyOwners.add(this);
            }
        }

        public MinecraftThermalInput input() {
            return manager.input;
        }

        private void invalidatePage() {
            ThermalPageHandle current = page;
            if (current != null) {
                current.beginGeometryMutation();
            }
        }

        private boolean deferredGeometryInvalidation;

        private synchronized boolean takeDirty(MutationScratch scratch) {
            if (pending != null) {
                long[] changed = scratch.changed;
                scratch.changed = pending;
                pending = changed;
                if (pendingNonGeometry != null) {
                    long[] nonGeometry = scratch.nonGeometry;
                    scratch.nonGeometry = pendingNonGeometry;
                    pendingNonGeometry = nonGeometry;
                }
            }
            scratch.sourceRelevant = pendingSourceMutation;
            pendingSourceMutation = false;
            boolean deferred = deferredGeometryInvalidation;
            deferredGeometryInvalidation = false;
            return deferred;
        }

        private void invalidate() {
            valid = false;
            page = null;
            MinecraftThermalSectionAttachment attachment =
                    attachment(section);
            if (attachment.frostedheart$getThermalInputOwner() == this) {
                attachment.frostedheart$setThermalInputOwner(null);
            }
        }
    }

    private static final class MutationScratch {
        private long[] changed = new long[64];
        private long[] nonGeometry = new long[64];
        private boolean sourceRelevant;

        private void clear() {
            Arrays.fill(changed, 0L);
            Arrays.fill(nonGeometry, 0L);
            sourceRelevant = false;
        }
    }

    private static final class PageEntry {
        private final long sectionKey;
        private ThermalPageHandle handle;
        private int playerReferences;
        private int sourceReferences;
        private int continuationReferences;
        private int continuationFaceMask;
        private int queuedPriority = -1;
        private short[] centers;
        private int[] signatureIds;
        private long[] centerPresence;
        private int centerCount;
        private long captureRevision;
        private boolean fullResync;
        private ThermalPageHandle.GeometryResyncReason resyncReason;
        private PageSignatures fullSignatureCut;
        private long lastMutationTick = -1L;
        private long retryAfterTick;

        private PageEntry(long sectionKey) {
            this.sectionKey = sectionKey;
        }

        private boolean interested() {
            return playerReferences != 0 || sourceReferences != 0
                    || continuationReferences != 0;
        }

        private int priority() {
            return playerReferences != 0 ? PRIMARY
                    : sourceReferences != 0 ? SOURCE : CONTINUATION;
        }

        private void markCenter(
                int blockIndex,
                long revision,
                long tick
        ) {
            captureRevision = revision;
            lastMutationTick = tick;
            if (centerPresence == null
                    || (centerPresence[blockIndex >>> 6]
                    & 1L << blockIndex) != 0L) {
                for (int index = 0; index < centerCount; index++) {
                    if (Short.toUnsignedInt(centers[index]) == blockIndex) {
                        signatureIds[index] = UNCAPTURED_SIGNATURE;
                        return;
                    }
                }
            }
            if (centers == null) {
                centers = new short[INITIAL_CENTER_CAPACITY];
                signatureIds = new int[INITIAL_CENTER_CAPACITY];
            } else if (centerCount == centers.length) {
                int capacity = centers.length + (centers.length >>> 1);
                centers = Arrays.copyOf(centers, capacity);
                signatureIds = Arrays.copyOf(signatureIds, capacity);
            }
            centers[centerCount] = (short) blockIndex;
            signatureIds[centerCount] = UNCAPTURED_SIGNATURE;
            centerCount++;
            if (centerPresence == null
                    && centerCount >= CENTER_BITMAP_THRESHOLD) {
                centerPresence = new long[64];
                for (int index = 0; index < centerCount; index++) {
                    int present = Short.toUnsignedInt(centers[index]);
                    centerPresence[present >>> 6] |= 1L << present;
                }
            } else if (centerPresence != null) {
                centerPresence[blockIndex >>> 6] |= 1L << blockIndex;
            }
            if (centerCount > SPARSE_CENTER_LIMIT) {
                captureRevision = handle.requireFullGeometryResync(
                        ThermalPageHandle.GeometryResyncReason
                                .CAPTURE_INCOMPLETE);
                fullResync = true;
                resyncReason = ThermalPageHandle.GeometryResyncReason
                        .CAPTURE_INCOMPLETE;
                fullSignatureCut = null;
                centerCount = 0;
                Arrays.fill(centerPresence, 0L);
            }
        }

        private void requireFullCapture(
                long revision,
                long tick,
                ThermalPageHandle.GeometryResyncReason reason
        ) {
            captureRevision = revision;
            lastMutationTick = tick;
            fullResync = true;
            resyncReason = reason;
            fullSignatureCut = null;
            centerCount = 0;
        }

        private int nextUnresolvedCenter() {
            for (int index = 0; index < centerCount; index++) {
                if (signatureIds[index] == UNCAPTURED_SIGNATURE) {
                    return index;
                }
            }
            return -1;
        }

        private void resetCapture() {
            centerCount = 0;
            if (centerPresence != null) {
                Arrays.fill(centerPresence, 0L);
            }
            fullResync = false;
            resyncReason = null;
            fullSignatureCut = null;
            lastMutationTick = -1L;
        }
    }

    private static final class PlayerLease {
        private final UUID playerId;
        private long currentSection;
        private long previousSection = Long.MIN_VALUE;
        private long currentExpiry;
        private long previousExpiry;
        private PlayerLease previous;
        private PlayerLease next;
        private int bucket = -1;

        private PlayerLease(UUID playerId) {
            this.playerId = playerId;
        }
    }
}
