/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.geometry.ComponentBrickCompiler;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PagePublication;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;

import java.util.Arrays;
import java.util.function.LongPredicate;

/**
 * LevelChunk 自有的、有界休眠温度 checkpoint。
 *
 * <p>只保存重新进入时需要的 Brick/Air component 温度残差与一次性 source
 * 支持位；不保存 topology、arena slot、source 历史或离线 solver 状态。</p>
 */
public final class DormantChunkThermalState {
    private static final String ROOT_TAG = "FrostedHeartThermal";
    private static final int FORMAT_VERSION = 1;
    private static final int BRICKS = ThermalPageHandle.BASE_BRICK_COUNT;
    private static final int MAX_EXACT_COMPONENTS = 256;
    private static final int MAX_VALUES = MAX_EXACT_COMPONENTS + BRICKS;
    private static final int RESIDUAL_SCALE = 16;
    private static final int PRUNE_RESIDUAL = 4;
    private static final long CACHE_INTERVAL_TICKS = 20L;

    private final int minimumSectionY;
    private final SectionEntry[] entries;
    private long[] cachedDecayTicks;
    private double[] cachedDecayFactors;
    private double[] cachedNaturalTemperatures;

    public DormantChunkThermalState(int minimumSectionY, int sectionCount) {
        if (sectionCount <= 0) {
            throw new IllegalArgumentException("sectionCount must be positive");
        }
        this.minimumSectionY = minimumSectionY;
        entries = new SectionEntry[sectionCount];
    }

    public static DormantChunkThermalState decode(
            CompoundTag chunkTag,
            int minimumSectionY,
            int sectionCount
    ) {
        if (!chunkTag.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            return null;
        }
        CompoundTag root = chunkTag.getCompound(ROOT_TAG);
        if (root.getInt("version") != FORMAT_VERSION) {
            return null;
        }
        DormantChunkThermalState result = new DormantChunkThermalState(
                minimumSectionY, sectionCount);
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
            if (entry != null) {
                sections.add(entry.encode(minimumSectionY + index));
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
        clearDecayCache(index);
        return true;
    }

    public boolean activateLoaded(long gameTick, double halfLifeSeconds) {
        boolean changed = false;
        for (int index = 0; index < entries.length; index++) {
            SectionEntry entry = entries[index];
            if (entry == null || !entry.sourceSustained) {
                continue;
            }
            entries[index] = entry.rebase(
                    gameTick,
                    decayFactor(entry.savedGameTick, gameTick, halfLifeSeconds),
                    true);
            clearDecayCache(index);
            changed = true;
        }
        return changed;
    }

    public boolean rebaseForSave(long gameTick, double halfLifeSeconds) {
        boolean changed = false;
        for (int index = 0; index < entries.length; index++) {
            SectionEntry entry = entries[index];
            if (entry == null) {
                continue;
            }
            SectionEntry rebased = entry.rebase(
                    gameTick,
                    decayFactor(entry.savedGameTick, gameTick, halfLifeSeconds),
                    false);
            if (!SectionEntry.contentEquals(entry, rebased)) {
                entries[index] = rebased;
                clearDecayCache(index);
                changed = true;
            }
        }
        return changed;
    }

    public boolean refreshSourceSupport(
            int sectionX,
            int sectionZ,
            LongPredicate supported
    ) {
        boolean changed = false;
        for (int index = 0; index < entries.length; index++) {
            SectionEntry entry = entries[index];
            if (entry == null) {
                continue;
            }
            long sectionKey = net.minecraft.core.SectionPos.asLong(
                    sectionX, minimumSectionY + index, sectionZ);
            boolean next = supported.test(sectionKey);
            if (entry.sourceSustained != next) {
                entries[index] = entry.withSourceSustained(next);
                changed = true;
            }
        }
        return changed;
    }

    public double sample(
            int sectionY,
            int brick,
            long gameTick,
            double halfLifeSeconds,
            ServerLevel level,
            int sectionX,
            int sectionZ,
            BlockPos.MutableBlockPos naturalPosition
    ) {
        int index = sectionY - minimumSectionY;
        if (index < 0 || index >= entries.length || brick < 0 || brick >= BRICKS) {
            return Double.NaN;
        }
        SectionEntry entry = entries[index];
        if (entry == null || !entry.hasBrick(brick)) {
            return Double.NaN;
        }
        double factor = cachedDecayFactor(
                index, entry, gameTick, halfLifeSeconds,
                level, sectionX, sectionY, sectionZ, naturalPosition);
        return cachedNaturalTemperatures[index]
                + entry.warmestResidual(brick) / (double) RESIDUAL_SCALE * factor;
    }

    public ThermalInputBatch.DormantAirCut admissionCut(
            int sectionY,
            long gameTick,
            double halfLifeSeconds,
            double currentNaturalTemperatureC
    ) {
        int index = sectionY - minimumSectionY;
        if (index < 0 || index >= entries.length || entries[index] == null) {
            return null;
        }
        SectionEntry entry = entries[index];
        return new ThermalInputBatch.DormantAirCut(
                entry,
                currentNaturalTemperatureC,
                decayFactor(entry.savedGameTick, gameTick, halfLifeSeconds));
    }

    public boolean isEmpty() {
        for (SectionEntry entry : entries) {
            if (entry != null) {
                return false;
            }
        }
        return true;
    }

    private double cachedDecayFactor(
            int index,
            SectionEntry entry,
            long gameTick,
            double halfLifeSeconds,
            ServerLevel level,
            int sectionX,
            int sectionY,
            int sectionZ,
            BlockPos.MutableBlockPos naturalPosition
    ) {
        long boundary = Math.floorDiv(gameTick, CACHE_INTERVAL_TICKS)
                * CACHE_INTERVAL_TICKS;
        if (cachedDecayTicks == null) {
            cachedDecayTicks = new long[entries.length];
            cachedDecayFactors = new double[entries.length];
            cachedNaturalTemperatures = new double[entries.length];
            Arrays.fill(cachedDecayTicks, Long.MIN_VALUE);
        }
        if (cachedDecayTicks[index] != boundary) {
            cachedDecayTicks[index] = boundary;
            cachedDecayFactors[index] = decayFactor(
                    entry.savedGameTick, boundary, halfLifeSeconds);
            naturalPosition.set(
                    SectionPos.sectionToBlockCoord(sectionX) + 8,
                    SectionPos.sectionToBlockCoord(sectionY) + 8,
                    SectionPos.sectionToBlockCoord(sectionZ) + 8);
            cachedNaturalTemperatures[index] = WorldTemperature.naturalAir(
                    level, naturalPosition);
        }
        return cachedDecayFactors[index];
    }

    private void clearDecayCache(int index) {
        if (cachedDecayTicks != null) {
            cachedDecayTicks[index] = Long.MIN_VALUE;
        }
    }

    private int index(int sectionY) {
        int index = sectionY - minimumSectionY;
        if (index < 0 || index >= entries.length) {
            throw new IllegalArgumentException("sectionY is outside the owning chunk");
        }
        return index;
    }

    private static double decayFactor(
            long savedGameTick,
            long currentGameTick,
            double halfLifeSeconds
    ) {
        if (!Double.isFinite(halfLifeSeconds) || halfLifeSeconds <= 0.0D) {
            throw new IllegalArgumentException("halfLifeSeconds must be positive");
        }
        long elapsed = Math.max(0L, currentGameTick - savedGameTick);
        return Math.pow(2.0D, -elapsed / (halfLifeSeconds * 20.0D));
    }

    public static CaptureResult capture(
            PagePublication publication,
            QueryPublication queries,
            QueryPublication.MutableSample sample,
            double naturalTemperatureC,
            CaptureScratch scratch
    ) {
        int totalComponents = 0;
        for (int brick = 0; brick < BRICKS; brick++) {
            PagePublication.Brick payload = publication.brick(brick);
            if (payload.coverageSlot() >= 0) {
                totalComponents += payload.mixedGeometry() == null
                        ? 1 : payload.mixedGeometry().componentCount();
            }
        }
        boolean exact = totalComponents <= MAX_EXACT_COMPONENTS;
        int valueCount = 0;
        int countWrite = 0;
        long brickMask = 0L;
        long mixedMask = 0L;
        long commonSampleTick = -1L;

        for (int brick = 0; brick < BRICKS; brick++) {
            PagePublication.Brick payload = publication.brick(brick);
            if (payload.coverageSlot() < 0) {
                continue;
            }
            ComponentBrickCompiler.CompiledBrick mixed = payload.mixedGeometry();
            int components = mixed == null ? 1 : mixed.componentCount();
            double weightedTemperature = 0.0D;
            double totalVolume = 0.0D;
            boolean retainExact = false;
            for (int component = 0; component < components; component++) {
                if (!queries.tryRead(
                        payload.coverageSlot() + component,
                        payload.arenaGeneration(),
                        publication.topologyGeneration(),
                        sample)) {
                    return CaptureResult.FAILED;
                }
                if (commonSampleTick < 0L) {
                    commonSampleTick = sample.sampleTick();
                } else if (commonSampleTick != sample.sampleTick()) {
                    return CaptureResult.FAILED;
                }
                double temperature = sample.temperatureC();
                double volume = mixed == null ? 64.0D : mixed.componentVolume(component);
                weightedTemperature += temperature * volume;
                totalVolume += volume;
                if (exact) {
                    scratch.temperatures[component] = temperature;
                    retainExact |= Math.abs(quantizeResidual(
                            temperature - naturalTemperatureC)) > PRUNE_RESIDUAL;
                }
            }
            short mean = quantizeResidual(
                    weightedTemperature / totalVolume - naturalTemperatureC);
            boolean retained = exact ? retainExact : Math.abs(mean) > PRUNE_RESIDUAL;
            if (!retained) {
                continue;
            }
            brickMask |= 1L << brick;
            scratch.residuals[valueCount++] = mean;
            if (exact && components > 1) {
                mixedMask |= 1L << brick;
                scratch.counts[countWrite++] = (byte) (components - 1);
                for (int component = 0; component < components; component++) {
                    scratch.residuals[valueCount++] = quantizeResidual(
                            scratch.temperatures[component] - naturalTemperatureC);
                }
            }
        }
        if (commonSampleTick < 0L || brickMask == 0L) {
            return new CaptureResult(true, null);
        }
        long[] packed = new long[(valueCount + 3) >>> 2];
        for (int index = 0; index < valueCount; index++) {
            putResidual(packed, index, scratch.residuals[index]);
        }
        SectionEntry entry = new SectionEntry(
                commonSampleTick,
                false,
                brickMask,
                mixedMask,
                Arrays.copyOf(scratch.counts, countWrite),
                packed);
        return new CaptureResult(true, entry);
    }

    public static final class CaptureScratch {
        public CaptureScratch() {
        }

        final double[] temperatures = new double[MAX_EXACT_COMPONENTS];
        final short[] residuals = new short[MAX_VALUES];
        final byte[] counts = new byte[BRICKS];
    }

    public record CaptureResult(boolean valid, SectionEntry entry) {
        private static final CaptureResult FAILED = new CaptureResult(false, null);
    }

    public static final class SectionEntry {
        private final long savedGameTick;
        private final boolean sourceSustained;
        private final long brickMask;
        private final long mixedMask;
        private final byte[] componentCountMinusOne;
        private final long[] residuals;
        private final short[] valueOffsets;
        private final short[] warmestResiduals;

        public SectionEntry(
                long savedGameTick,
                boolean sourceSustained,
                long brickMask,
                long mixedMask,
                byte[] componentCountMinusOne,
                long[] residuals
        ) {
            this.savedGameTick = Math.max(0L, savedGameTick);
            this.sourceSustained = sourceSustained;
            this.brickMask = brickMask;
            this.mixedMask = mixedMask;
            this.componentCountMinusOne = componentCountMinusOne;
            this.residuals = residuals;
            if (mixedMask == 0L) {
                valueOffsets = null;
                warmestResiduals = null;
            } else {
                valueOffsets = new short[BRICKS];
                warmestResiduals = new short[BRICKS];
                buildMixedLookup();
            }
        }

        private static SectionEntry decode(CompoundTag tag) {
            long brickMask = tag.getLong("bricks");
            long mixedMask = tag.getLong("mixed");
            byte[] counts = tag.getByteArray("counts");
            long[] residuals = tag.getLongArray("residuals");
            if (brickMask == 0L || (mixedMask & ~brickMask) != 0L
                    || counts.length != Long.bitCount(mixedMask)) {
                return null;
            }
            int values = Long.bitCount(brickMask);
            int components = Long.bitCount(brickMask) - counts.length;
            for (byte count : counts) {
                int componentCount = Byte.toUnsignedInt(count) + 1;
                values += componentCount;
                components += componentCount;
            }
            if (components > MAX_EXACT_COMPONENTS
                    || values > MAX_VALUES
                    || residuals.length != (values + 3) >>> 2) {
                return null;
            }
            try {
                return new SectionEntry(
                        Math.max(0L, tag.getLong("tick")),
                        tag.getBoolean("supported"),
                        brickMask,
                        mixedMask,
                        counts,
                        residuals);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }

        private CompoundTag encode(int sectionY) {
            CompoundTag result = new CompoundTag();
            result.putInt("y", sectionY);
            result.putLong("tick", savedGameTick);
            result.putBoolean("supported", sourceSustained);
            result.putLong("bricks", brickMask);
            result.putLong("mixed", mixedMask);
            result.putByteArray("counts", componentCountMinusOne);
            result.putLongArray("residuals", residuals);
            return result;
        }

        public boolean hasBrick(int brick) {
            return (brickMask & 1L << brick) != 0L;
        }

        public int componentCount(int brick) {
            if (!hasBrick(brick)) {
                return 0;
            }
            if ((mixedMask & 1L << brick) == 0L) {
                return 1;
            }
            int rank = Long.bitCount(mixedMask & lowerBits(brick));
            return Byte.toUnsignedInt(componentCountMinusOne[rank]) + 1;
        }

        public double meanTemperatureC(
                int brick,
                double naturalTemperatureC,
                double factor
        ) {
            return naturalTemperatureC
                    + meanResidual(brick) / (double) RESIDUAL_SCALE * factor;
        }

        public double componentTemperatureC(
                int brick,
                int component,
                int currentComponentCount,
                double naturalTemperatureC,
                double factor
        ) {
            int storedCount = componentCount(brick);
            short residual = meanResidual(brick);
            if (storedCount == currentComponentCount && storedCount > 1
                    && component >= 0 && component < storedCount) {
                residual = residualAt(residuals, valueOffset(brick) + 1 + component);
            }
            return naturalTemperatureC
                    + residual / (double) RESIDUAL_SCALE * factor;
        }

        public short warmestResidual(int brick) {
            if (!hasBrick(brick)) {
                throw new IllegalArgumentException("Brick is not stored");
            }
            return mixedMask == 0L
                    ? meanResidual(brick)
                    : warmestResiduals[brick];
        }

        private short meanResidual(int brick) {
            if (!hasBrick(brick)) {
                throw new IllegalArgumentException("Brick is not stored");
            }
            return residualAt(residuals, valueOffset(brick));
        }

        private int valueOffset(int brick) {
            return mixedMask == 0L
                    ? Long.bitCount(brickMask & lowerBits(brick))
                    : Short.toUnsignedInt(valueOffsets[brick]) - 1;
        }

        private void buildMixedLookup() {
            int value = 0;
            int count = 0;
            for (int brick = 0; brick < BRICKS; brick++) {
                if (!hasBrick(brick)) {
                    continue;
                }
                valueOffsets[brick] = (short) (value + 1);
                short warmest = residualAt(residuals, value++);
                if ((mixedMask & 1L << brick) != 0L) {
                    int components = Byte.toUnsignedInt(
                            componentCountMinusOne[count++]) + 1;
                    warmest = Short.MIN_VALUE;
                    for (int component = 0; component < components; component++) {
                        warmest = (short) Math.max(
                                warmest, residualAt(residuals, value++));
                    }
                }
                warmestResiduals[brick] = warmest;
            }
        }

        private SectionEntry withSourceSustained(boolean value) {
            return sourceSustained == value ? this : new SectionEntry(
                    savedGameTick,
                    value,
                    brickMask,
                    mixedMask,
                    componentCountMinusOne,
                    residuals);
        }

        private SectionEntry rebase(
                long gameTick,
                double factor,
                boolean applySourceSupport
        ) {
            short[] output = new short[MAX_VALUES];
            byte[] counts = new byte[BRICKS];
            int outputValues = 0;
            int outputCounts = 0;
            long nextBricks = 0L;
            long nextMixed = 0L;
            for (int brick = 0; brick < BRICKS; brick++) {
                if (!hasBrick(brick)) {
                    continue;
                }
                int offset = valueOffset(brick);
                int components = componentCount(brick);
                int vectorLength = components > 1 ? components + 1 : 1;
                double scale = applySourceSupport && warmestResidual(brick) > 0
                        ? 1.0D : factor;
                boolean retained = false;
                for (int vector = 0; vector < vectorLength; vector++) {
                    short transformed = scaleResidual(
                            residualAt(residuals, offset + vector), scale);
                    output[outputValues + vector] = transformed;
                    if ((components == 1 || vector != 0)
                            && Math.abs(transformed) > PRUNE_RESIDUAL) {
                        retained = true;
                    }
                }
                if (!retained) {
                    continue;
                }
                nextBricks |= 1L << brick;
                if (components > 1) {
                    nextMixed |= 1L << brick;
                    counts[outputCounts++] = (byte) (components - 1);
                }
                outputValues += vectorLength;
            }
            if (nextBricks == 0L) {
                return null;
            }
            long[] packed = new long[(outputValues + 3) >>> 2];
            for (int index = 0; index < outputValues; index++) {
                putResidual(packed, index, output[index]);
            }
            return new SectionEntry(
                    Math.max(0L, gameTick),
                    false,
                    nextBricks,
                    nextMixed,
                    Arrays.copyOf(counts, outputCounts),
                    packed);
        }

        private static boolean contentEquals(SectionEntry first, SectionEntry second) {
            if (first == second) {
                return true;
            }
            return first != null && second != null
                    && first.savedGameTick == second.savedGameTick
                    && first.sourceSustained == second.sourceSustained
                    && first.brickMask == second.brickMask
                    && first.mixedMask == second.mixedMask
                    && Arrays.equals(
                            first.componentCountMinusOne,
                            second.componentCountMinusOne)
                    && Arrays.equals(first.residuals, second.residuals);
        }
    }

    private static short quantizeResidual(double residualC) {
        if (!Double.isFinite(residualC)) {
            throw new IllegalArgumentException("temperature residual must be finite");
        }
        return (short) Math.max(
                Short.MIN_VALUE,
                Math.min(Short.MAX_VALUE, Math.round(residualC * RESIDUAL_SCALE)));
    }

    private static short scaleResidual(short residual, double factor) {
        return (short) Math.max(
                Short.MIN_VALUE,
                Math.min(Short.MAX_VALUE, Math.round(residual * factor)));
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
