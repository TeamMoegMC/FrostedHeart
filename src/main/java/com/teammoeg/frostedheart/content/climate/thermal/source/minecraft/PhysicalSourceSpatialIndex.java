/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.source.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.radiation.RadiationService;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.DimensionInputAccumulator;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftPageManager;
import com.teammoeg.frostedheart.content.climate.thermal.source.EmissionPort;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.MinecraftPhysicalSourceProfile.Port;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.MinecraftPhysicalSourceProfile.PortKind;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceMode;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.ChunkPos;

import java.util.Arrays;
import java.util.Objects;

/**
 * 主线程物理热源的 SoA 存储与 section-local 反向索引。
 *
 * <p>它读取已加载的 BlockEntity/BlockState，将 source 变化编码进下一份
 * input batch；目标 arena slot 只由 worker 侧
 * {@link WorkerPhysicalSourceBindings} 解析。</p>
 */
public final class PhysicalSourceSpatialIndex
        implements AutoCloseable, RadiationService.SourceIndex {
    private static final int NO_SLOT = -1;
    private static final int MAX_PORTS = 3;
    private static final byte PRESENT = 1;
    private static final byte REGISTERED = 1 << 1;
    private static final byte REGISTRATION_STALE = 1 << 2;
    private static final byte DESIRED_ENABLED = 1 << 3;
    private static final byte OFFERED_ENABLED = 1 << 4;
    private static final byte TARGETS_CHANGED = 1 << 5;
    private static final byte DIRTY_QUEUED = 1 << 6;
    private static final byte CAMPFIRE_PROFILE_ID =
            (byte) MinecraftPhysicalSourceProfile.CAMPFIRE.profileId();
    private static final byte GENERATOR_PROFILE_ID =
            (byte) MinecraftPhysicalSourceProfile.GENERATOR.profileId();

    private DimensionInputAccumulator accumulator;
    private final MinecraftPageManager pages;
    private final MinecraftPhysicalSourceProfile campfireProfile;
    private final Long2IntOpenHashMap slotsById = new Long2IntOpenHashMap();
    private final Long2ObjectOpenHashMap<IntOpenHashSet> sourcesByOriginSection =
            new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<IntOpenHashSet> sourcesByOriginChunk =
            new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<IntOpenHashSet> sourcesByTargetSection =
            new Long2ObjectOpenHashMap<>();
    private final IntArrayList dirtyOrder = new IntArrayList();
    private final int maximumSources;

    private long[] sourceIds;
    private int[] lifecycleGenerations;
    private int[] anchorX;
    private int[] anchorY;
    private int[] anchorZ;
    private byte[] profileIds;
    private long[] radiationRevisions;
    private double[] desiredPowerW;
    private double[] offeredPowerW;
    private byte[] sourceFlags;
    private int[] nextFree;
    private byte[] targetCount;
    private long[] targetSections;
    private int highWaterMark;
    private int freeHead = NO_SLOT;
    private int nextLifecycleGeneration;
    private long nextRadiationRevision;
    private boolean capacityRecoveryPending;
    private boolean closed;

    public PhysicalSourceSpatialIndex(
            DimensionInputAccumulator accumulator,
            MinecraftPageManager pages,
            MinecraftPhysicalSourceProfile campfireProfile,
            int initialCapacity,
            int maximumSources
    ) {
        if (initialCapacity < 0 || maximumSources <= 0
                || initialCapacity > maximumSources) {
            throw new IllegalArgumentException(
                    "source index capacity is negative");
        }
        this.accumulator = accumulator;
        this.pages = pages;
        this.campfireProfile = Objects.requireNonNull(
                campfireProfile, "campfireProfile");
        if (campfireProfile.profileId()
                != Byte.toUnsignedInt(CAMPFIRE_PROFILE_ID)) {
            throw new IllegalArgumentException(
                    "configured campfire profile has the wrong identity");
        }
        this.maximumSources = maximumSources;
        slotsById.defaultReturnValue(NO_SLOT);
        allocate(Math.max(1, initialCapacity));
    }

    public void observeMachine(
            BlockPos source,
            BlockPos anchor,
            MinecraftPhysicalSourceProfile profile,
            double level,
            boolean active
    ) {
        observe(
                source.getX(), source.getY(), source.getZ(),
                anchor.getX(), anchor.getY(), anchor.getZ(),
                profile,
                profile.powerForLevel(level),
                active);
    }

    public void resyncBlock(int x, int y, int z, BlockState state) {
        if (isCampfire(state)) {
            observeCampfire(x, y, z, state);
        } else {
            int slot = slotsById.get(BlockPos.asLong(x, y, z));
            if (slot != NO_SLOT && isLive(slot)
                    && profileIds[slot] == CAMPFIRE_PROFILE_ID) {
                remove(x, y, z);
            }
        }
    }

    public void resyncSection(
            int sectionX,
            int sectionY,
            int sectionZ,
            LevelChunkSection section
    ) {
        IntOpenHashSet indexed = sourcesByOriginSection.get(
                SectionPos.asLong(sectionX, sectionY, sectionZ));
        if (indexed != null) {
            for (int slot : indexed) {
                if (isLive(slot) && flag(slot, PRESENT)
                        && profileIds[slot] == CAMPFIRE_PROFILE_ID) {
                    setFlag(slot, PRESENT, false);
                    markDirty(slot);
                }
            }
        }
        int minX = SectionPos.sectionToBlockCoord(sectionX);
        int minY = SectionPos.sectionToBlockCoord(sectionY);
        int minZ = SectionPos.sectionToBlockCoord(sectionZ);
        for (int localY = 0; localY < 16; localY++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                for (int localX = 0; localX < 16; localX++) {
                    BlockState state = section.getBlockState(
                            localX, localY, localZ);
                    if (isCampfire(state)) {
                        observeCampfire(
                                minX + localX, minY + localY, minZ + localZ,
                                state);
                    }
                }
            }
        }
    }

    public void onChunkLoad(LevelChunk chunk) {
        for (BlockEntity entity : chunk.getBlockEntities().values()) {
            if (!(entity instanceof CampfireBlockEntity)
                    || !isCampfire(entity.getBlockState())) {
                continue;
            }
            BlockPos position = entity.getBlockPos();
            observeCampfire(
                    position.getX(), position.getY(), position.getZ(),
                    entity.getBlockState());
        }
    }

    public void beforeChunkUnload(LevelChunk chunk, long eventTick) {
        IntOpenHashSet indexed =
                sourcesByOriginChunk.get(chunk.getPos().toLong());
        if (indexed != null) {
            for (int slot : indexed) {
                if (isLive(slot)) {
                    setFlag(slot, PRESENT, false);
                    markDirty(slot);
                }
            }
        }
        flush(eventTick);
    }

    public void replaceAccumulator(DimensionInputAccumulator next) {
        accumulator = next;
    }

    public void reseedAll(long eventTick) {
        for (int slot = 0; slot < highWaterMark; slot++) {
            if (isLive(slot) && flag(slot, PRESENT)) {
                setFlag(slot, REGISTERED, false);
                setFlag(slot, REGISTRATION_STALE, false);
                markDirty(slot);
            }
        }
        flush(eventTick);
    }

    public void flush(long eventTick) {
        int dirtyCount = dirtyOrder.size();
        Arrays.sort(dirtyOrder.elements(), 0, dirtyCount);
        for (int dirtyIndex = 0; dirtyIndex < dirtyCount; dirtyIndex++) {
            int slot = dirtyOrder.getInt(dirtyIndex);
            sourceFlags[slot] &= ~DIRTY_QUEUED;
            if (!isLive(slot)) {
                continue;
            }
            if (!flag(slot, PRESENT)) {
                if (flag(slot, REGISTERED)) {
                    accumulator.unloadSource(
                            sourceIds[slot],
                            lifecycleGenerations[slot],
                            eventTick);
                }
                releaseTargets(slot);
                unlinkOrigin(slot);
                slotsById.remove(sourceIds[slot]);
                recycle(slot);
                continue;
            }
            if (flag(slot, REGISTRATION_STALE)
                    && flag(slot, REGISTERED)) {
                accumulator.unloadSource(
                        sourceIds[slot],
                        lifecycleGenerations[slot],
                        eventTick);
                setFlag(slot, REGISTERED, false);
                setFlag(slot, REGISTRATION_STALE, false);
                lifecycleGenerations[slot] = nextGeneration();
            }
            if (flag(slot, TARGETS_CHANGED)) {
                refreshTargets(slot);
                sourceFlags[slot] &= ~TARGETS_CHANGED;
            }
            if (!flag(slot, REGISTERED)) {
                MinecraftPhysicalSourceProfile profile = profile(slot);
                EmissionPort[] ports =
                        WorkerPhysicalSourceBindings.initialPorts(
                                sourceIds[slot], profile);
                accumulator.registerSource(
                        sourceIds[slot],
                        lifecycleGenerations[slot],
                        ThermalSourceMode.POWER_SOURCE,
                        desiredPowerW[slot],
                        flag(slot, DESIRED_ENABLED),
                        eventTick,
                        anchorX[slot],
                        anchorY[slot],
                        anchorZ[slot],
                        profile.profileId(),
                        ports);
                setFlag(slot, REGISTERED, true);
                offeredPowerW[slot] = desiredPowerW[slot];
                setFlag(
                        slot, OFFERED_ENABLED,
                        flag(slot, DESIRED_ENABLED));
                continue;
            }
            if (Double.compare(
                    offeredPowerW[slot], desiredPowerW[slot]) != 0) {
                accumulator.changeSourcePower(
                        sourceIds[slot], desiredPowerW[slot], eventTick);
                offeredPowerW[slot] = desiredPowerW[slot];
            }
            if (flag(slot, OFFERED_ENABLED)
                    != flag(slot, DESIRED_ENABLED)) {
                accumulator.changeSourceEnabled(
                        sourceIds[slot],
                        flag(slot, DESIRED_ENABLED),
                        eventTick);
                setFlag(
                        slot, OFFERED_ENABLED,
                        flag(slot, DESIRED_ENABLED));
            }
        }
        dirtyOrder.clear();
    }

    public boolean capacityRecoveryPending() {
        return capacityRecoveryPending;
    }

    public boolean hasAvailableCapacity() {
        return freeHead != NO_SLOT || highWaterMark < maximumSources;
    }

    public void beginCapacityRecoveryPass() {
        capacityRecoveryPending = false;
    }

    public void continueCapacityRecoveryPass() {
        capacityRecoveryPending = true;
    }

    public BlockPos nearestEnabledGenerator(
            BlockPos position,
            double maximumDistanceSqr
    ) {
        int radius = (int) Math.ceil(
                Math.sqrt(maximumDistanceSqr) / 16.0D);
        int centerX = SectionPos.blockToSectionCoord(position.getX());
        int centerY = SectionPos.blockToSectionCoord(position.getY());
        int centerZ = SectionPos.blockToSectionCoord(position.getZ());
        int nearestSlot = NO_SLOT;
        double nearestDistance = maximumDistanceSqr;
        for (int y = centerY - radius; y <= centerY + radius; y++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                for (int x = centerX - radius; x <= centerX + radius; x++) {
                    IntOpenHashSet indexed = sourcesByOriginSection.get(
                            SectionPos.asLong(x, y, z));
                    if (indexed == null) {
                        continue;
                    }
                    for (int slot : indexed) {
                        if (!isLive(slot) || !flag(slot, PRESENT)
                                || !flag(slot, DESIRED_ENABLED)
                                || desiredPowerW[slot] <= 0.0D
                                || profileIds[slot] != GENERATOR_PROFILE_ID) {
                            continue;
                        }
                        double dx = BlockPos.getX(sourceIds[slot]) - position.getX();
                        double dy = BlockPos.getY(sourceIds[slot]) - position.getY();
                        double dz = BlockPos.getZ(sourceIds[slot]) - position.getZ();
                        double distance = dx * dx + dy * dy + dz * dz;
                        if (distance <= nearestDistance) {
                            nearestDistance = distance;
                            nearestSlot = slot;
                        }
                    }
                }
            }
        }
        return nearestSlot < 0 ? null : BlockPos.of(sourceIds[nearestSlot]);
    }

    public boolean supportsDormantSection(long sectionKey) {
        if (enabledTarget(sectionKey)) {
            return true;
        }
        int x = SectionPos.x(sectionKey);
        int y = SectionPos.y(sectionKey);
        int z = SectionPos.z(sectionKey);
        return enabledTarget(SectionPos.asLong(x - 1, y, z))
                || enabledTarget(SectionPos.asLong(x + 1, y, z))
                || enabledTarget(SectionPos.asLong(x, y - 1, z))
                || enabledTarget(SectionPos.asLong(x, y + 1, z))
                || enabledTarget(SectionPos.asLong(x, y, z - 1))
                || enabledTarget(SectionPos.asLong(x, y, z + 1));
    }

    private boolean enabledTarget(long sectionKey) {
        IntOpenHashSet indexed = sourcesByTargetSection.get(sectionKey);
        if (indexed == null) {
            return false;
        }
        for (int slot : indexed) {
            if (isLive(slot) && flag(slot, PRESENT)
                    && flag(slot, DESIRED_ENABLED)
                    && desiredPowerW[slot] > 0.0D) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void visitSection(
            int sectionX,
            int sectionY,
            int sectionZ,
            RadiationService.SourceVisitor visitor
    ) {
        IntOpenHashSet indexed = sourcesByOriginSection.get(
                SectionPos.asLong(sectionX, sectionY, sectionZ));
        if (indexed == null) {
            return;
        }
        for (int slot : indexed) {
            if (!isLive(slot) || !flag(slot, PRESENT)
                    || !flag(slot, DESIRED_ENABLED)
                    || desiredPowerW[slot] <= 0.0D) {
                continue;
            }
            MinecraftPhysicalSourceProfile profile = profile(slot);
            double radiativePower = profile.radiativePowerW(
                    desiredPowerW[slot]);
            if (radiativePower <= 0.0D) {
                continue;
            }
            if (!visitor.visit(
                    sourceIds[slot],
                    radiationRevisions[slot],
                    BlockPos.getX(sourceIds[slot]) + profile.radiationOffsetX(),
                    BlockPos.getY(sourceIds[slot]) + profile.radiationOffsetY(),
                    BlockPos.getZ(sourceIds[slot]) + profile.radiationOffsetZ(),
                    radiativePower,
                    profile.radiationDirectionalUpperBound())) {
                return;
            }
        }
    }

    private void observe(
            int x,
            int y,
            int z,
            int targetX,
            int targetY,
            int targetZ,
            MinecraftPhysicalSourceProfile profile,
            double powerW,
            boolean enabled
    ) {
        requireOpen();
        long sourceId = BlockPos.asLong(x, y, z);
        int slot = slotsById.get(sourceId);
        boolean radiationChanged = false;
        if (slot == NO_SLOT) {
            slot = allocateSlot();
            if (slot == NO_SLOT) {
                capacityRecoveryPending = true;
                return;
            }
            sourceIds[slot] = sourceId;
            lifecycleGenerations[slot] = nextGeneration();
            anchorX[slot] = targetX;
            anchorY[slot] = targetY;
            anchorZ[slot] = targetZ;
            profileIds[slot] = (byte) profile.profileId();
            setFlag(slot, PRESENT, true);
            sourceFlags[slot] |= TARGETS_CHANGED;
            slotsById.put(sourceId, slot);
            indexOrigin(slot);
            radiationChanged = true;
        } else if (profileIds[slot] != (byte) profile.profileId()
                || anchorX[slot] != targetX
                || anchorY[slot] != targetY
                || anchorZ[slot] != targetZ) {
            releaseTargets(slot);
            profileIds[slot] = (byte) profile.profileId();
            anchorX[slot] = targetX;
            anchorY[slot] = targetY;
            anchorZ[slot] = targetZ;
            setFlag(
                    slot, REGISTRATION_STALE,
                    flag(slot, REGISTERED));
            sourceFlags[slot] |= TARGETS_CHANGED;
            radiationChanged = true;
        }
        boolean changed = !flag(slot, PRESENT)
                || Double.compare(desiredPowerW[slot], powerW) != 0
                || flag(slot, DESIRED_ENABLED) != enabled;
        setFlag(slot, PRESENT, true);
        desiredPowerW[slot] = powerW;
        setFlag(slot, DESIRED_ENABLED, enabled);
        if (changed || radiationChanged) {
            radiationRevisions[slot] =
                    Math.incrementExact(nextRadiationRevision);
            nextRadiationRevision = radiationRevisions[slot];
        }
        if (changed || flag(slot, TARGETS_CHANGED)
                || flag(slot, REGISTRATION_STALE)) {
            markDirty(slot);
        }
    }

    private void observeCampfire(
            int x,
            int y,
            int z,
            BlockState state
    ) {
        observe(
                x, y, z, x, y, z,
                campfireProfile,
                campfireProfile.ratedPowerW(),
                state.getValue(CampfireBlock.LIT));
    }

    public void remove(int x, int y, int z) {
        requireOpen();
        int slot = slotsById.get(BlockPos.asLong(x, y, z));
        if (slot != NO_SLOT && flag(slot, PRESENT)) {
            setFlag(slot, PRESENT, false);
            markDirty(slot);
        }
    }

    private void markDirty(int slot) {
        if (!flag(slot, DIRTY_QUEUED)) {
            sourceFlags[slot] |= DIRTY_QUEUED;
            dirtyOrder.add(slot);
        }
    }

    private boolean flag(int slot, byte mask) {
        return (sourceFlags[slot] & mask) != 0;
    }

    private void setFlag(int slot, byte mask, boolean value) {
        sourceFlags[slot] = (byte) (value ? sourceFlags[slot] | mask
                : sourceFlags[slot] & ~mask);
    }

    private void refreshTargets(int slot) {
        releaseTargets(slot);
        MinecraftPhysicalSourceProfile profile = profile(slot);
        int write = 0;
        for (int index = 0; index < profile.portCount(); index++) {
            Port port = profile.port(index);
            if (port.kind() != PortKind.AIR_FACE) {
                continue;
            }
            long sectionKey = SectionPos.asLong(
                    SectionPos.blockToSectionCoord(
                            anchorX[slot] + port.offsetX()),
                    SectionPos.blockToSectionCoord(
                            anchorY[slot] + port.offsetY()),
                    SectionPos.blockToSectionCoord(
                            anchorZ[slot] + port.offsetZ()));
            boolean duplicate = false;
            for (int existing = 0; existing < write; existing++) {
                duplicate |= targetSections[slot * MAX_PORTS + existing]
                        == sectionKey;
            }
            if (duplicate) {
                continue;
            }
            targetSections[slot * MAX_PORTS + write++] = sectionKey;
            pages.updateSourcePage(sectionKey, true);
            sourcesByTargetSection.computeIfAbsent(
                    sectionKey, ignored -> new IntOpenHashSet()).add(slot);
        }
        targetCount[slot] = (byte) write;
    }

    private void releaseTargets(int slot) {
        int first = slot * MAX_PORTS;
        for (int index = 0;
             index < Byte.toUnsignedInt(targetCount[slot]);
             index++) {
            long sectionKey = targetSections[first + index];
            pages.updateSourcePage(sectionKey, false);
            IntOpenHashSet indexed = sourcesByTargetSection.get(sectionKey);
            if (indexed != null) {
                indexed.remove(slot);
                if (indexed.isEmpty()) {
                    sourcesByTargetSection.remove(sectionKey);
                }
            }
            targetSections[first + index] = 0L;
        }
        targetCount[slot] = 0;
    }

    private void indexOrigin(int slot) {
        sourcesByOriginSection.computeIfAbsent(
                originSection(slot),
                ignored -> new IntOpenHashSet()).add(slot);
        sourcesByOriginChunk.computeIfAbsent(
                originChunk(slot),
                ignored -> new IntOpenHashSet()).add(slot);
    }

    private void unlinkOrigin(int slot) {
        removeIndex(sourcesByOriginSection, originSection(slot), slot);
        removeIndex(sourcesByOriginChunk, originChunk(slot), slot);
    }

    private long originSection(int slot) {
        long sourceId = sourceIds[slot];
        return SectionPos.asLong(
                SectionPos.blockToSectionCoord(BlockPos.getX(sourceId)),
                SectionPos.blockToSectionCoord(BlockPos.getY(sourceId)),
                SectionPos.blockToSectionCoord(BlockPos.getZ(sourceId)));
    }

    private long originChunk(int slot) {
        long sourceId = sourceIds[slot];
        return ChunkPos.asLong(
                SectionPos.blockToSectionCoord(BlockPos.getX(sourceId)),
                SectionPos.blockToSectionCoord(BlockPos.getZ(sourceId)));
    }

    private static void removeIndex(
            Long2ObjectOpenHashMap<IntOpenHashSet> index,
            long key,
            int slot
    ) {
        IntOpenHashSet values = index.get(key);
        if (values == null) return;
        values.remove(slot);
        if (values.isEmpty()) index.remove(key);
    }

    private MinecraftPhysicalSourceProfile profile(int slot) {
        return profileIds[slot] == CAMPFIRE_PROFILE_ID
                ? campfireProfile
                : MinecraftPhysicalSourceProfile.byId(
                        Byte.toUnsignedInt(profileIds[slot]));
    }

    private int allocateSlot() {
        if (freeHead != NO_SLOT) {
            int slot = freeHead;
            freeHead = nextFree[slot];
            nextFree[slot] = NO_SLOT;
            return slot;
        }
        if (highWaterMark >= maximumSources) {
            return NO_SLOT;
        }
        ensureCapacity(highWaterMark + 1);
        return highWaterMark++;
    }

    private void recycle(int slot) {
        sourceFlags[slot] = 0;
        sourceIds[slot] = 0L;
        lifecycleGenerations[slot] = 0;
        desiredPowerW[slot] = 0.0D;
        offeredPowerW[slot] = 0.0D;
        nextFree[slot] = freeHead;
        freeHead = slot;
    }

    private boolean isLive(int slot) {
        return slot >= 0 && slot < highWaterMark
                && lifecycleGenerations[slot] != 0;
    }

    private int nextGeneration() {
        return nextLifecycleGeneration = Math.incrementExact(
                nextLifecycleGeneration);
    }

    private void allocate(int capacity) {
        sourceIds = new long[capacity];
        lifecycleGenerations = new int[capacity];
        anchorX = new int[capacity];
        anchorY = new int[capacity];
        anchorZ = new int[capacity];
        profileIds = new byte[capacity];
        radiationRevisions = new long[capacity];
        desiredPowerW = new double[capacity];
        offeredPowerW = new double[capacity];
        sourceFlags = new byte[capacity];
        nextFree = new int[capacity];
        targetCount = new byte[capacity];
        targetSections = new long[capacity * MAX_PORTS];
        Arrays.fill(nextFree, NO_SLOT);
    }

    private void ensureCapacity(int required) {
        if (required <= sourceIds.length) {
            return;
        }
        int old = sourceIds.length;
        int capacity = Math.min(
                maximumSources,
                Math.max(required, old + Math.max(8, old >>> 1)));
        sourceIds = Arrays.copyOf(sourceIds, capacity);
        lifecycleGenerations = Arrays.copyOf(
                lifecycleGenerations, capacity);
        anchorX = Arrays.copyOf(anchorX, capacity);
        anchorY = Arrays.copyOf(anchorY, capacity);
        anchorZ = Arrays.copyOf(anchorZ, capacity);
        profileIds = Arrays.copyOf(profileIds, capacity);
        radiationRevisions = Arrays.copyOf(
                radiationRevisions, capacity);
        desiredPowerW = Arrays.copyOf(desiredPowerW, capacity);
        offeredPowerW = Arrays.copyOf(offeredPowerW, capacity);
        sourceFlags = Arrays.copyOf(sourceFlags, capacity);
        nextFree = Arrays.copyOf(nextFree, capacity);
        Arrays.fill(nextFree, old, capacity, NO_SLOT);
        targetCount = Arrays.copyOf(targetCount, capacity);
        targetSections = Arrays.copyOf(
                targetSections, capacity * MAX_PORTS);
    }

    private static boolean isCampfire(BlockState state) {
        return state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE);
    }

    private void requireOpen() {
        if (closed) throw new IllegalStateException(
                "physical source index is closed");
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        for (int slot = 0; slot < highWaterMark; slot++) {
            if (isLive(slot)) {
                releaseTargets(slot);
            }
        }
        slotsById.clear();
        sourcesByOriginSection.clear();
        sourcesByOriginChunk.clear();
        sourcesByTargetSection.clear();
        dirtyOrder.clear();
    }
}
