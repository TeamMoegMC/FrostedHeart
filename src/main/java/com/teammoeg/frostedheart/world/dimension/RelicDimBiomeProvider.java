package com.teammoeg.frostedheart.world.dimension;


import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.world.FHBiomes;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryLookupCodec;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class RelicDimBiomeProvider extends BiomeProvider {
    public static final Codec<RelicDimBiomeProvider> CODEC = RecordCodecBuilder.create((builder) -> {
        return builder.group(
                RegistryLookupCodec.getLookUpCodec(Registry.BIOME_KEY).forGetter((provider) -> {
                    return provider.biomeRegistry;
                })).apply(builder, builder.stable(RelicDimBiomeProvider::new));
    });
    private final Registry<Biome> biomeRegistry;
    private final Biome biome;

    public RelicDimBiomeProvider(Registry<Biome> biomeRegistry) {
        super(ImmutableList.of(FHBiomes.RELIC.get()));
        this.biomeRegistry = biomeRegistry;
        this.biome = biomeRegistry.getOrThrow(FHBiomes.makeKey(FHBiomes.RELIC.get()));
    }


    @Override
    protected Codec<? extends BiomeProvider> getBiomeProviderCodec() {
        return CODEC;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public BiomeProvider getBiomeProvider(long seed) {
        return new RelicDimBiomeProvider(biomeRegistry);
    }

    @Override
    public Biome getNoiseBiome(int x, int y, int z) {
        return biome;
    }
}
