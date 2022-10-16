/*
 * Copyright (c) 2022 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.world;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.world.dimension.RelicDimBiomeProvider;
import com.teammoeg.frostedheart.world.dimension.RelicDimChunkGenerator;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;

public class FHDimensions {
    public static final RegistryKey<DimensionType> RELIC_DIM_TYPE = RegistryKey.getOrCreateKey(Registry.DIMENSION_TYPE_KEY, new ResourceLocation(FHMain.MODID, "relic_dim"));
    public static final RegistryKey<World> RELIC_DIM = RegistryKey.getOrCreateKey(Registry.WORLD_KEY, new ResourceLocation(FHMain.MODID, "relic_dim"));

    public static void register() {
       Registry.register(Registry.CHUNK_GENERATOR_CODEC, new ResourceLocation(FHMain.MODID, "relic_chunkgen"),
                RelicDimChunkGenerator.CODEC);
        Registry.register(Registry.BIOME_PROVIDER_CODEC, new ResourceLocation(FHMain.MODID, "relic_biomeprovider"),
                RelicDimBiomeProvider.CODEC);
    }
}
