/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart;

import com.alcatrazescapee.primalwinter.common.ModBlocks;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.teammoeg.frostedheart.client.DynamicModelSetup;
import com.teammoeg.frostedheart.client.particles.FHParticleTypes;
import com.teammoeg.frostedheart.climate.ClimateData;
import com.teammoeg.frostedheart.climate.TemperatureSimulator;
import com.teammoeg.frostedheart.climate.chunkdata.ChunkDataCapabilityProvider;
import com.teammoeg.frostedheart.compat.CreateCompat;
import com.teammoeg.frostedheart.compat.CuriosCompat;
import com.teammoeg.frostedheart.crash.ClimateCrash;
import com.teammoeg.frostedheart.data.DeathInventoryData;
import com.teammoeg.frostedheart.events.ClientRegistryEvents;
import com.teammoeg.frostedheart.events.FTBTeamsEvents;
import com.teammoeg.frostedheart.events.PlayerEvents;
import com.teammoeg.frostedheart.network.PacketHandler;
import com.teammoeg.frostedheart.research.Researches;
import com.teammoeg.frostedheart.research.data.FHResearchDataManager;
import com.teammoeg.frostedheart.resources.FHRecipeReloadListener;
import com.teammoeg.frostedheart.util.*;
import com.teammoeg.frostedheart.world.FHBiomes;
import com.teammoeg.frostedheart.world.FHFeatures;
import com.teammoeg.frostedheart.world.FHStructures;

import dev.ftb.mods.ftbteams.event.TeamEvent;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.GameRules;
import net.minecraft.world.GameRules.IntegerValue;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent.MissingMappings;
import net.minecraftforge.event.RegistryEvent.MissingMappings.Mapping;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.CrashReportExtender;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.InputStreamReader;

@Mod(FHMain.MODID)
public class FHMain {
    public static final Logger LOGGER = LogManager.getLogger();

    public static final String MODID = "frostedheart";
    public static final String MODNAME = "Frosted Heart";
    public static FHRemote remote;
    public static FHRemote local;
    public static FHRemote pre;
    public static File lastbkf;
    public static File lastServerConfig;
    public static boolean saveNeedUpdate;

    public static final ItemGroup itemGroup = new ItemGroup(MODID) {
        @Override
        @Nonnull
        public ItemStack createIcon() {
            return new ItemStack(FHBlocks.generator_core_t1.asItem());
        }
    };

    public static ResourceLocation rl(String path) {
        return new ResourceLocation(MODID, path);
    }

    public FHMain() {
        local = new FHRemote.FHLocal();
        remote = new FHRemote();
        if (local.fetchVersion().resolve().orElse(FHVersion.empty).getOriginal().contains("pre"))
            pre = new FHRemote.FHPreRemote();
        System.out.println(local.fetchVersion().resolve().orElse(FHVersion.empty).getOriginal());
        CreateCompat.init();

        IEventBus mod = FMLJavaModLoadingContext.get().getModEventBus();

        mod.addListener(this::setup);
        mod.addListener(this::processIMC);
        mod.addListener(this::enqueueIMC);
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> DynamicModelSetup::setup);
        FHConfig.register();

        FHProps.init();
        FHItems.init();
        FHBlocks.init();
        FHMultiblocks.init();
        FHContent.registerContainers();
        FHTileTypes.REGISTER.register(mod);
        FHFluids.FLUIDS.register(mod);
        FHSounds.SOUNDS.register(mod);
        FHRecipes.RECIPE_SERIALIZERS.register(mod);
        FHParticleTypes.REGISTER.register(mod);
        FHBiomes.BIOME_REGISTER.register(mod);
        TeamEvent.PLAYER_CHANGED.register(FTBTeamsEvents::syncDataWhenTeamChange);
//        FHStructures.STRUCTURE_DEFERRED_REGISTER.register(mod);
        ItemPredicate.register(new ResourceLocation(MODID, "blacklist"), BlackListPredicate::new);
        DeferredWorkQueue.runLater(FHRecipes::registerRecipeTypes);
        JsonParser gs = new JsonParser();
        JsonObject jo = gs.parse(new InputStreamReader(ClientRegistryEvents.class.getClassLoader().getResourceAsStream(FHMain.MODID + ".mixins.json"))).getAsJsonObject();
        JsonArray mixins = jo.get("mixins").getAsJsonArray();

        if (!mixins.contains(new JsonPrimitive("projecte.MixinPhilosopherStone")) ||
                !mixins.contains(new JsonPrimitive("projecte.MixinTransmutationStone")) ||
                !mixins.contains(new JsonPrimitive("projecte.MixinTransmutationTablet")))
            throw new ChException.作弊者禁止进入();
        //remove primal winter blocks not to temper rankine world
        ModBlocks.SNOWY_TERRAIN_BLOCKS.remove(Blocks.GRASS_BLOCK);
        ModBlocks.SNOWY_TERRAIN_BLOCKS.remove(Blocks.DIRT);
        ModBlocks.SNOWY_TERRAIN_BLOCKS.remove(Blocks.PODZOL);
    }

    public void setup(final FMLCommonSetupEvent event) {

        MinecraftForge.EVENT_BUS.addListener(this::serverStart);
        MinecraftForge.EVENT_BUS.addListener(this::serverSave);
        MinecraftForge.EVENT_BUS.addListener(this::serverStop);
        MinecraftForge.EVENT_BUS.register(new FHRecipeReloadListener(null));

        MinecraftForge.EVENT_BUS.addGenericListener(Fluid.class, this::missingMapping);
        MinecraftForge.EVENT_BUS.addGenericListener(Item.class, this::missingMappingR);
        MinecraftForge.EVENT_BUS.addGenericListener(Block.class, this::missingMappingB);
        if (ModList.get().isLoaded("projecte")) {
            MinecraftForge.EVENT_BUS.addListener(PlayerEvents::onRC);
            System.out.println("pe loaded");
        } else
            try {
                Class.forName("moze_intel.projecte.PECore");
                MinecraftForge.EVENT_BUS.addListener(PlayerEvents::onRC);
            } catch (Throwable ignored) {
            }
        ChunkDataCapabilityProvider.setup();
        CrashReportExtender.registerCrashCallable(new ClimateCrash());
        PacketHandler.register();
        ClimateData.setup();
        DeathInventoryData.setup();
        FHBiomes.Biomes();
        FHStructures.registerStructureGenerate();
        FHFeatures.initFeatures();
        TemperatureSimulator.init();
        //modify default value
        GameRules.GAME_RULES.put(GameRules.SPAWN_RADIUS, IntegerValue.create(0));
    }

    private void enqueueIMC(final InterModEnqueueEvent event) {
        CuriosCompat.sendIMCS();
    }

    private void serverStart(final FMLServerAboutToStartEvent event) {
        new FHResearchDataManager(event.getServer());
        

        FHResearchDataManager.INSTANCE.load();

    }

    private void serverStop(final FMLServerStoppedEvent event) {
        FHResearchDataManager.server = null;

    }

    private void serverSave(final WorldEvent.Save event) {
        if (FHResearchDataManager.INSTANCE != null)
            FHResearchDataManager.INSTANCE.save();
    }

    private void processIMC(final InterModProcessEvent event) {

    }

    private void missingMappingR(MissingMappings<Item> miss) {
        ResourceLocation hw = new ResourceLocation(MODID, "hot_water");
        for (Mapping<Item> i : miss.getAllMappings()) {
            ResourceLocation rl = VersionRemap.remaps.get(i.key);
            if (rl != null)
                i.remap(ForgeRegistries.ITEMS.getValue(rl));
        }
    }

    private void missingMappingB(MissingMappings<Block> miss) {
        ResourceLocation hw = new ResourceLocation(MODID, "hot_water");
        for (Mapping<Block> i : miss.getAllMappings()) {
            ResourceLocation rl = VersionRemap.remaps.get(i.key);
            if (rl != null)
                i.remap(ForgeRegistries.BLOCKS.getValue(rl));
        }
    }

    private void missingMapping(MissingMappings<Fluid> miss) {
        ResourceLocation hw = new ResourceLocation(MODID, "hot_water");
        for (Mapping<Fluid> i : miss.getAllMappings()) {
            if (i.key.equals(hw))
                i.remap(ForgeRegistries.FLUIDS.getValue(new ResourceLocation("thermopolium", "nail_soup")));
        }
    }
}
