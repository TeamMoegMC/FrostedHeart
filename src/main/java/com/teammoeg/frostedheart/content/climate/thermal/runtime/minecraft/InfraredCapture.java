/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import static com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput.MAX_PUBLICATION_AGE_TICKS;
import static com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput.dormantHalfLifeSeconds;

import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.network.InfraredBrickCodec;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalAnalyticField;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalAnalyticFieldIndex;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.BlockBrickLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PagePublication;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.DormantThermalCooling;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.DormantChunkThermalState;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.MaterialSectionState;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.MinecraftThermalChunkAttachment;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftThermalProfiles;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput.InfraredSnapshot;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftPageManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Arrays;

/** Shared main-thread scratch; display reads never start a runtime or load chunks. */
final class InfraredCapture implements AutoCloseable {
    private static final int INFRARED_ACTIVE_TICKS = 80;
    private static final int INFRARED_PAGE_CAPACITY = 9 * 9 * 9;
    static final int INFRARED_PRESENCE_WORDS = 12;
    private static final long[] NO_INFRARED_PRESENCE = new long[0];
    private final QueryPublication.ReadCursor cursor = new QueryPublication.ReadCursor();
    private final ThermalPageHandle[] handles = new ThermalPageHandle[INFRARED_PAGE_CAPACITY];
    private final ThermalPageHandle[] localHandles = new ThermalPageHandle[INFRARED_PAGE_CAPACITY];
    private final short[] localIndexes = new short[INFRARED_PAGE_CAPACITY];
    private final long[] presence = new long[INFRARED_PRESENCE_WORDS];
    private final long[] fieldPages = new long[INFRARED_PRESENCE_WORDS];
    private final long[] storedPresence = new long[INFRARED_PRESENCE_WORDS];
    private final LevelChunk[] loadedChunks = new LevelChunk[81];
    private final double[] storedTemperatures = new double[4096];
    private final long[] changedMaterialBlocks = new long[64];
    private long changedMaterialBricks;
    private final MaterialSample storedSample = new MaterialSample();
    private int profileEpoch = -1;
    private long profileRevision;
    private final short[] nodes = new short[64], blocks = new short[64];
    private final double[] rawNodes = new double[64], rawBlocks = new double[64];
    private final java.util.ArrayList<ThermalAnalyticField> fields = new java.util.ArrayList<>();
    private final java.util.ArrayList<ThermalAnalyticField> brickFields =
            new java.util.ArrayList<>();
    private final ThermalAnalyticFieldIndex.Sample fieldSample =
            new ThermalAnalyticFieldIndex.Sample();
    private final QueryPublication.MutableSample sample = new QueryPublication.MutableSample();
    private final BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
    private final InfraredBrickCodec.Builder payload = new InfraredBrickCodec.Builder();
    private final boolean[] loadedNeighbors = new boolean[9];
    private boolean neighborsReady;
    private ServerLevel level;
    private int originX, originY, originZ;

    InfraredSnapshot capture(
            MinecraftPageManager pages,
            QueryPublication publication,
            long generation,
            ServerPlayer player,
            boolean forceFull,
            long knownGeneration,
            long knownCenter,
            int lastEpoch,
            long[] knownPresence,
            boolean knownReadable,
            long[] knownFieldPages,
            long knownStoredEpoch,
            long knownStoredSampleTick) {
        level = player.serverLevel();
        int cx = Mth.floor(player.getX()) >> 4, cy = Mth.floor(player.getEyeY()) >> 4;
        int cz = Mth.floor(player.getZ()) >> 4;
        originX = (cx - 4) * 16;
        originY = (cy - 4) * 16;
        originZ = (cz - 4) * 16;
        long center = SectionPos.asLong(cx, cy, cz), tick = level.getGameTime();
        boolean reactivated =
                publication != null && publication.noteInfraredRequest(tick, INFRARED_ACTIVE_TICKS);
        try {
            if (profileEpoch != MinecraftThermalProfiles.profileEpoch()) {
                profileEpoch = MinecraftThermalProfiles.profileEpoch();
                profileRevision = DormantChunkThermalState.nextMaterialRevision();
            }
            collectFields();
            collectStoredMaterials(cx, cy, cz);
            long storedEpoch = DormantChunkThermalState.currentMaterialRevision();
            // A concurrent publication exchange is not a material deletion. Retry, then retain the
            // client baseline.
            captureAttempt:
            for (int attempt = 0; attempt < 2; attempt++) {
                payload.reset();
                cursor.clear();
                Arrays.fill(presence, 0L);
                Arrays.fill(localHandles, null);
                if (publication != null && !publication.beginRead(cursor)) continue;
                boolean readable =
                        publication != null
                                && cursor.valid()
                                && tick - cursor.sampleTick() <= MAX_PUBLICATION_AGE_TICKS;
                int epoch = readable ? cursor.infraredEpoch() : 0;
                boolean full =
                        forceFull
                                || knownGeneration != generation
                                || knownCenter != center
                                || knownStoredEpoch > storedEpoch
                                || knownStoredSampleTick > tick
                                || knownReadable != readable
                                || readable && (reactivated || lastEpoch == 0 || lastEpoch > epoch);
                if (publication != null) collectMaterials(pages, cx, cy, cz, readable);
                for (int word = 0; word < presence.length; word++) {
                    long previousFields =
                            full || knownFieldPages.length == 0 ? 0 : knownFieldPages[word];
                    long work =
                            presence[word]
                                    | storedPresence[word]
                                    | fieldPages[word]
                                    | (full ? 0 : knownPresence[word] | previousFields);
                    while (work != 0) {
                        int local = word * 64 + Long.numberOfTrailingZeros(work);
                        work &= work - 1;
                        if (local >= INFRARED_PAGE_CAPACITY) continue;
                        LevelChunk chunk = loadedChunks[local % 81];
                        int sectionY = cy - 4 + local / 81;
                        var attachment = (MinecraftThermalChunkAttachment) (Object) chunk;
                        var state =
                                attachment == null
                                        ? null
                                        : attachment.frostedheart$getDormantThermalState();
                        MaterialSectionState stored =
                                state == null ? null : state.materials(sectionY);
                        ThermalPageHandle handle = localHandles[local];
                        if (handle == null && publication != null && stored != null) {
                            handle =
                                    pages.handle(
                                            SectionPos.asLong(
                                                    cx - 4 + local % 9,
                                                    cy - 4 + local / 81,
                                                    cz - 4 + local / 9 % 9));
                        }
                        PagePublication page =
                                !readable || handle == null ? null : handle.lastPublication();
                        if (page != null && page.topologyGeneration() > cursor.topologyGeneration())
                            continue captureAttempt;
                        if (page == null) {
                            presence[word] &= ~(1L << (local & 63));
                        }
                        if (handle != null && page == null) stored = null;
                        changedMaterialBricks =
                                page != null
                                                && page.geometryRevision()
                                                        != handle.liveGeometryRevision()
                                        ? pages.collectMaterialChangesSince(
                                                handle.sectionKey(),
                                                page.geometryRevision(),
                                                changedMaterialBlocks)
                                        : 0L;
                        if (changedMaterialBricks != 0L) {
                            presence[word] &= ~(1L << (local & 63));
                            for (int b = 0; b < 64; b++)
                                if (readableSurfaceMask(page.brick(b), b) != 0) {
                                    presence[word] |= 1L << (local & 63);
                                    break;
                                }
                        }
                        long storedMask = storedBrickMask(page, stored);
                        double storedNatural =
                                storedMask == 0
                                        ? 0
                                        : state.naturalTemperature(
                                                level,
                                                cx - 4 + local % 9,
                                                sectionY,
                                                cz - 4 + local / 9 % 9,
                                                tick,
                                                position);
                        long storedRevision =
                                attachment == null
                                        ? 0
                                        : Math.max(
                                                attachment.frostedheart$getMaterialRevision(),
                                                state == null
                                                        ? 0
                                                        : state.materialRevision(sectionY));
                        if (storedMask != 0) presence[word] |= 1L << (local & 63);
                        boolean refresh =
                                presenceBit(fieldPages, local)
                                        || (previousFields & 1L << (local & 63)) != 0;
                        boolean replace =
                                full
                                        || refresh
                                        || presenceBit(presence, local)
                                                != presenceBit(knownPresence, local);
                        long changed =
                                replace
                                        ? -1L
                                        : page == null
                                                ? 0
                                                : changedBricks(page.workerPageSlot(), lastEpoch);
                        changed |= changedMaterialBricks;
                        if (storedRevision > knownStoredEpoch
                                || storedMask != 0 && profileRevision > knownStoredEpoch)
                            changed = -1L;
                        if (storedMask != 0)
                            changed |=
                                    readStoredPage(
                                            stored,
                                            storedMask,
                                            local,
                                            storedNatural,
                                            tick,
                                            knownStoredSampleTick,
                                            changed);
                        if (changed == 0) continue;
                        payload.beginPage();
                        if (!writePage(page, stored, storedMask, local, changed, full)
                                || page != null && handle.lastPublication() != page)
                            continue captureAttempt;
                    }
                }
                if (publication != null && !cursor.isCurrent()) continue;
                boolean changedPresence = !Arrays.equals(presence, knownPresence);
                if (!full
                        && !changedPresence
                        && payload.size() == 0
                        && sameFieldPages(knownFieldPages)) return null;
                return new InfraredSnapshot(
                        cx,
                        cz,
                        cy,
                        generation,
                        epoch,
                        readable,
                        full,
                        full || changedPresence ? presence.clone() : NO_INFRARED_PRESENCE,
                        hasFieldPages() ? fieldPages.clone() : NO_INFRARED_PRESENCE,
                        payload.finishParts(),
                        DormantChunkThermalState.currentMaterialRevision(),
                        tick);
            }
            return null;
        } finally {
            cursor.clear();
            Arrays.fill(handles, null);
            Arrays.fill(localHandles, null);
            Arrays.fill(loadedChunks, null);
            fields.clear();
            brickFields.clear();
            level = null;
            payload.reset();
        }
    }

    private void collectMaterials(
            MinecraftPageManager pages, int cx, int cy, int cz, boolean readable) {
        int count = pages.collectInfraredPages(cx, cy, cz, handles, localIndexes, presence);
        Arrays.fill(presence, 0L);
        for (int i = 0; i < count; i++) {
            ThermalPageHandle handle = handles[i];
            int local = Short.toUnsignedInt(localIndexes[i]);
            localHandles[local] = handle;
            PagePublication page = handle.lastPublication();
            if (!readable
                    || page == null
                    || page.topologyGeneration() > cursor.topologyGeneration()) continue;
            for (int b = 0; b < 64; b++) {
                if (surfaceMask(page.brick(b)) == 0) continue;
                presence[local >>> 6] |= 1L << (local & 63);
                break;
            }
        }
    }

    private void collectStoredMaterials(int cx, int cy, int cz) {
        Arrays.fill(storedPresence, 0);
        for (int z = 0; z < 9; z++)
            for (int x = 0; x < 9; x++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx - 4 + x, cz - 4 + z);
                loadedChunks[x + 9 * z] = chunk;
                if (chunk == null) continue;
                var attachment = (MinecraftThermalChunkAttachment) (Object) chunk;
                var state = attachment.frostedheart$getDormantThermalState();
                if (state == null) continue;
                for (int y = 0; y < 9; y++) {
                    int local = x + 9 * (z + 9 * y);
                    if (state.hasMaterials(cy - 4 + y))
                        storedPresence[local >>> 6] |= 1L << (local & 63);
                }
            }
    }

    private static long storedBrickMask(PagePublication page, MaterialSectionState stored) {
        if (stored == null) return 0;
        long mask = stored.brickMask(), remaining = mask;
        if (page != null)
            while (remaining != 0) {
                int index = Long.numberOfTrailingZeros(remaining);
                remaining &= remaining - 1;
                var brick = page.brick(index);
                if (brick.resolved() && brick.firstSlot() >= 0) mask &= ~(1L << index);
            }
        return mask;
    }

    private void collectFields() {
        Arrays.fill(fieldPages, 0L);
        fields.clear();
        var index = MinecraftGameplayFields.existing(level);
        if (index == null) return;
        index.collectIntersecting(
                originX + .5,
                originY + .5,
                originZ + .5,
                originX + 143.5,
                originY + 143.5,
                originZ + 143.5,
                fields);
        for (var field : fields) {
            int minX = clippedPage(field.min(0), originX),
                    maxX = clippedPage(field.max(0), originX);
            int minY = clippedPage(field.min(1), originY),
                    maxY = clippedPage(field.max(1), originY);
            int minZ = clippedPage(field.min(2), originZ),
                    maxZ = clippedPage(field.max(2), originZ);
            for (int y = minY; y <= maxY; y++)
                for (int z = minZ; z <= maxZ; z++)
                    for (int x = minX; x <= maxX; x++) {
                        int bx = originX + x * 16, by = originY + y * 16, bz = originZ + z * 16;
                        if (!field.intersects(
                                bx + .5, by + .5, bz + .5, bx + 15.5, by + 15.5, bz + 15.5))
                            continue;
                        int local = x + 9 * (z + 9 * y);
                        fieldPages[local >>> 6] |= 1L << (local & 63);
                    }
        }
    }

    private static int clippedPage(double coordinate, int origin) {
        return (int) Math.max(0, Math.min(8, Math.floor((coordinate - origin) / 16)));
    }

    private boolean hasFieldPages() {
        for (long word : fieldPages) if (word != 0) return true;
        return false;
    }

    private boolean sameFieldPages(long[] known) {
        return known.length == 0 ? !hasFieldPages() : Arrays.equals(fieldPages, known);
    }

    private boolean writePage(
            PagePublication page,
            MaterialSectionState stored,
            long storedMask,
            int local,
            long changed,
            boolean full) {
        int x = originX + local % 9 * 16,
                z = originZ + local / 9 % 9 * 16,
                y = originY + local / 81 * 16;
        boolean withFields =
                presenceBit(fieldPages, local)
                        && !level.isOutsideBuildHeight(y)
                        && loadedChunks[local % 81] != null;
        neighborsReady = false;
        while (changed != 0) {
            int brick = Long.numberOfTrailingZeros(changed);
            changed &= changed - 1;
            int bx = x + (brick & 3) * 4,
                    by = y + (brick >>> 4) * 4,
                    bz = z + (brick >>> 2 & 3) * 4;
            brickFields.clear();
            if (withFields)
                for (int i = 0; i < fields.size(); i++) {
                    var field = fields.get(i);
                    if (field.intersects(bx + .5, by + .5, bz + .5, bx + 3.5, by + 3.5, bz + 3.5))
                        brickFields.add(field);
                }
            if (!writeBrick(page, (storedMask & 1L << brick) != 0, local, brick, full, bx, by, bz))
                return false;
        }
        return true;
    }

    private long readStoredPage(
            MaterialSectionState stored,
            long mask,
            int local,
            double natural,
            long tick,
            long previousTick,
            long changed) {
        Arrays.fill(storedTemperatures, Double.NaN);
        LevelChunk chunk = loadedChunks[local % 81];
        int x = originX + local % 9 * 16,
                z = originZ + local / 9 % 9 * 16,
                y = originY + local / 81 * 16;
        double coolingRate = DormantThermalCooling.rate(dormantHalfLifeSeconds());
        long cachedTick = Long.MIN_VALUE;
        double currentFraction = 0, previousFraction = 0;
        for (int index = 0; index < stored.size(); index++) {
            int block = stored.position(index);
            int brick =
                    (block & 15) >>> 2
                            | ((block >>> 4 & 15) >>> 2) << 2
                            | ((block >>> 8) >>> 2) << 4;
            if ((mask & 1L << brick) == 0) continue;
            BlockState state =
                    chunk.getBlockState(
                            position.set(
                                    x + (block & 15), y + (block >>> 8), z + (block >>> 4 & 15)));
            if (net.minecraft.world.level.block.Block.BLOCK_STATE_REGISTRY.getId(state)
                    != stored.stateId(index)) continue;
            var law = MinecraftThermalProfiles.materialLaw(state);
            if (law == null) continue;
            if (stored.savedTick(index) != cachedTick) {
                cachedTick = stored.savedTick(index);
                currentFraction = DormantThermalCooling.fraction(cachedTick, tick, coolingRate);
                previousFraction =
                        DormantThermalCooling.fraction(cachedTick, previousTick, coolingRate);
            }
            stored.read(index, law, tick, natural, coolingRate, currentFraction, storedSample);
            storedTemperatures[block] = storedSample.temperatureC();
            if (tick != previousTick && (changed & 1L << brick) == 0) {
                short current = InfraredBrickCodec.quantize(storedSample.temperatureC());
                stored.read(
                        index,
                        law,
                        previousTick,
                        natural,
                        coolingRate,
                        previousFraction,
                        storedSample);
                if (current != InfraredBrickCodec.quantize(storedSample.temperatureC()))
                    changed |= 1L << brick;
            }
        }
        return changed;
    }

    private static long surfaceMask(PagePublication.Brick brick) {
        return brick.resolved() && brick.firstSlot() >= 0 && brick.blockLayout() != null
                ? brick.blockLayout().materialNodeMask()
                : 0L;
    }

    private long readableSurfaceMask(PagePublication.Brick brick, int index) {
        long mask = surfaceMask(brick);
        if ((changedMaterialBricks & 1L << index) == 0L) return mask;
        long nodes = mask;
        while (nodes != 0L) {
            int node = Long.numberOfTrailingZeros(nodes);
            nodes &= nodes - 1;
            if ((brick.blockLayout().nodeBlockMask(node) & changedMaterialBlocks[index]) != 0L)
                mask &= ~(1L << node);
        }
        return mask;
    }

    private long changedBricks(int page, int epoch) {
        if (cursor.pageChangeEpoch(page) <= epoch) return 0;
        long result = 0;
        for (int brick = 0; brick < 64; brick++)
            if (cursor.brickChangeEpoch(page, brick) > epoch) result |= 1L << brick;
        return result;
    }

    private boolean writeBrick(
            PagePublication page,
            boolean stored,
            int localPage,
            int index,
            boolean full,
            int x,
            int y,
            int z) {
        var brick = page == null ? null : page.brick(index);
        long mask = brick == null ? 0 : readableSurfaceMask(brick, index);
        int address = localPage * 64 + index;
        boolean compose = !brickFields.isEmpty();
        if (mask == 0 && !stored && !compose) {
            payload.writeInvalid(address, full);
            return true;
        }
        long remaining = mask;
        while (remaining != 0) {
            int node = Long.numberOfTrailingZeros(remaining);
            remaining &= remaining - 1;
            if (!cursor.tryRead(
                    brick.firstSlot() + node,
                    brick.arenaGeneration(),
                    page.topologyGeneration(),
                    sample)) return false;
            if (compose) rawNodes[node] = sample.temperatureC();
            else nodes[node] = InfraredBrickCodec.quantize(sample.temperatureC());
        }
        for (int block = 0; block < 64; block++) {
            int node =
                    brick == null || brick.blockLayout() == null
                            ? -1
                            : brick.blockLayout().nodeAt(block);
            boolean material = node >= 0 && (mask & 1L << node) != 0;
            double saved =
                    stored
                            ? storedTemperatures[BlockBrickLayout.pageBlock(index, block)]
                            : Double.NaN;
            if (compose) rawBlocks[block] = material ? rawNodes[node] : saved;
            else blocks[block] = material ? nodes[node] : InfraredBrickCodec.quantize(saved);
        }
        if (compose) {
            int naturalMode = 0, naturalLayers = 0;
            // Material values have been expanded; rawNodes can now hold four exact natural Y
            // layers.
            for (int block = 0; block < 64; block++) {
                int px = x + (block & 3), py = y + (block >>> 4), pz = z + (block >>> 2 & 3);
                fieldSample.clear();
                for (int i = 0; i < brickFields.size(); i++) {
                    var field = brickFields.get(i);
                    if (field.contains(px + .5, py + .5, pz + .5)) fieldSample.include(field);
                }
                double temperature = rawBlocks[block];
                if (fieldSample.present()) {
                    boolean needsNatural =
                            fieldSample.requiresNatural()
                                    || !Double.isFinite(temperature) && fieldSample.requiresBase();
                    if (needsNatural && !neighborsReady) prepareNeighbors(x >> 4, z >> 4);
                    if (!needsNatural || hasBiomeNeighbors(px, pz)) {
                        double natural = 0;
                        if (needsNatural) {
                            if (naturalMode == 0) naturalMode = uniformBiomeBrick(x, y, z) ? 1 : -1;
                            int layer = block >>> 4;
                            if (naturalMode > 0) {
                                if ((naturalLayers & 1 << layer) == 0) {
                                    rawNodes[layer] =
                                            WorldTemperature.naturalAir(
                                                    level, position.set(px, py, pz));
                                    naturalLayers |= 1 << layer;
                                }
                                natural = rawNodes[layer];
                            } else
                                natural =
                                        WorldTemperature.naturalAir(
                                                level, position.set(px, py, pz));
                        }
                        temperature =
                                fieldSample.compose(
                                        natural,
                                        Double.isFinite(temperature) ? temperature : natural);
                    }
                }
                blocks[block] = InfraredBrickCodec.quantize(temperature);
            }
        }
        payload.writeBrick(address, blocks, full);
        return true;
    }

    private void prepareNeighbors(int cx, int cz) {
        for (int dz = -1; dz <= 1; dz++)
            for (int dx = -1; dx <= 1; dx++)
                loadedNeighbors[(dz + 1) * 3 + dx + 1] =
                        level.getChunkSource().getChunkNow(cx + dx, cz + dz) != null;
        neighborsReady = true;
    }

    private boolean hasBiomeNeighbors(int x, int z) {
        int dx = (x & 15) < 2 ? -1 : (x & 15) >= 14 ? 1 : 0;
        int dz = (z & 15) < 2 ? -1 : (z & 15) >= 14 ? 1 : 0;
        return loadedNeighbors[4 + dx]
                && loadedNeighbors[4 + dz * 3]
                && loadedNeighbors[4 + dz * 3 + dx];
    }

    private boolean uniformBiomeBrick(int x, int y, int z) {
        int minX = (x & 15) == 0 ? -1 : 0, maxX = (x & 15) == 12 ? 1 : 0;
        int minZ = (z & 15) == 0 ? -1 : 0, maxZ = (z & 15) == 12 ? 1 : 0;
        for (int dz = minZ; dz <= maxZ; dz++)
            for (int dx = minX; dx <= maxX; dx++)
                if (!loadedNeighbors[(dz + 1) * 3 + dx + 1]) return false;
        int qx = x >> 2, qy = y >> 2, qz = z >> 2;
        var biome = level.getNoiseBiome(qx, qy, qz);
        for (int dy = -1; dy <= 1; dy++)
            for (int dz = -1; dz <= 1; dz++)
                for (int dx = -1; dx <= 1; dx++)
                    if ((dx != 0 || dy != 0 || dz != 0)
                            && level.getNoiseBiome(qx + dx, qy + dy, qz + dz) != biome)
                        return false;
        return true;
    }

    @Override
    public void close() {
        payload.close();
    }

    private static boolean presenceBit(long[] presence, int page) {
        return presence.length != 0 && (presence[page >>> 6] & 1L << (page & 63)) != 0;
    }
}
