package com.teammoeg.frostedheart.content.world.features.deposit;

import java.util.concurrent.atomic.AtomicReferenceArray;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

/** Passes depth through unchanged while carrying the native world-seeded region field. */
public final class DepositRegions implements DensityFunction {
    public record Settings(int cellSize, int jitter, double chance, int minY, int maxY, long salt) {
        static final Codec<Settings> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(384, 4096).optionalFieldOf("cell_size", 512).forGetter(Settings::cellSize),
                Codec.intRange(0, 40).optionalFieldOf("jitter", 32).forGetter(Settings::jitter),
                Codec.doubleRange(0, 1).optionalFieldOf("chance", .35).forGetter(Settings::chance),
                Codec.intRange(-48, -32).optionalFieldOf("min_y", -40).forGetter(Settings::minY),
                Codec.intRange(-32, -24).optionalFieldOf("max_y", -32).forGetter(Settings::maxY),
                Codec.LONG.optionalFieldOf("salt", 48151623L).forGetter(Settings::salt)
        ).apply(i, Settings::new));
    }
    public static final KeyDispatchDataCodec<DepositRegions> CODEC = KeyDispatchDataCodec.of(RecordCodecBuilder.mapCodec(i -> i.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("input").forGetter(v -> v.input),
            DensityFunction.NoiseHolder.CODEC.fieldOf("seed_noise").forGetter(v -> v.seedNoise),
            Settings.CODEC.fieldOf("settings").forGetter(v -> v.settings)
    ).apply(i, DepositRegions::new)));
    private final DensityFunction input;
    private final NoiseHolder seedNoise;
    private final Settings settings;
    private final AtomicReferenceArray<Entry> memo;
    private record Entry(long key, DepositLayout layout) {}

    public DepositRegions(DensityFunction input, NoiseHolder noise, Settings settings) {
        this(input, noise, settings, new AtomicReferenceArray<>(128));
    }
    private DepositRegions(DensityFunction input, NoiseHolder noise, Settings settings, AtomicReferenceArray<Entry> memo) {
        this.input = input; this.seedNoise = noise; this.settings = settings; this.memo = memo;
    }
    public int cellSize() { return settings.cellSize; }
    public boolean affectsHeight(int y, int below, int above) {
        return y >= settings.minY + below && y <= settings.maxY + above;
    }
    public DepositLayout layout(int x, int z) {
        if (seedNoise.noise() == null) return null;
        int cx = Math.floorDiv(x, settings.cellSize), cz = Math.floorDiv(z, settings.cellSize);
        long key = ((long) cx << 32) ^ (cz & 0xffffffffL);
        int slot = (int) DepositLayout.mix(key) & 127;
        Entry cached = memo.get(slot);
        if (cached != null && cached.key == key) return cached.layout;
        long bits = Double.doubleToLongBits(seedNoise.getValue(cx * 17.0 + .25, .5, cz * 31.0 + .75));
        long hash = DepositLayout.mix(bits ^ settings.salt ^ cx * 0x632be59bd9b4e019L ^ cz * 0x9e3779b97f4a7c15L);
        DepositLayout plan = null;
        if (DepositLayout.unit(hash) < settings.chance) {
            int spread = 2 * settings.jitter + 1;
            int centerX = cx * settings.cellSize + settings.cellSize / 2
                    + (int) Math.floorMod(DepositLayout.mix(hash + 1), spread) - settings.jitter;
            int centerZ = cz * settings.cellSize + settings.cellSize / 2
                    + (int) Math.floorMod(DepositLayout.mix(hash + 2), spread) - settings.jitter;
            int y = settings.minY + (int) Math.floorMod(DepositLayout.mix(hash + 3), settings.maxY - settings.minY + 1);
            double angle = DepositLayout.unit(DepositLayout.mix(hash + 4)) * 360;
            plan = new DepositLayout(new BlockPos(centerX, y, centerZ), angle, DepositLayout.mix(hash + 5));
        }
        memo.set(slot, new Entry(key, plan));
        return plan;
    }
    public static DepositRegions resolve(DensityFunction function) {
        while (function instanceof DensityFunctions.HolderHolder holder) function = holder.function().value();
        return function instanceof DepositRegions regions ? regions : null;
    }
    @Override public double compute(FunctionContext context) { return input.compute(context); }
    @Override public void fillArray(double[] values, ContextProvider provider) { input.fillArray(values, provider); }
    @Override public DensityFunction mapAll(Visitor visitor) {
        var noise = visitor.visitNoise(seedNoise);
        return visitor.apply(new DepositRegions(input.mapAll(visitor), noise, settings,
                noise.noise() == seedNoise.noise() ? memo : new AtomicReferenceArray<>(128)));
    }
    @Override public double minValue() { return input.minValue(); }
    @Override public double maxValue() { return input.maxValue(); }
    @Override public KeyDispatchDataCodec<? extends DensityFunction> codec() { return CODEC; }
}
