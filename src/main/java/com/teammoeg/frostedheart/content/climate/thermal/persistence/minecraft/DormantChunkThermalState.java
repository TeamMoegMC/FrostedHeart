/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft;

import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialThermalLaw;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PagePublication;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.DormantThermalCooling;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftThermalProfiles;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;

import java.util.Arrays;

/**
 * LevelChunk 自有的、有界休眠温度 checkpoint。
 *
 * <p>保存 Air 温度残差及自然基准，以及材料本体 H/分支/状态身份和时间；
 * 不保存 topology、arena slot、source 历史或离线 solver 状态。</p>
 */
public final class DormantChunkThermalState {
    public enum MaterialRead {
        MISSING,
        MISMATCH,
        MATCH
    }

    private static final java.util.concurrent.atomic.AtomicLong MATERIAL_REVISION =
            new java.util.concurrent.atomic.AtomicLong();

    public static long nextMaterialRevision() {
        return MATERIAL_REVISION.incrementAndGet();
    }

    public static long currentMaterialRevision() {
        return MATERIAL_REVISION.get();
    }

    private static final String ROOT_TAG = "FrostedHeartThermal";
    private static final int FORMAT_VERSION = 4;
    private static final int BRICKS = ThermalPageHandle.BASE_BRICK_COUNT;
    // Air stores signed temperature excess in sixteenths of a degree, relative to savedNaturalC.
    // All short values are valid here, including MIN_VALUE (unlike infrared's missing marker).
    private static final int RESIDUAL_SCALE = 16;
    private static final int PRUNE_RESIDUAL = 4; // Retain only residuals strictly above 0.25 C.
    private static final int MAX_AIR_PAYLOAD_BYTES = 640;
    private static final long CACHE_INTERVAL_TICKS = 20L;

    private final int minimumSectionY;
    private final SectionEntry[] entries;
    private final MaterialSectionState.Editor[] materialEntries;
    private long[] materialRevisions;
    private long[] cachedNaturalTicks;
    private double[] cachedNaturalTemperatures;

    public DormantChunkThermalState(int minimumSectionY, int sectionCount) {
        if (sectionCount <= 0) {
            throw new IllegalArgumentException("sectionCount must be positive");
        }
        this.minimumSectionY = minimumSectionY;
        entries = new SectionEntry[sectionCount];
        materialEntries = new MaterialSectionState.Editor[sectionCount];
    }

    public static DormantChunkThermalState decode(
            CompoundTag chunkTag, int minimumSectionY, int sectionCount) {
        if (!chunkTag.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            return null;
        }
        CompoundTag root = chunkTag.getCompound(ROOT_TAG);
        if (root.getInt("version") != FORMAT_VERSION) {
            return null;
        }
        DormantChunkThermalState result =
                new DormantChunkThermalState(minimumSectionY, sectionCount);
        ListTag sections = root.getList("sections", Tag.TAG_COMPOUND);
        for (Tag tag : sections) {
            CompoundTag section = (CompoundTag) tag;
            int sectionY = section.getInt("y");
            int index = sectionY - minimumSectionY;
            if (index < 0 || index >= sectionCount || result.entries[index] != null) {
                continue;
            }
            SectionEntry entry = SectionEntry.decode(section);
            if (entry != null) {
                result.entries[index] = entry;
            }
            if (section.contains("materials", Tag.TAG_COMPOUND)) {
                result.replaceMaterials(
                        sectionY, MaterialSectionState.decode(section.getCompound("materials")));
            }
        }
        return result.isEmpty() ? null : result;
    }

    public void encode(CompoundTag chunkTag) {
        if (isEmpty()) {
            chunkTag.remove(ROOT_TAG);
            return;
        }
        CompoundTag root = new CompoundTag();
        root.putInt("version", FORMAT_VERSION);
        ListTag sections = new ListTag();
        for (int index = 0; index < entries.length; index++) {
            SectionEntry entry = entries[index];
            MaterialSectionState material = materials(minimumSectionY + index);
            if (entry != null || material != null) {
                CompoundTag section =
                        entry == null ? new CompoundTag() : entry.encode(minimumSectionY + index);
                section.putInt("y", minimumSectionY + index);
                if (material != null) section.put("materials", material.encode());
                sections.add(section);
            }
        }
        root.put("sections", sections);
        chunkTag.put(ROOT_TAG, root);
    }

    public boolean replace(int sectionY, SectionEntry entry) {
        int index = index(sectionY);
        SectionEntry previous = entries[index];
        if (SectionEntry.contentEquals(previous, entry)) {
            return false;
        }
        entries[index] = entry;
        return true;
    }

    public MaterialSectionState materials(int sectionY) {
        int index = sectionY - minimumSectionY;
        return index < 0 || index >= materialEntries.length || materialEntries[index] == null
                ? null
                : materialEntries[index].snapshot();
    }

    /** Mutation-path lookup does not publish a snapshot or trigger its next copy-on-write. */
    public boolean hasMaterials(int sectionY) {
        int index = sectionY - minimumSectionY;
        return index >= 0 && index < materialEntries.length && materialEntries[index] != null;
    }

    public boolean hasMaterial(int sectionY, int position) {
        int index = sectionY - minimumSectionY;
        return index >= 0
                && index < materialEntries.length
                && materialEntries[index] != null
                && materialEntries[index].find(position) >= 0;
    }

    /** A scalar thermometer read shares no arrays with its caller. */
    public MaterialRead readMaterial(int sectionY, int position, int stateId, MaterialSample out) {
        int index = sectionY - minimumSectionY;
        return index >= 0 && index < materialEntries.length && materialEntries[index] != null
                ? materialEntries[index].read(position, stateId, out)
                : MaterialRead.MISSING;
    }

    /** Main-thread projection of an already-read checkpoint; never changes its stored H or timestamp. */
    public void projectMaterial(
            ServerLevel level,
            BlockPos position,
            MaterialThermalLaw currentLaw,
            MaterialSample out,
            BlockPos.MutableBlockPos naturalPosition) {
        long tick = level.getGameTime();
        double natural =
                naturalTemperature(
                        level,
                        position.getX() >> 4,
                        position.getY() >> 4,
                        position.getZ() >> 4,
                        tick,
                        naturalPosition);
        DormantThermalCooling.project(
                out,
                tick,
                natural,
                DormantThermalCooling.rate(
                        MinecraftThermalProfiles.dormantTemperatureHalfLifeSeconds()));
        MaterialSectionState.adaptLaw(out, currentLaw);
    }

    public boolean applyMaterialChange(
            int sectionY,
            int position,
            int stateId,
            MaterialThermalLaw law,
            byte cause,
            double naturalC,
            long tick,
            double coolingRate,
            double coolingNaturalC) {
        int index = index(sectionY);
        var editor = materialEntries[index];
        if (editor == null
                || !editor.applyChange(
                        position,
                        stateId,
                        law,
                        cause,
                        naturalC,
                        tick,
                        coolingRate,
                        coolingNaturalC)) return false;
        if (editor.isEmpty()) materialEntries[index] = null;
        markMaterialChanged(index);
        return true;
    }

    public boolean updateMaterial(
            int sectionY,
            int position,
            int stateId,
            MaterialThermalLaw law,
            double energyJ,
            byte branch,
            long tick) {
        var editor = materialEntries[index(sectionY)];
        if (editor == null
                || !(law == null
                        ? editor.remove(position)
                        : editor.update(position, stateId, law, energyJ, branch, tick)))
            return false;
        if (editor.isEmpty()) materialEntries[index(sectionY)] = null;
        markMaterialChanged(index(sectionY));
        return true;
    }

    public boolean mergeMaterials(int sectionY, MaterialSectionState current, long sampledBricks) {
        MaterialSectionState previous = materials(sectionY);
        MaterialSectionState next = MaterialSectionState.merge(previous, current, sampledBricks);
        if (MaterialSectionState.contentEquals(previous, next)) return false;
        replaceMaterials(sectionY, next);
        return true;
    }

    public void replaceMaterials(int sectionY, MaterialSectionState state) {
        int index = index(sectionY);
        if (MaterialSectionState.contentEquals(materials(sectionY), state)) return;
        materialEntries[index] = state == null ? null : new MaterialSectionState.Editor(state);
        markMaterialChanged(index);
    }

    private void markMaterialChanged(int index) {
        if (materialRevisions == null) materialRevisions = new long[materialEntries.length];
        materialRevisions[index] = nextMaterialRevision();
    }

    public long materialRevision(int sectionY) {
        int index = sectionY - minimumSectionY;
        return materialRevisions == null || index < 0 || index >= materialRevisions.length
                ? 0
                : materialRevisions[index];
    }

    public long storedBrickMask(int sectionY) {
        int index = sectionY - minimumSectionY;
        return index < 0 || index >= entries.length || entries[index] == null
                ? 0L
                : entries[index].brickMask;
    }

    public double sample(
            int sectionY,
            int brick,
            long gameTick,
            double halfLifeSeconds,
            ServerLevel level,
            int sectionX,
            int sectionZ,
            BlockPos.MutableBlockPos naturalPosition) {
        int index = sectionY - minimumSectionY;
        if (index < 0 || index >= entries.length || brick < 0 || brick >= BRICKS) {
            return Double.NaN;
        }
        SectionEntry entry = entries[index];
        if (entry == null || !entry.hasBrick(brick)) {
            return Double.NaN;
        }
        double natural =
                naturalTemperature(level, sectionX, sectionY, sectionZ, gameTick, naturalPosition);
        return entry.temperature(
                entry.warmestResidual(brick),
                natural,
                DormantThermalCooling.factor(
                        entry.savedGameTick,
                        gameTick,
                        DormantThermalCooling.rate(halfLifeSeconds)));
    }

    public ThermalInputBatch.DormantAirCut admissionCut(
            int sectionY,
            long gameTick,
            double halfLifeSeconds,
            double currentNaturalTemperatureC) {
        int index = sectionY - minimumSectionY;
        if (index < 0 || index >= entries.length || entries[index] == null) {
            return null;
        }
        SectionEntry entry = entries[index];
        return new ThermalInputBatch.DormantAirCut(
                entry,
                currentNaturalTemperatureC,
                DormantThermalCooling.factor(
                        entry.savedGameTick,
                        gameTick,
                        DormantThermalCooling.rate(halfLifeSeconds)));
    }

    public boolean isEmpty() {
        for (MaterialSectionState.Editor material : materialEntries) {
            if (material != null && !material.isEmpty()) return false;
        }
        for (SectionEntry entry : entries) {
            if (entry != null) {
                return false;
            }
        }
        return true;
    }

    public double naturalTemperature(
            ServerLevel level,
            int sectionX,
            int sectionY,
            int sectionZ,
            long gameTick,
            BlockPos.MutableBlockPos naturalPosition) {
        int index = index(sectionY);
        long boundary = Math.floorDiv(gameTick, CACHE_INTERVAL_TICKS) * CACHE_INTERVAL_TICKS;
        if (cachedNaturalTicks == null) {
            cachedNaturalTicks = new long[entries.length];
            cachedNaturalTemperatures = new double[entries.length];
            Arrays.fill(cachedNaturalTicks, Long.MIN_VALUE);
        }
        if (cachedNaturalTicks[index] != boundary) {
            naturalPosition.set(
                    SectionPos.sectionToBlockCoord(sectionX) + 8,
                    SectionPos.sectionToBlockCoord(sectionY) + 8,
                    SectionPos.sectionToBlockCoord(sectionZ) + 8);
            double natural = WorldTemperature.naturalAir(level, naturalPosition);
            if (cachedNaturalTicks[index] == Long.MIN_VALUE
                    || natural != cachedNaturalTemperatures[index]) {
                if (materialEntries[index] != null) markMaterialChanged(index);
                cachedNaturalTemperatures[index] = natural;
            }
            cachedNaturalTicks[index] = boundary;
        }
        return cachedNaturalTemperatures[index];
    }

    private int index(int sectionY) {
        int index = sectionY - minimumSectionY;
        if (index < 0 || index >= entries.length) {
            throw new IllegalArgumentException("sectionY is outside the owning chunk");
        }
        return index;
    }

    public static CaptureResult capture(
            PagePublication publication,
            QueryPublication queries,
            QueryPublication.MutableSample sample,
            double naturalTemperatureC,
            CaptureScratch scratch) {
        long brickMask = 0, sampleTick = -1;
        int exactNodes = 0;
        for (int brick = 0; brick < 64; brick++) {
            var payload = publication.brick(brick);
            int n = payload.airNodeCount();
            scratch.counts[brick] = 0;
            if (payload.firstSlot() < 0 || n == 0) continue;
            double sum = 0;
            int blocks = 0;
            boolean different = false, retained = false;
            short firstResidual = 0;
            for (int node = 0; node < n; node++) {
                if (!queries.tryRead(
                        payload.firstSlot() + node,
                        payload.arenaGeneration(),
                        publication.topologyGeneration(),
                        sample)) return CaptureResult.FAILED;
                if (sampleTick < 0) sampleTick = sample.sampleTick();
                else if (sampleTick != sample.sampleTick()) return CaptureResult.FAILED;
                long mask =
                        payload.blockLayout() == null
                                ? -1L
                                : payload.blockLayout().nodeBlockMask(node);
                int weight = Long.bitCount(mask);
                short residual = quantizeResidual(sample.temperatureC() - naturalTemperatureC);
                int index = brick * 64 + node;
                scratch.nodeMasks[index] = mask;
                scratch.nodeResiduals[index] = residual;
                if (node == 0) firstResidual = residual;
                else different |= residual != firstResidual;
                retained |= Math.abs(residual) > PRUNE_RESIDUAL;
                sum += sample.temperatureC() * weight;
                blocks += weight;
            }
            if (!retained) continue;
            brickMask |= 1L << brick;
            scratch.means[brick] = quantizeResidual(sum / blocks - naturalTemperatureC);
            if (different) {
                scratch.counts[brick] = (byte) n;
                exactNodes += n;
            }
        }
        return new CaptureResult(
                true, fromScratch(sampleTick, naturalTemperatureC, brickMask, exactNodes, scratch));
    }

    private static SectionEntry fromScratch(
            long sampleTick,
            double naturalTemperatureC,
            long brickMask,
            int exactNodes,
            CaptureScratch scratch) {
        if (brickMask == 0) return null;
        int means = Long.bitCount(brickMask);
        // Four residuals per long, plus one long block mask per exact node. Object overhead
        // and the byte counts are not part of this existing numeric payload limit.
        boolean exact =
                8 * ((means + exactNodes + 3) / 4) + 8 * exactNodes <= MAX_AIR_PAYLOAD_BYTES;
        short[] residuals = new short[means + (exact ? exactNodes : 0)];
        long[] masks = new long[exact ? exactNodes : 0];
        byte[] counts = new byte[means];
        int residualIndex = 0, brickRank = 0, nodeMaskIndex = 0;
        for (int brick = 0; brick < 64; brick++) {
            if ((brickMask & 1L << brick) == 0) continue;
            residuals[residualIndex++] = scratch.means[brick];
            int count = exact ? Byte.toUnsignedInt(scratch.counts[brick]) : 0;
            counts[brickRank++] = (byte) count;
            for (int node = 0; node < count; node++) {
                masks[nodeMaskIndex++] = scratch.nodeMasks[brick * 64 + node];
                residuals[residualIndex++] = scratch.nodeResiduals[brick * 64 + node];
            }
        }
        return new SectionEntry(
                sampleTick, naturalTemperatureC, brickMask, counts, pack(residuals), masks);
    }

    /** Only real captures rebase retained Air; serializing an unchanged checkpoint does not. */
    public double captureNatural(int sectionY, long tick, double naturalC) {
        SectionEntry previous = entries[index(sectionY)];
        return previous != null && previous.savedGameTick == tick
                ? previous.savedNaturalC
                : naturalC;
    }

    public boolean mergeAir(
            int sectionY,
            SectionEntry current,
            long sampledBricks,
            long tick,
            double naturalC,
            double rate,
            CaptureScratch scratch) {
        SectionEntry previous = entries[index(sectionY)];
        if (previous == null || (previous.brickMask & ~sampledBricks) == 0)
            return replace(sectionY, current);
        if (current == null && sampledBricks == 0) return false;
        long mask = 0;
        int exactNodes = 0;
        for (int brick = 0; brick < BRICKS; brick++) {
            SectionEntry source = (sampledBricks & 1L << brick) == 0 ? previous : current;
            if (source == null || !source.hasBrick(brick)) continue;
            double factor = DormantThermalCooling.factor(source.savedGameTick, tick, rate);
            short mean =
                    quantizeResidual(source.meanTemperatureC(brick, naturalC, factor) - naturalC);
            boolean retained = Math.abs(mean) > PRUNE_RESIDUAL;
            int count = source.exactCount(brick);
            for (int n = 0; n < count; n++) {
                short value =
                        quantizeResidual(
                                source.temperature(
                                                residualAt(
                                                        source.residuals,
                                                        source.residualOffsets[brick] + 1 + n),
                                                naturalC,
                                                factor)
                                        - naturalC);
                scratch.nodeResiduals[brick * 64 + n] = value;
                scratch.nodeMasks[brick * 64 + n] =
                        source.blockMasks[source.nodeMaskOffsets[brick] + n];
                retained |= Math.abs(value) > PRUNE_RESIDUAL;
            }
            if (!retained) continue;
            mask |= 1L << brick;
            scratch.means[brick] = mean;
            scratch.counts[brick] = (byte) count;
            exactNodes += count;
        }
        return replace(sectionY, fromScratch(tick, naturalC, mask, exactNodes, scratch));
    }

    public static final class CaptureScratch {
        final long[] nodeMasks = new long[4096];
        final short[] nodeResiduals = new short[4096], means = new short[64];
        final byte[] counts = new byte[64];
    }

    public record CaptureResult(boolean valid, SectionEntry entry) {
        private static final CaptureResult FAILED = new CaptureResult(false, null);
    }

    /**
     * Air checkpoint ordered by set bits of brickMask. Each Brick contributes its mean residual,
     * followed by exactCounts[brickRank] node residuals and their corresponding block masks.
     * residualOffsets indexes unpacked short values; nodeMaskOffsets indexes long block masks.
     * These spatial records remain meaningful after arena slots have been recycled.
     */
    public static final class SectionEntry {
        private final long savedGameTick, brickMask;
        private final double savedNaturalC;
        private final byte[] exactCounts;
        private final long[] residuals, blockMasks;
        private final short[] residualOffsets = new short[64], nodeMaskOffsets = new short[64];

        public SectionEntry(
                long savedGameTick,
                double savedNaturalC,
                long brickMask,
                byte[] exactCounts,
                long[] residuals,
                long[] blockMasks) {
            this.savedGameTick = Math.max(0, savedGameTick);
            this.savedNaturalC = savedNaturalC;
            this.brickMask = brickMask;
            this.exactCounts = exactCounts;
            this.residuals = residuals;
            this.blockMasks = blockMasks;
            int residualIndex = 0, nodeMaskIndex = 0, brickRank = 0;
            for (int brick = 0; brick < 64; brick++)
                if (hasBrick(brick)) {
                    residualOffsets[brick] = (short) residualIndex;
                    nodeMaskOffsets[brick] = (short) nodeMaskIndex;
                    int nodeCount = Byte.toUnsignedInt(exactCounts[brickRank++]);
                    residualIndex += 1 + nodeCount;
                    nodeMaskIndex += nodeCount;
                }
        }

        private static SectionEntry decode(CompoundTag tag) {
            long bricks = tag.getLong("bricks");
            byte[] counts = tag.getByteArray("counts");
            long[] values = tag.getLongArray("residuals"), masks = tag.getLongArray("blocks");
            if (bricks == 0 || counts.length != Long.bitCount(bricks)) return null;
            int exact = 0;
            for (byte count : counts) {
                int n = Byte.toUnsignedInt(count);
                if (n > 64) return null;
                exact += n;
            }
            if (masks.length != exact
                    || values.length != (counts.length + exact + 3) / 4
                    || 8 * (values.length + masks.length) > MAX_AIR_PAYLOAD_BYTES) return null;
            int at = 0;
            for (byte count : counts) {
                long seen = 0;
                for (int n = 0; n < Byte.toUnsignedInt(count); n++) {
                    long m = masks[at++];
                    if (m == 0 || (seen & m) != 0) return null;
                    seen |= m;
                }
            }
            return new SectionEntry(
                    tag.getLong("tick"), tag.getDouble("natural"), bricks, counts, values, masks);
        }

        private CompoundTag encode(int sectionY) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("y", sectionY);
            tag.putLong("tick", savedGameTick);
            tag.putDouble("natural", savedNaturalC);
            tag.putLong("bricks", brickMask);
            tag.putByteArray("counts", exactCounts);
            tag.putLongArray("residuals", residuals);
            tag.putLongArray("blocks", blockMasks);
            return tag;
        }

        public boolean hasBrick(int brick) {
            return (brickMask & 1L << brick) != 0;
        }

        private int exactCount(int brick) {
            return Byte.toUnsignedInt(exactCounts[Long.bitCount(brickMask & lowerBits(brick))]);
        }

        private short meanResidual(int brick) {
            return residualAt(residuals, residualOffsets[brick]);
        }

        public double meanTemperatureC(int brick, double natural, double factor) {
            return temperature(meanResidual(brick), natural, factor);
        }

        private double temperature(short residual, double natural, double factor) {
            return DormantThermalCooling.temperature(
                    savedNaturalC + residual / (double) RESIDUAL_SCALE, natural, factor);
        }

        public void fillBlockTemperatures(
                int brick, double natural, double factor, double[] target) {
            Arrays.fill(target, 0, 64, meanTemperatureC(brick, natural, factor));
            int count = exactCount(brick),
                    offset = residualOffsets[brick] + 1,
                    masks = nodeMaskOffsets[brick];
            for (int i = 0; i < count; i++) {
                double t = temperature(residualAt(residuals, offset + i), natural, factor);
                long mask = blockMasks[masks + i];
                while (mask != 0) {
                    int b = Long.numberOfTrailingZeros(mask);
                    mask &= mask - 1;
                    target[b] = t;
                }
            }
        }

        public short warmestResidual(int brick) {
            int n = exactCount(brick);
            short warmest = n == 0 ? meanResidual(brick) : Short.MIN_VALUE;
            for (int i = 0; i < n; i++)
                warmest =
                        (short)
                                Math.max(
                                        warmest,
                                        residualAt(residuals, residualOffsets[brick] + 1 + i));
            return warmest;
        }

        private static boolean contentEquals(SectionEntry a, SectionEntry b) {
            return a == b
                    || a != null
                            && b != null
                            && a.savedGameTick == b.savedGameTick
                            && a.savedNaturalC == b.savedNaturalC
                            && a.brickMask == b.brickMask
                            && Arrays.equals(a.exactCounts, b.exactCounts)
                            && Arrays.equals(a.residuals, b.residuals)
                            && Arrays.equals(a.blockMasks, b.blockMasks);
        }
    }

    private static long[] pack(short[] values) {
        long[] result = new long[(values.length + 3) / 4];
        for (int i = 0; i < values.length; i++) putResidual(result, i, values[i]);
        return result;
    }

    private static short quantizeResidual(double residualC) {
        if (!Double.isFinite(residualC)) {
            throw new IllegalArgumentException("temperature residual must be finite");
        }
        return (short)
                Math.max(
                        Short.MIN_VALUE,
                        Math.min(Short.MAX_VALUE, Math.round(residualC * RESIDUAL_SCALE)));
    }

    private static long lowerBits(int bit) {
        return bit == 0 ? 0L : -1L >>> (Long.SIZE - bit);
    }

    private static short residualAt(long[] packed, int index) {
        return (short) (packed[index >>> 2] >>> ((index & 3) << 4));
    }

    private static void putResidual(long[] packed, int index, short value) {
        packed[index >>> 2] |= ((long) value & 0xffffL) << ((index & 3) << 4);
    }
}
