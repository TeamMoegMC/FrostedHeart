package com.teammoeg.frostedheart.world;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.world.biome.VolcanicBiome;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeManager;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class FHBiomes {
    public static final DeferredRegister<Biome> BIOME_REGISTER = DeferredRegister.create(ForgeRegistries.BIOMES, FHMain.MODID);

    public static RegistryObject<Biome> VOLCANIC = BIOME_REGISTER.register("volcanic", () -> new VolcanicBiome().build());

    private static RegistryKey<Biome> makeKey(Biome biome) {
        return RegistryKey.getOrCreateKey(Registry.BIOME_KEY, biome.getRegistryName());
    }

    public static void Biomes(){
        BiomeManager.addBiome(BiomeManager.BiomeType.COOL, new BiomeManager.BiomeEntry(makeKey(VOLCANIC.get()), 2));
    }
}
