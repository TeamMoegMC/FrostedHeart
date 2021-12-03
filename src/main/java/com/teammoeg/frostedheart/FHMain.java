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

import javax.annotation.Nonnull;

import com.teammoeg.frostedheart.client.particles.FHParticleTypes;
import com.teammoeg.frostedheart.climate.chunkdata.ChunkDataCapabilityProvider;
import com.teammoeg.frostedheart.compat.CreateCompat;
import com.teammoeg.frostedheart.compat.CuriosCompat;
import com.teammoeg.frostedheart.crash.ClimateCrash;
import com.teammoeg.frostedheart.events.PEEvents;
import com.teammoeg.frostedheart.network.PacketHandler;
import com.teammoeg.frostedheart.research.FHResearch;
import com.teammoeg.frostedheart.research.Research;
import com.teammoeg.frostedheart.research.ResearchCategories;
import com.teammoeg.frostedheart.research.ResearchDataManager;
import com.teammoeg.frostedheart.resources.FHRecipeReloadListener;
import com.teammoeg.frostedheart.util.BlackListPredicate;
import com.teammoeg.frostedheart.util.FHProps;

import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.CrashReportExtender;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(FHMain.MODID)
public class FHMain {

    public static final String MODID = "frostedheart";
    public static final String MODNAME = "Frosted Heart";

    public static final ItemGroup itemGroup = new ItemGroup(MODID) {
        @Override
        @Nonnull
        public ItemStack createIcon() {
            return new ItemStack(FHContent.FHBlocks.generator_core_t1.asItem());
        }
    };

    public static ResourceLocation rl(String path) {
        return new ResourceLocation(MODID, path);
    }

    public FHMain() {
        CreateCompat.init();

        IEventBus mod = FMLJavaModLoadingContext.get().getModEventBus();

        mod.addListener(this::setup);
        mod.addListener(this::processIMC);
        mod.addListener(this::enqueueIMC);

        FHConfig.register();
        PacketHandler.register();

        FHProps.init();
        FHContent.FHItems.init();
        FHContent.FHBlocks.init();
        FHContent.FHMultiblocks.init();
        FHContent.registerContainers();
        FHContent.FHTileTypes.REGISTER.register(mod);
        FHFluids.FLUIDS.register(mod);
        FHContent.FHRecipes.RECIPE_SERIALIZERS.register(mod);
        FHParticleTypes.REGISTER.register(mod);
        ItemPredicate.register(new ResourceLocation(MODID,"blacklist"),BlackListPredicate::new);
        DeferredWorkQueue.runLater(FHContent.FHRecipes::registerRecipeTypes);
        ResearchCategories.init();
        FHResearch.researches.register(new Research("generator_t1", ResearchCategories.HEATING));
        FHResearch.researches.register(new Research("generator_t2", ResearchCategories.HEATING, FHResearch.getResearch("generator_t1")));
        FHResearch.researches.register(new Research("generator_t3", ResearchCategories.HEATING, FHResearch.getResearch("generator_t2")));
        FHResearch.researches.register(new Research("generator_t4", ResearchCategories.HEATING, FHResearch.getResearch("generator_t3")));

       
    }

    public void setup(final FMLCommonSetupEvent event) {
    	
    	MinecraftForge.EVENT_BUS.addListener(this::serverStart);
    	MinecraftForge.EVENT_BUS.addListener(this::serverSave);
    	MinecraftForge.EVENT_BUS.register(new FHRecipeReloadListener(null));
    	if(ModList.get().isLoaded("projecte")) {
    		MinecraftForge.EVENT_BUS.addListener(PEEvents::onRC);
    		System.out.println("pe loaded");
    	}
        ChunkDataCapabilityProvider.setup();
        CrashReportExtender.registerCrashCallable(new ClimateCrash());
    }

    private void enqueueIMC(final InterModEnqueueEvent event) {
        CuriosCompat.sendIMCS();
    }
    private void serverStart(final FMLServerAboutToStartEvent event) {
    	new ResearchDataManager(event.getServer()).load();
    }
    private void serverSave(final WorldEvent.Save event) {
    	if(ResearchDataManager.INSTANCE!=null)
    		ResearchDataManager.INSTANCE.save();
    }
    private void processIMC(final InterModProcessEvent event) {

    }
}
