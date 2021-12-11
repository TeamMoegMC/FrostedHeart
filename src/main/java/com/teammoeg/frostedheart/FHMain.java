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

import com.cannolicatfish.rankine.init.RankineItems;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.simibubi.create.AllItems;
import com.teammoeg.frostedheart.client.ClientProxy;
import com.teammoeg.frostedheart.client.particles.FHParticleTypes;
import com.teammoeg.frostedheart.climate.ClimateData;
import com.teammoeg.frostedheart.climate.chunkdata.ChunkDataCapabilityProvider;
import com.teammoeg.frostedheart.compat.CreateCompat;
import com.teammoeg.frostedheart.compat.CuriosCompat;
import com.teammoeg.frostedheart.crash.ClimateCrash;
import com.teammoeg.frostedheart.events.ClientRegistryEvents;
import com.teammoeg.frostedheart.events.PEEvents;
import com.teammoeg.frostedheart.network.PacketHandler;
import com.teammoeg.frostedheart.research.FHResearch;
import com.teammoeg.frostedheart.research.Research;
import com.teammoeg.frostedheart.research.ResearchCategories;
import com.teammoeg.frostedheart.research.ResearchDataManager;
import com.teammoeg.frostedheart.resources.FHRecipeReloadListener;
import com.teammoeg.frostedheart.util.BlackListPredicate;
import com.teammoeg.frostedheart.util.ChException;
import com.teammoeg.frostedheart.util.FHProps;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
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
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import javax.annotation.Nonnull;
import java.io.InputStreamReader;

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
        DistExecutor.safeRunWhenOn(Dist.CLIENT,()->ClientProxy::setup);
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
    	JsonParser gs=new JsonParser();
    	JsonObject jo=gs.parse(new InputStreamReader(ClientRegistryEvents.class.getClassLoader().getResourceAsStream(FHMain.MODID+".mixins.json"))).getAsJsonObject();
    	JsonArray mixins=jo.get("mixins").getAsJsonArray();
    	
        if(!mixins.contains(new JsonPrimitive("projecte.MixinPhilosopherStone"))||
        !mixins.contains(new JsonPrimitive("projecte.MixinTransmutationStone"))||
        !mixins.contains(new JsonPrimitive( "projecte.MixinTransmutationTablet")))
        	throw new ChException.作弊者禁止进入();
    }

    public void setup(final FMLCommonSetupEvent event) {
    	
    	MinecraftForge.EVENT_BUS.addListener(this::serverStart);
    	MinecraftForge.EVENT_BUS.addListener(this::serverSave);
    	MinecraftForge.EVENT_BUS.register(new FHRecipeReloadListener(null));
    	
    	if(ModList.get().isLoaded("projecte")) {
    		MinecraftForge.EVENT_BUS.addListener(PEEvents::onRC);
    		System.out.println("pe loaded");
    	}else
    	try {
    		Class.forName("moze_intel.projecte.PECore");
    		MinecraftForge.EVENT_BUS.addListener(PEEvents::onRC);
    	}catch(Exception ignored){}
        ChunkDataCapabilityProvider.setup();
        CrashReportExtender.registerCrashCallable(new ClimateCrash());

        ClimateData.setup();

        ResearchCategories.init();
        FHResearch.researches.register(new Research("coal_hand_stove", ResearchCategories.LIVING, FHContent.FHItems.hand_stove));
        FHResearch.researches.register(new Research("snow_boots", ResearchCategories.EXPLORATION, RankineItems.SNOWSHOES.get()));
        FHResearch.researches.register(new Research("mechanics", ResearchCategories.ARS, AllItems.GOGGLES.get()));
        FHResearch.researches.register(new Research("steam_properties", ResearchCategories.ARS, FHContent.FHItems.steam_bottle));
        FHResearch.researches.register(new Research("steam_cannon", ResearchCategories.ARS, AllItems.POTATO_CANNON.get(),
                FHResearch.getResearch("mechanics"), FHResearch.getResearch("steam_properties")));
        FHResearch.researches.register(new Research("sulfuric_acid", ResearchCategories.PRODUCTION, RankineItems.SULFUR.get()));
        FHResearch.researches.register(new Research("aluminum_extraction", ResearchCategories.PRODUCTION, RankineItems.ALUMINUM_INGOT.get(),
                FHResearch.getResearch("sulfuric_acid")));
        FHResearch.researches.register(new Research("generator_t1", ResearchCategories.RESCUE,  FHContent.FHItems.energy_core));
        FHResearch.researches.register(new Research("generator_t2", ResearchCategories.RESCUE, FHResearch.getResearch("generator_t1")));
        FHResearch.researches.register(new Research("generator_t3", ResearchCategories.RESCUE, FHResearch.getResearch("generator_t2")));
        FHResearch.researches.register(new Research("generator_t4", ResearchCategories.RESCUE, FHResearch.getResearch("generator_t3")));
        FHResearch.indexResearches();

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
