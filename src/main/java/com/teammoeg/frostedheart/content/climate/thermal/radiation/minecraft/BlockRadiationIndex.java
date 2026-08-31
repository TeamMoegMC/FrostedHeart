/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.radiation.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftStateThermalTable;
import com.teammoeg.frostedheart.content.climate.thermal.radiation.RadiationService;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftPageManager;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;

/** Receiver-lazy sparse aggregate index for read-only static Block radiation. */
public final class BlockRadiationIndex
        implements RadiationService.NearbySourceIndex, AutoCloseable {
    private static final int BRICK_COUNT = 64;
    private static final int BLOCKS_PER_BRICK = 64;
    private static final int MAX_BRICK_CAPTURES_PER_TICK = 64;
    private static final int DIRTY_INTERVAL_TICKS = 20;
    private static final double RANGE_BLOCKS = 8.0D;
    private static final double RANGE_SQUARED = RANGE_BLOCKS * RANGE_BLOCKS;

    private static final int NEGATIVE_X = 0;
    private static final int POSITIVE_X = 1;
    private static final int NEGATIVE_Y = 2;
    private static final int POSITIVE_Y = 3;
    private static final int NEGATIVE_Z = 4;
    private static final int POSITIVE_Z = 5;

    private static final long[] X_MASKS = new long[16];
    private static final long[] Y_MASKS = new long[16];
    private static final long[] Z_MASKS = new long[16];

    static {
        for (int selection = 0; selection < 16; selection++) {
            long xMask = 0L;
            long yMask = 0L;
            long zMask = 0L;
            for (int brick = 0; brick < BRICK_COUNT; brick++) {
                int x = brick & 3;
                int z = brick >>> 2 & 3;
                int y = brick >>> 4 & 3;
                long bit = 1L << brick;
                if ((selection & 1 << x) != 0) xMask |= bit;
                if ((selection & 1 << y) != 0) yMask |= bit;
                if ((selection & 1 << z) != 0) zMask |= bit;
            }
            X_MASKS[selection] = xMask;
            Y_MASKS[selection] = yMask;
            Z_MASKS[selection] = zMask;
        }
    }

    private final ServerLevel level;
    private final MinecraftPageManager pages;
    private final MinecraftStateThermalTable states;
    private final Predicate<BlockState> radiationPredicate;
    private final ThermalMemoryBudget.Reservation reservation;
    private final int maximumSections;
    private final Long2ObjectOpenHashMap<SectionState> coveredBySection =
            new Long2ObjectOpenHashMap<>();
    private final Long2LongOpenHashMap pendingBySection =
            new Long2LongOpenHashMap();
    private final Object dirtyLock = new Object();
    private Long2LongOpenHashMap writeDirty;
    private Long2LongOpenHashMap drainDirty;

    private final int[] profileIds = new int[BLOCKS_PER_BRICK];
    private final float[] fluidHeights = new float[BLOCKS_PER_BRICK];
    private long radiatorMask;
    private long occludingMask;
    private final long[] replacementValues = new long[BRICK_COUNT];
    private final long[] nextValues = new long[BRICK_COUNT];
    private long replacementMask;
    private final LevelChunkSection[] neighborSections =
            new LevelChunkSection[6];
    private byte neighborResolvedMask;
    private byte neighborPresentMask;

    private long currentSectionKey;
    private LevelChunk currentChunk;
    private LevelChunkSection currentSection;
    private int currentBrick;
    private boolean closed;

    private float neighborHeight;
    private boolean neighborLava;
    private boolean neighborOccluding;
    private double brickPower;
    private double brickWeightedX;
    private double brickWeightedY;
    private double brickWeightedZ;

    private BlockRadiationIndex(
            ServerLevel level,
            MinecraftPageManager pages,
            MinecraftStateThermalTable states,
            ThermalMemoryBudget.Reservation reservation,
            int maximumSections
    ) {
        this.level = Objects.requireNonNull(level, "level");
        this.pages = Objects.requireNonNull(pages, "pages");
        this.states = Objects.requireNonNull(states, "states");
        this.reservation = Objects.requireNonNull(reservation, "reservation");
        this.maximumSections = maximumSections;
        radiationPredicate = states::hasStaticRadiation;
        if (!states.radiationEnabled()) {
            throw new IllegalArgumentException(
                    "Block radiation index requires enabled profiles");
        }
    }

    public static BlockRadiationIndex tryCreate(
            ServerLevel level,
            MinecraftPageManager pages,
            MinecraftStateThermalTable states,
            ThermalMemoryBudget dimensionBudget,
            int maximumSections
    ) {
        ThermalMemoryBudget.Reservation reservation = dimensionBudget.tryReserve(
                projectedMaximumBytes(maximumSections));
        return reservation == null ? null : new BlockRadiationIndex(
                level, pages, states, reservation, maximumSections);
    }

    public static long projectedMaximumBytes(int maximumSections) {
        if (maximumSections <= 0) {
            throw new IllegalArgumentException("maximumSections must be positive");
        }
        return Math.addExact(
                Math.multiplyExact(maximumSections, 640L),
                256L * 1024L);
    }

    public void tick(long gameTick) {
        requireOpen();
        processPending(MAX_BRICK_CAPTURES_PER_TICK);
        if (gameTick % DIRTY_INTERVAL_TICKS == 0L) {
            flushDirty();
        }
    }

    public void markBlock(
            MinecraftPageManager.SectionOwner owner,
            int localX,
            int localY,
            int localZ
    ) {
        if (owner != null) {
            markBlock(owner.sectionKey(), localX, localY, localZ);
        }
    }

    public void markBlock(int blockX, int blockY, int blockZ) {
        markBlock(
                SectionPos.asLong(
                        SectionPos.blockToSectionCoord(blockX),
                        SectionPos.blockToSectionCoord(blockY),
                        SectionPos.blockToSectionCoord(blockZ)),
                SectionPos.sectionRelative(blockX),
                SectionPos.sectionRelative(blockY),
                SectionPos.sectionRelative(blockZ));
    }

    public void onChunkLoad(LevelChunk chunk) {
        requireOpen();
        markHorizontalNeighbors(chunk);
    }

    public void onChunkUnload(LevelChunk chunk) {
        requireOpen();
        for (int index = 0; index < chunk.getSections().length; index++) {
            removeSection(SectionPos.asLong(
                    chunk.getPos().x,
                    chunk.getSectionYFromSectionIndex(index),
                    chunk.getPos().z));
        }
        markHorizontalNeighbors(chunk);
    }

    public void onSectionIdentityReplaced(long sectionKey) {
        requireOpen();
        removeSection(sectionKey);
        int x = SectionPos.x(sectionKey);
        int y = SectionPos.y(sectionKey);
        int z = SectionPos.z(sectionKey);
        markBoundary(SectionPos.asLong(x - 1, y, z), POSITIVE_X);
        markBoundary(SectionPos.asLong(x + 1, y, z), NEGATIVE_X);
        markBoundary(SectionPos.asLong(x, y - 1, z), POSITIVE_Y);
        markBoundary(SectionPos.asLong(x, y + 1, z), NEGATIVE_Y);
        markBoundary(SectionPos.asLong(x, y, z - 1), POSITIVE_Z);
        markBoundary(SectionPos.asLong(x, y, z + 1), NEGATIVE_Z);
    }

    @Override
    public void visitNearby(
            double receiverX,
            double receiverY,
            double receiverZ,
            int maximumVisits,
            RadiationService.SourceVisitor visitor
    ) {
        int minBrickX = floorBrick(receiverX - RANGE_BLOCKS);
        int maxBrickX = floorBrick(receiverX + RANGE_BLOCKS);
        int minBrickY = floorBrick(receiverY - RANGE_BLOCKS);
        int maxBrickY = floorBrick(receiverY + RANGE_BLOCKS);
        int minBrickZ = floorBrick(receiverZ - RANGE_BLOCKS);
        int maxBrickZ = floorBrick(receiverZ + RANGE_BLOCKS);
        int minSectionX = Math.floorDiv(minBrickX, 4);
        int maxSectionX = Math.floorDiv(maxBrickX, 4);
        int minSectionY = Math.floorDiv(minBrickY, 4);
        int maxSectionY = Math.floorDiv(maxBrickY, 4);
        int minSectionZ = Math.floorDiv(minBrickZ, 4);
        int maxSectionZ = Math.floorDiv(maxBrickZ, 4);

        for (int sectionY = minSectionY; sectionY <= maxSectionY; sectionY++) {
            for (int sectionZ = minSectionZ; sectionZ <= maxSectionZ; sectionZ++) {
                for (int sectionX = minSectionX;
                     sectionX <= maxSectionX;
                     sectionX++) {
                    long sectionKey = SectionPos.asLong(
                            sectionX, sectionY, sectionZ);
                    if (!withinRange(
                            receiverX, receiverY, receiverZ,
                            SectionPos.sectionToBlockCoord(sectionX),
                            SectionPos.sectionToBlockCoord(sectionY),
                            SectionPos.sectionToBlockCoord(sectionZ), 16.0D)) {
                        continue;
                    }
                    SectionState state = coveredBySection.get(sectionKey);
                    if (state == null || state.knownBrickMask != -1L) {
                        long requestedMask = requestedMask(
                                sectionX, sectionY, sectionZ,
                                minBrickX, maxBrickX,
                                minBrickY, maxBrickY,
                                minBrickZ, maxBrickZ);
                        state = ensureCoverage(
                                sectionKey, sectionX, sectionY, sectionZ,
                                requestedMask, state);
                    }
                    if (state == null || maximumVisits <= 0) {
                        continue;
                    }
                    maximumVisits = visitSection(
                            sectionX, sectionY, sectionZ, state,
                            receiverX, receiverY, receiverZ,
                            maximumVisits, visitor);
                }
            }
        }
    }

    private SectionState ensureCoverage(
            long sectionKey,
            int sectionX,
            int sectionY,
            int sectionZ,
            long requestedMask,
            SectionState state
    ) {
        if (state == null && coveredBySection.size() >= maximumSections) {
            return null;
        }
        if (state == null) {
            LevelChunk chunk = level.getChunkSource().getChunkNow(
                    sectionX, sectionZ);
            if (chunk == null) {
                return null;
            }
            int sectionIndex = chunk.getSectionIndexFromSectionY(sectionY);
            if (sectionIndex < 0
                    || sectionIndex >= chunk.getSections().length) {
                return null;
            }
            LevelChunkSection section = chunk.getSections()[sectionIndex];
            if (!section.maybeHas(radiationPredicate)
                    || pages.loadedSectionOrAttach(
                            sectionKey, chunk, sectionIndex) == null) {
                return null;
            }
            state = new SectionState();
            synchronized (dirtyLock) {
                coveredBySection.put(sectionKey, state);
            }
        }
        long unknown = requestedMask & ~state.knownBrickMask;
        if (unknown != 0L) {
            pendingBySection.put(
                    sectionKey,
                    pendingBySection.get(sectionKey) | unknown);
        }
        return state;
    }

    private int visitSection(
            int sectionX,
            int sectionY,
            int sectionZ,
            SectionState state,
            double receiverX,
            double receiverY,
            double receiverZ,
            int remainingVisits,
            RadiationService.SourceVisitor visitor
    ) {
        int sectionMinX = SectionPos.sectionToBlockCoord(sectionX);
        int sectionMinY = SectionPos.sectionToBlockCoord(sectionY);
        int sectionMinZ = SectionPos.sectionToBlockCoord(sectionZ);
        long remaining = state.emitterMask;
        int ordinal = 0;
        while (remaining != 0L) {
            int brick = Long.numberOfTrailingZeros(remaining);
            int brickMinX = sectionMinX + ((brick & 3) << 2);
            int brickMinZ = sectionMinZ + ((brick >>> 2 & 3) << 2);
            int brickMinY = sectionMinY + ((brick >>> 4 & 3) << 2);
            long packed = state.emitters[ordinal++];
            if (withinRange(
                    receiverX, receiverY, receiverZ,
                    brickMinX, brickMinY, brickMinZ, 4.0D)) {
                double sourceX = brickMinX + unpackCoordinate(packed, 32);
                double sourceY = brickMinY + unpackCoordinate(packed, 40);
                double sourceZ = brickMinZ + unpackCoordinate(packed, 48);
                double dx = receiverX - sourceX;
                double dy = receiverY - sourceY;
                double dz = receiverZ - sourceZ;
                if (remainingVisits > 0
                        && dx * dx + dy * dy + dz * dz <= RANGE_SQUARED) {
                    remainingVisits--;
                    if (!visitor.visit(
                                BlockPos.asLong(
                                        brickMinX, brickMinY, brickMinZ),
                                RadiationService.STATIC_BLOCK_REVISION,
                                sourceX, sourceY, sourceZ,
                                Float.intBitsToFloat((int) packed),
                                1.0D)) {
                        remainingVisits = 0;
                    }
                }
            }
            remaining &= remaining - 1L;
        }
        return remainingVisits;
    }

    private void processPending(int budget) {
        if (pendingBySection.isEmpty()) {
            return;
        }
        var iterator = pendingBySection.long2LongEntrySet().fastIterator();
        while (budget > 0 && iterator.hasNext()) {
            Long2LongMap.Entry entry = iterator.next();
            long sectionKey = entry.getLongKey();
            SectionState state = coveredBySection.get(sectionKey);
            long unknown = state == null
                    ? 0L : entry.getLongValue() & ~state.knownBrickMask;
            if (unknown == 0L) {
                iterator.remove();
                continue;
            }
            MinecraftPageManager.SectionOwner owner =
                    pages.loadedSectionOrAttach(sectionKey);
            if (owner == null || !owner.section().maybeHas(radiationPredicate)) {
                iterator.remove();
                synchronized (dirtyLock) {
                    coveredBySection.remove(sectionKey);
                    if (writeDirty != null) {
                        writeDirty.remove(sectionKey);
                        drainDirty.remove(sectionKey);
                    }
                }
                continue;
            }
            long batch = takeLowestBits(unknown, budget);
            int captured = Long.bitCount(batch);
            synchronized (dirtyLock) {
                SectionState current = coveredBySection.get(sectionKey);
                if (current != state) {
                    iterator.remove();
                    continue;
                }
                state.knownBrickMask |= batch;
            }
            try {
                rebuild(
                        sectionKey,
                        owner.chunk(),
                        owner.section(),
                        batch);
            } catch (RuntimeException | Error failure) {
                synchronized (dirtyLock) {
                    if (coveredBySection.get(sectionKey) == state) {
                        state.knownBrickMask &= ~batch;
                    }
                }
                throw failure;
            }
            long remaining = entry.getLongValue() & ~batch;
            if (remaining == 0L) {
                iterator.remove();
            } else {
                entry.setValue(remaining);
            }
            budget -= captured;
        }
    }

    private void flushDirty() {
        Long2LongOpenHashMap detached;
        synchronized (dirtyLock) {
            if (writeDirty == null || writeDirty.isEmpty()) {
                return;
            }
            detached = writeDirty;
            writeDirty = drainDirty;
            drainDirty = detached;
        }
        try {
            var iterator = detached.long2LongEntrySet().fastIterator();
            while (iterator.hasNext()) {
                Long2LongMap.Entry entry = iterator.next();
                long sectionKey = entry.getLongKey();
                SectionState state = coveredBySection.get(sectionKey);
                long dirtyMask = state == null
                        ? 0L : entry.getLongValue() & state.knownBrickMask;
                if (dirtyMask == 0L) {
                    continue;
                }
                MinecraftPageManager.SectionOwner owner =
                        pages.loadedSectionOrAttach(sectionKey);
                if (owner == null) {
                    pendingBySection.remove(sectionKey);
                    synchronized (dirtyLock) {
                        coveredBySection.remove(sectionKey);
                        writeDirty.remove(sectionKey);
                    }
                    continue;
                }
                if (!owner.section().maybeHas(radiationPredicate)) {
                    pendingBySection.remove(sectionKey);
                    synchronized (dirtyLock) {
                        coveredBySection.remove(sectionKey);
                        writeDirty.remove(sectionKey);
                    }
                    continue;
                }
                rebuild(
                        sectionKey,
                        owner.chunk(),
                        owner.section(),
                        dirtyMask);
            }
        } finally {
            detached.clear();
        }
    }

    private void markBlock(
            long sectionKey,
            int localX,
            int localY,
            int localZ
    ) {
        if (closed) {
            return;
        }
        int brick = localX >>> 2
                | (localZ >>> 2) << 2
                | (localY >>> 2) << 4;
        markKnownMask(sectionKey, 1L << brick);
    }

    private void markKnownMask(long sectionKey, long mask) {
        synchronized (dirtyLock) {
            markKnownMaskLocked(sectionKey, mask);
        }
    }

    private void markKnownMaskLocked(long sectionKey, long mask) {
        SectionState state = coveredBySection.get(sectionKey);
        long known = state == null ? 0L : mask & state.knownBrickMask;
        if (known == 0L) {
            return;
        }
        ensureDirtyBuffers();
        writeDirty.put(sectionKey, writeDirty.get(sectionKey) | known);
    }

    private void markBoundary(long sectionKey, int face) {
        markKnownMask(sectionKey, boundaryBrickMask(face));
    }

    private void markHorizontalNeighbors(LevelChunk chunk) {
        int x = chunk.getPos().x;
        int z = chunk.getPos().z;
        synchronized (dirtyLock) {
            if (coveredBySection.isEmpty()) {
                return;
            }
            for (int index = 0; index < chunk.getSections().length; index++) {
                int y = chunk.getSectionYFromSectionIndex(index);
                markKnownMaskLocked(
                        SectionPos.asLong(x - 1, y, z),
                        boundaryBrickMask(POSITIVE_X));
                markKnownMaskLocked(
                        SectionPos.asLong(x + 1, y, z),
                        boundaryBrickMask(NEGATIVE_X));
                markKnownMaskLocked(
                        SectionPos.asLong(x, y, z - 1),
                        boundaryBrickMask(POSITIVE_Z));
                markKnownMaskLocked(
                        SectionPos.asLong(x, y, z + 1),
                        boundaryBrickMask(NEGATIVE_Z));
            }
        }
    }

    private void removeSection(long sectionKey) {
        pendingBySection.remove(sectionKey);
        synchronized (dirtyLock) {
            coveredBySection.remove(sectionKey);
            if (writeDirty != null) {
                writeDirty.remove(sectionKey);
                drainDirty.remove(sectionKey);
            }
        }
    }

    private void rebuild(
            long sectionKey,
            LevelChunk chunk,
            LevelChunkSection section,
            long dirtyMask
    ) {
        beginSection(sectionKey, chunk, section);
        replacementMask = 0L;
        try {
            long remaining = dirtyMask;
            while (remaining != 0L) {
                int brick = Long.numberOfTrailingZeros(remaining);
                beginBrick(brick);
                readBrick(section, brick);
                finishBrick();
                remaining &= remaining - 1L;
            }
            commitReplacements(sectionKey, dirtyMask);
        } finally {
            clearSectionScratch();
        }
    }

    private void beginBrick(int brick) {
        currentBrick = brick;
        radiatorMask = 0L;
        occludingMask = 0L;
    }

    private void readBrick(LevelChunkSection section, int brick) {
        int minX = (brick & 3) << 2;
        int minZ = (brick >>> 2 & 3) << 2;
        int minY = (brick >>> 4 & 3) << 2;
        for (int localY = 0; localY < 4; localY++) {
            for (int localZ = 0; localZ < 4; localZ++) {
                for (int localX = 0; localX < 4; localX++) {
                    int block = localX | localZ << 2 | localY << 4;
                    BlockState state = section.getBlockState(
                            minX + localX,
                            minY + localY,
                            minZ + localZ);
                    acceptBlock(block, state, states.code(state));
                }
            }
        }
    }

    private void acceptBlock(
            int blockInBrick,
            BlockState state,
            int thermalStateCode
    ) {
        int profileId = states.radiationProfileIdFromCode(thermalStateCode);
        byte mode = states.radiationMode(profileId);
        profileIds[blockInBrick] = profileId;
        fluidHeights[blockInBrick] =
                mode == MinecraftStateThermalTable.RADIATION_LAVA_SURFACE
                        ? state.getFluidState().getOwnHeight() : 0.0F;
        if (mode != MinecraftStateThermalTable.RADIATION_NONE) {
            radiatorMask |= 1L << blockInBrick;
        }
        if (states.blocksRadiationFromCode(thermalStateCode)) {
            occludingMask |= 1L << blockInBrick;
        }
    }

    private void finishBrick() {
        long packed = compileCurrentBrick();
        replacementValues[currentBrick] = packed;
        if (packed != 0L) {
            replacementMask |= 1L << currentBrick;
        }
    }

    private long compileCurrentBrick() {
        int brickMinX = SectionPos.sectionToBlockCoord(
                SectionPos.x(currentSectionKey)) + ((currentBrick & 3) << 2);
        int brickMinY = SectionPos.sectionToBlockCoord(
                SectionPos.y(currentSectionKey)) + ((currentBrick >>> 4 & 3) << 2);
        int brickMinZ = SectionPos.sectionToBlockCoord(
                SectionPos.z(currentSectionKey)) + ((currentBrick >>> 2 & 3) << 2);
        brickPower = 0.0D;
        brickWeightedX = 0.0D;
        brickWeightedY = 0.0D;
        brickWeightedZ = 0.0D;

        long remaining = radiatorMask;
        while (remaining != 0L) {
            int block = Long.numberOfTrailingZeros(remaining);
            remaining &= remaining - 1L;
            int profileId = profileIds[block];
            byte mode = states.radiationMode(profileId);
            int localX = block & 3;
            int localZ = block >>> 2 & 3;
            int localY = block >>> 4 & 3;
            int sectionX = ((currentBrick & 3) << 2) + localX;
            int sectionZ = ((currentBrick >>> 2 & 3) << 2) + localZ;
            int sectionY = ((currentBrick >>> 4 & 3) << 2) + localY;
            double blockX = brickMinX + localX;
            double blockY = brickMinY + localY;
            double blockZ = brickMinZ + localZ;
            if (mode == MinecraftStateThermalTable.RADIATION_FIXED) {
                addEmitterPoint(
                        states.radiationPower(profileId),
                        blockX + 0.5D,
                        blockY + 0.5D,
                        blockZ + 0.5D);
                continue;
            }

            loadNeighbor(sectionX, sectionY + 1, sectionZ);
            double height = neighborLava ? 1.0D : fluidHeights[block];
            double surfacePower = states.radiationPower(profileId);
            if (!neighborLava && !neighborOccluding) {
                addEmitterPoint(
                        surfacePower,
                        blockX + 0.5D,
                        blockY + height,
                        blockZ + 0.5D);
            }

            loadNeighbor(sectionX, sectionY - 1, sectionZ);
            if (!neighborLava && !neighborOccluding) {
                addEmitterPoint(
                        surfacePower,
                        blockX + 0.5D,
                        blockY,
                        blockZ + 0.5D);
            }

            addSideFace(
                    sectionX - 1, sectionY, sectionZ,
                    height, surfacePower,
                    blockX, blockZ + 0.5D, true);
            addSideFace(
                    sectionX + 1, sectionY, sectionZ,
                    height, surfacePower,
                    blockX + 1.0D, blockZ + 0.5D, true);
            addSideFace(
                    sectionX, sectionY, sectionZ - 1,
                    height, surfacePower,
                    blockZ, blockX + 0.5D, false);
            addSideFace(
                    sectionX, sectionY, sectionZ + 1,
                    height, surfacePower,
                    blockZ + 1.0D, blockX + 0.5D, false);
        }
        if (!(brickPower > 0.0D)) {
            return 0L;
        }
        return packEmitter(
                brickWeightedX / brickPower - brickMinX,
                brickWeightedY / brickPower - brickMinY,
                brickWeightedZ / brickPower - brickMinZ,
                brickPower);
    }

    private void addSideFace(
            int neighborX,
            int neighborY,
            int neighborZ,
            double height,
            double surfacePower,
            double fixedCoordinate,
            double centeredCoordinate,
            boolean xFace
    ) {
        loadNeighbor(neighborX, neighborY, neighborZ);
        if (neighborOccluding) {
            return;
        }
        double neighborSurface = 0.0D;
        if (neighborLava) {
            double ownHeight = neighborHeight;
            loadNeighbor(neighborX, neighborY + 1, neighborZ);
            neighborSurface = neighborLava ? 1.0D : ownHeight;
        }
        double area = Math.max(0.0D, height - neighborSurface);
        if (area == 0.0D) {
            return;
        }
        double power = surfacePower * area;
        double y = SectionPos.sectionToBlockCoord(
                SectionPos.y(currentSectionKey))
                + neighborY + (neighborSurface + height) * 0.5D;
        addEmitterPoint(
                power,
                xFace ? fixedCoordinate : centeredCoordinate,
                y,
                xFace ? centeredCoordinate : fixedCoordinate);
    }

    private void addEmitterPoint(
            double power,
            double x,
            double y,
            double z
    ) {
        brickPower += power;
        brickWeightedX += power * x;
        brickWeightedY += power * y;
        brickWeightedZ += power * z;
    }

    private void loadNeighbor(int localX, int localY, int localZ) {
        if (localX >= 0 && localX < 16
                && localY >= 0 && localY < 16
                && localZ >= 0 && localZ < 16) {
            int brick = localX >>> 2
                    | (localZ >>> 2) << 2
                    | (localY >>> 2) << 4;
            if (brick == currentBrick) {
                int block = localX & 3
                        | (localZ & 3) << 2
                        | (localY & 3) << 4;
                int profileId = profileIds[block];
                neighborHeight = fluidHeights[block];
                neighborLava = states.radiationMode(profileId)
                        == MinecraftStateThermalTable.RADIATION_LAVA_SURFACE;
                neighborOccluding = (occludingMask & 1L << block) != 0L;
                return;
            }
            setNeighbor(currentSection.getBlockState(localX, localY, localZ));
            return;
        }

        int face;
        int x = localX;
        int y = localY;
        int z = localZ;
        if (localX < 0) {
            face = NEGATIVE_X;
            x = 15;
        } else if (localX >= 16) {
            face = POSITIVE_X;
            x = 0;
        } else if (localY < 0) {
            face = NEGATIVE_Y;
            y = 15;
        } else if (localY >= 16) {
            face = POSITIVE_Y;
            y = 0;
        } else if (localZ < 0) {
            face = NEGATIVE_Z;
            z = 15;
        } else {
            face = POSITIVE_Z;
            z = 0;
        }
        LevelChunkSection section = neighborSection(face);
        if (section == null) {
            neighborHeight = 0.0F;
            neighborLava = false;
            neighborOccluding = true;
            return;
        }
        setNeighbor(section.getBlockState(x, y, z));
    }

    private void setNeighbor(BlockState state) {
        int stateCode = states.code(state);
        int profileId = states.radiationProfileIdFromCode(stateCode);
        neighborLava = states.radiationMode(profileId)
                == MinecraftStateThermalTable.RADIATION_LAVA_SURFACE;
        neighborHeight = neighborLava
                ? state.getFluidState().getOwnHeight() : 0.0F;
        neighborOccluding = states.blocksRadiationFromCode(stateCode);
    }

    private LevelChunkSection neighborSection(int face) {
        int bit = 1 << face;
        if ((neighborResolvedMask & bit) != 0) {
            return (neighborPresentMask & bit) != 0
                    ? neighborSections[face] : null;
        }
        neighborResolvedMask |= (byte) bit;
        int x = SectionPos.x(currentSectionKey);
        int y = SectionPos.y(currentSectionKey);
        int z = SectionPos.z(currentSectionKey);
        LevelChunkSection result = null;
        if (face == NEGATIVE_Y || face == POSITIVE_Y) {
            int targetY = y + (face == NEGATIVE_Y ? -1 : 1);
            int sectionIndex = currentChunk.getSectionIndexFromSectionY(targetY);
            if (sectionIndex >= 0
                    && sectionIndex < currentChunk.getSections().length) {
                result = currentChunk.getSections()[sectionIndex];
            }
        } else {
            int targetX = x + (face == NEGATIVE_X ? -1
                    : face == POSITIVE_X ? 1 : 0);
            int targetZ = z + (face == NEGATIVE_Z ? -1
                    : face == POSITIVE_Z ? 1 : 0);
            LevelChunk chunk = level.getChunkSource().getChunkNow(
                    targetX, targetZ);
            if (chunk != null) {
                int sectionIndex = chunk.getSectionIndexFromSectionY(y);
                if (sectionIndex >= 0
                        && sectionIndex < chunk.getSections().length) {
                    result = chunk.getSections()[sectionIndex];
                }
            }
        }
        neighborSections[face] = result;
        if (result != null) {
            neighborPresentMask |= (byte) bit;
        }
        return result;
    }

    private void beginSection(
            long sectionKey,
            LevelChunk chunk,
            LevelChunkSection section
    ) {
        currentSectionKey = sectionKey;
        currentChunk = chunk;
        currentSection = section;
        neighborResolvedMask = 0;
        neighborPresentMask = 0;
    }

    private void clearSectionScratch() {
        Arrays.fill(neighborSections, null);
        currentChunk = null;
        currentSection = null;
        neighborResolvedMask = 0;
        neighborPresentMask = 0;
    }

    private void commitReplacements(long sectionKey, long dirtyMask) {
        SectionState state = coveredBySection.get(sectionKey);
        if (state == null) {
            return;
        }
        long oldMask = state.emitterMask;
        long nextMask = oldMask & ~dirtyMask | replacementMask & dirtyMask;
        int nextCount = Long.bitCount(nextMask);
        int write = 0;
        long remaining = nextMask;
        while (remaining != 0L) {
            int brick = Long.numberOfTrailingZeros(remaining);
            long bit = 1L << brick;
            nextValues[write++] = (dirtyMask & bit) != 0L
                    ? replacementValues[brick]
                    : state.emitters[Long.bitCount(oldMask & (bit - 1L))];
            remaining &= remaining - 1L;
        }
        boolean identical = oldMask == nextMask;
        if (identical && state.emitters != null) {
            for (int index = 0; index < nextCount; index++) {
                if (state.emitters[index] != nextValues[index]) {
                    identical = false;
                    break;
                }
            }
        }
        if (identical) {
            return;
        }
        if (nextCount > 0) {
            if (state.emitters == null) {
                state.emitters = new long[Math.max(4, nextCount)];
            } else if (state.emitters.length < nextCount) {
                state.emitters = Arrays.copyOf(
                        state.emitters,
                        Math.max(nextCount, state.emitters.length * 2));
            }
            System.arraycopy(nextValues, 0, state.emitters, 0, nextCount);
        }
        state.emitterMask = nextMask;
    }

    private static long requestedMask(
            int sectionX,
            int sectionY,
            int sectionZ,
            int minBrickX,
            int maxBrickX,
            int minBrickY,
            int maxBrickY,
            int minBrickZ,
            int maxBrickZ
    ) {
        int baseX = sectionX << 2;
        int baseY = sectionY << 2;
        int baseZ = sectionZ << 2;
        int xBits = rangeBits(
                Math.max(0, minBrickX - baseX),
                Math.min(3, maxBrickX - baseX));
        int yBits = rangeBits(
                Math.max(0, minBrickY - baseY),
                Math.min(3, maxBrickY - baseY));
        int zBits = rangeBits(
                Math.max(0, minBrickZ - baseZ),
                Math.min(3, maxBrickZ - baseZ));
        return X_MASKS[xBits] & Y_MASKS[yBits] & Z_MASKS[zBits];
    }

    private static int rangeBits(int min, int max) {
        return min > max ? 0 : (1 << (max + 1)) - (1 << min);
    }

    private static int floorBrick(double blockCoordinate) {
        return (int) Math.floor(blockCoordinate / 4.0D);
    }

    private static long takeLowestBits(long bits, int maximum) {
        long selected = 0L;
        while (bits != 0L && maximum-- > 0) {
            long bit = Long.lowestOneBit(bits);
            selected |= bit;
            bits ^= bit;
        }
        return selected;
    }

    private static long packEmitter(
            double localX,
            double localY,
            double localZ,
            double power
    ) {
        float finitePower = (float) power;
        if (!Float.isFinite(finitePower) || finitePower <= 0.0F) {
            throw new ArithmeticException(
                    "static Block radiation power exceeded float range");
        }
        return Integer.toUnsignedLong(Float.floatToRawIntBits(finitePower))
                | (long) quantizeCoordinate(localX) << 32
                | (long) quantizeCoordinate(localY) << 40
                | (long) quantizeCoordinate(localZ) << 48;
    }

    private static int quantizeCoordinate(double value) {
        return Math.max(0, Math.min(255, (int) Math.round(value * 16.0D)));
    }

    private static double unpackCoordinate(long packed, int shift) {
        return (packed >>> shift & 0xffL) / 16.0D;
    }

    private static boolean withinRange(
            double x,
            double y,
            double z,
            double minX,
            double minY,
            double minZ,
            double size
    ) {
        double dx = axisDistance(x, minX, minX + size);
        double dy = axisDistance(y, minY, minY + size);
        double dz = axisDistance(z, minZ, minZ + size);
        return dx * dx + dy * dy + dz * dz <= RANGE_SQUARED;
    }

    private static double axisDistance(double value, double min, double max) {
        return value < min ? min - value : value > max ? value - max : 0.0D;
    }

    private static long boundaryBrickMask(int face) {
        return switch (face) {
            case NEGATIVE_X -> X_MASKS[1];
            case POSITIVE_X -> X_MASKS[8];
            case NEGATIVE_Y -> Y_MASKS[1];
            case POSITIVE_Y -> Y_MASKS[8];
            case NEGATIVE_Z -> Z_MASKS[1];
            case POSITIVE_Z -> Z_MASKS[8];
            default -> 0L;
        };
    }

    private void ensureDirtyBuffers() {
        if (writeDirty == null) {
            writeDirty = new Long2LongOpenHashMap();
            drainDirty = new Long2LongOpenHashMap();
        }
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Block radiation index is closed");
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        pendingBySection.clear();
        synchronized (dirtyLock) {
            coveredBySection.clear();
            if (writeDirty != null) {
                writeDirty.clear();
                drainDirty.clear();
            }
        }
        clearSectionScratch();
        reservation.close();
    }

    private static final class SectionState {
        private long knownBrickMask;
        private long emitterMask;
        private long[] emitters;
    }
}
