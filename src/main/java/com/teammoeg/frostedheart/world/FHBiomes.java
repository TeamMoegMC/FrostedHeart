/*
 * Copyright (c) 2022-2024 TeamMoeg
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
import com.teammoeg.frostedheart.util.RegistryUtils;
import com.teammoeg.frostedheart.world.flora.FrozenForestBiome;
import com.teammoeg.frostedheart.world.geology.volcanic.VolcanicBiome;

import net.minecraft.util.RegistryKey;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeManager;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class FHBiomes {
    public static final DeferredRegister<Biome> BIOME_REGISTER = DeferredRegister.create(ForgeRegistries.BIOMES, FHMain.MODID);

    public static RegistryObject<Biome> VOLCANIC = BIOME_REGISTER.register("volcanic", () -> new VolcanicBiome().build());

    public static RegistryObject<Biome> FROZEN_FOREST = BIOME_REGISTER.register("frozen_forest", () -> new FrozenForestBiome().build());
//    public static RegistryObject<Biome> RELIC = BIOME_REGISTER.register("relic", () -> new VolcanicBiome().build());

    public static void biomes() {
        BiomeManager.addBiome(BiomeManager.BiomeType.COOL, new BiomeManager.BiomeEntry(makeKey(VOLCANIC.get()), 5));
        BiomeManager.addBiome(BiomeManager.BiomeType.COOL, new BiomeManager.BiomeEntry(makeKey(FROZEN_FOREST.get()), 8));
    }

    public static RegistryKey<Biome> makeKey(Biome biome) {
        return RegistryKey.getOrCreateKey(Registry.BIOME_KEY, RegistryUtils.getRegistryName(biome));
    }
}
