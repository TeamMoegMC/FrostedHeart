/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart;

import java.io.File;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonParser;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.repack.registrate.util.NonNullLazyValue;
import com.teammoeg.frostedheart.base.team.SpecialDataTypes;
import com.teammoeg.frostedheart.compat.CreateCompat;
import com.teammoeg.frostedheart.compat.CuriosCompat;
import com.teammoeg.frostedheart.compat.tetra.TetraCompat;
import com.teammoeg.frostedheart.content.climate.player.SurroundingTemperatureSimulator;
import com.teammoeg.frostedheart.content.research.FHResearch;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.font.KGlyphProvider;
import com.teammoeg.frostedheart.events.FHRecipeReloadListener;
import com.teammoeg.frostedheart.events.FTBTeamsEvents;
import com.teammoeg.frostedheart.events.PlayerEvents;
import com.teammoeg.frostedheart.mixin.minecraft.FoodAccess;
import com.teammoeg.frostedheart.util.RegistryUtils;
import com.teammoeg.frostedheart.util.constants.FHProps;
import com.teammoeg.frostedheart.util.constants.VersionRemap;
import com.teammoeg.frostedheart.util.utility.BlackListPredicate;
import com.teammoeg.frostedheart.util.version.FHRemote;
import com.teammoeg.frostedheart.util.version.FHVersion;
import com.teammoeg.frostedheart.world.FHBiomes;
import com.teammoeg.frostedheart.world.FHFeatures;
import com.teammoeg.frostedheart.world.FHStructures;

import dev.ftb.mods.ftbteams.event.TeamEvent;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.block.Block;
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
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(FHMain.MODID)
public class FHMain {
   

    public static final String MODID = "frostedheart";
    public static final String MODNAME = "Frosted Heart";
    public static final Logger LOGGER = LogManager.getLogger(MODNAME);
    public static FHRemote remote;
    public static FHRemote local;
    public static FHRemote pre;
    public static File lastbkf;
    public static File lastServerConfig;
    public static boolean saveNeedUpdate;
    public static final NonNullLazyValue<CreateRegistrate> registrate = CreateRegistrate.lazy(MODID);
    public static final ItemGroup itemGroup = new ItemGroup(MODID) {
        @Override
        @Nonnull
        public ItemStack createIcon() {
            return new ItemStack(FHBlocks.generator_core_t1.get().asItem());
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
        FHMain.LOGGER.info("TWR Version: " + local.fetchVersion().resolve().orElse(FHVersion.empty).getOriginal());
        CreateCompat.init();

        IEventBus mod = FMLJavaModLoadingContext.get().getModEventBus();

        mod.addListener(this::setup);
        mod.addListener(this::processIMC);
        mod.addListener(this::enqueueIMC);
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> DynamicModelSetup::setup);
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> KGlyphProvider::addListener);
        mod.addListener(this::modification);
        FHConfig.register();
        TetraCompat.init();
        FHProps.init();
        SpecialDataTypes.init();
        FHItems.registry.register(mod);
        FHBlocks.registry.register(mod);
        FHBlocks.init();
        FHMultiblocks.init();
        FHContainer.registerContainers();
        FHTileTypes.REGISTER.register(mod);
        FHFluids.FLUIDS.register(mod);
        FHSounds.SOUNDS.register(mod);
        FHContainer.CONTAINERS.register(mod);
        FHRecipes.RECIPE_SERIALIZERS.register(mod);
        FHParticleTypes.REGISTER.register(mod);
        FHBiomes.BIOME_REGISTER.register(mod);
        FHAttributes.REGISTER.register(mod);
        FHEffects.EFFECTS.register(mod);
        FHStructures.register(mod);
        TeamEvent.PLAYER_CHANGED.register(FTBTeamsEvents::syncDataWhenTeamChange);
        TeamEvent.CREATED.register(FTBTeamsEvents::syncDataWhenTeamCreated);
        TeamEvent.DELETED.register(FTBTeamsEvents::syncDataWhenTeamDeleted);
        TeamEvent.OWNERSHIP_TRANSFERRED.register(FTBTeamsEvents::syncDataWhenTeamTransfer);
//        FHStructures.STRUCTURE_DEFERRED_REGISTER.register(mod);
        ItemPredicate.register(new ResourceLocation(MODID, "blacklist"), BlackListPredicate::new);
        DeferredWorkQueue.runLater(FHRecipes::registerRecipeTypes);
        JsonParser gs = new JsonParser();
        // remove primal winter blocks not to temper rankine world
        //ModBlocks.SNOWY_TERRAIN_BLOCKS.remove(Blocks.GRASS_BLOCK);
        //ModBlocks.SNOWY_TERRAIN_BLOCKS.remove(Blocks.DIRT);
        //ModBlocks.SNOWY_TERRAIN_BLOCKS.remove(Blocks.PODZOL);
    }

    @SuppressWarnings("unused")
    private void enqueueIMC(final InterModEnqueueEvent event) {
        CuriosCompat.sendIMCS();
    }

    private void missingMapping(MissingMappings<Fluid> miss) {
        ResourceLocation hw = new ResourceLocation(MODID, "hot_water");
        for (Mapping<Fluid> i : miss.getAllMappings()) {
            if (i.key.equals(hw))
                i.remap(RegistryUtils.getFluid(new ResourceLocation("thermopolium", "nail_soup")));
        }
    }

    private void missingMappingB(MissingMappings<Block> miss) {
        for (Mapping<Block> i : miss.getAllMappings()) {
            ResourceLocation rl = VersionRemap.remaps.get(i.key);
            if (rl != null)
                i.remap(RegistryUtils.getBlock(rl));
        }
    }

    private void missingMappingR(MissingMappings<Item> miss) {
        for (Mapping<Item> i : miss.getAllMappings()) {
            ResourceLocation rl = VersionRemap.remaps.get(i.key);
            if (rl != null)
                i.remap(RegistryUtils.getItem(rl));
        }
    }

    public void modification(FMLLoadCompleteEvent event) {
        for (Item i : RegistryUtils.getItems()) {
            if (i.isFood()) {
                if (RegistryUtils.getRegistryName(i).getNamespace().equals("crockpot")) {
                    ((FoodAccess) i.getFood()).getEffectsSuppliers().removeIf(t -> t.getFirst().get().getPotion().isBeneficial());
                }
            }
        }
    }

    @SuppressWarnings("unused")
    private void processIMC(final InterModProcessEvent event) {

    }

    @SuppressWarnings("unused")
    private void serverSave(final WorldEvent.Save event) {
        if (FHTeamDataManager.INSTANCE != null) {
        	FHResearch.save();
            FHTeamDataManager.INSTANCE.save();
            //FHScenario.save();
        }
    }

    private void serverStart(final FMLServerAboutToStartEvent event) {
        new FHTeamDataManager(event.getServer());
        FHResearch.load();
        FHTeamDataManager.INSTANCE.load();

    }

    @SuppressWarnings("unused")
    private void serverStop(final FMLServerStoppedEvent event) {
        FHTeamDataManager.INSTANCE = null;
    }

    @SuppressWarnings("unused")
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
        } else
            try {
                Class.forName("moze_intel.projecte.PECore");
                MinecraftForge.EVENT_BUS.addListener(PlayerEvents::onRC);
            } catch (Throwable ignored) {
            }
        //CrashReportExtender.registerCrashCallable(new ClimateCrash());
        FHNetwork.register();
        FHCapabilities.setup();
        FHBiomes.biomes();
        FHStructures.registerStructureGenerate();
        FHStructures.setupStructures();
        FHFeatures.initFeatures();
        SurroundingTemperatureSimulator.init();
        // modify default value
        GameRules.GAME_RULES.put(GameRules.SPAWN_RADIUS, IntegerValue.create(0));

    }
}
