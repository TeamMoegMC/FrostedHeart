package com.teammoeg.frostedheart.world;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.world.dimension.RelicDimBiomeProvider;
import com.teammoeg.frostedheart.world.dimension.RelicDimChunkGenerator;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.Dimension;
import net.minecraft.world.DimensionType;

public class FHDimensions {
    public static final RegistryKey<DimensionType> RELIC_DIM_TYPE = RegistryKey.getOrCreateKey(Registry.DIMENSION_TYPE_KEY, new ResourceLocation(FHMain.MODID, "relic_dim_type"));
    public static final RegistryKey<Dimension> RELIC_DIM = RegistryKey.getOrCreateKey(Registry.DIMENSION_KEY, new ResourceLocation(FHMain.MODID, "relic_dimension"));

    public static void register() {
       Registry.register(Registry.CHUNK_GENERATOR_CODEC, new ResourceLocation(FHMain.MODID, "relic_chunkgen"),
                RelicDimChunkGenerator.CODEC);
        Registry.register(Registry.BIOME_PROVIDER_CODEC, new ResourceLocation(FHMain.MODID, "relic_biomeprovider"),
                RelicDimBiomeProvider.CODEC);
    }
}
