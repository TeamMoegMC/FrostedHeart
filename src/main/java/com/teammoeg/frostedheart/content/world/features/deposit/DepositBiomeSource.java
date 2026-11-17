package com.teammoeg.frostedheart.content.world.features.deposit;

import java.util.stream.Stream;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;

/** Uses the same seeded regions as terrain and delegates every other biome query unchanged. */
public final class DepositBiomeSource extends BiomeSource {
    public static final Codec<DepositBiomeSource> CODEC = RecordCodecBuilder.create(i -> i.group(
            Climate.ParameterList.codec(Biome.CODEC.fieldOf("biome")).fieldOf("biomes").forGetter(v -> v.parameters),
            Biome.CODEC.fieldOf("biome").forGetter(v -> v.biome)
    ).apply(i, DepositBiomeSource::new));
    private final BiomeSource base;
    private final Climate.ParameterList<Holder<Biome>> parameters;
    private final Holder<Biome> biome;
    public DepositBiomeSource(Climate.ParameterList<Holder<Biome>> parameters, Holder<Biome> biome) {
        this.parameters = parameters; this.base = MultiNoiseBiomeSource.createFromList(parameters); this.biome = biome;
    }
    @Override protected Codec<? extends BiomeSource> codec() { return CODEC; }
    @Override protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.concat(base.possibleBiomes().stream(), Stream.of(biome)).distinct();
    }
    @Override public Holder<Biome> getNoiseBiome(int qx, int qy, int qz, Climate.Sampler sampler) {
        var regions = DepositRegions.resolve(sampler.depth());
        int x = QuartPos.toBlock(qx), y = QuartPos.toBlock(qy), z = QuartPos.toBlock(qz);
        var plan = regions == null || !regions.affectsHeight(y, -22, 44) ? null : regions.layout(x, z);
        return plan != null && plan.containsBiome(x, y, z) ? biome : base.getNoiseBiome(qx, qy, qz, sampler);
    }
}
