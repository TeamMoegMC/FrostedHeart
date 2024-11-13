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

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.teammoeg.frostedheart.base.team.FHTeamDataManager;
import com.teammoeg.frostedheart.base.team.SpecialDataTypes;
import com.teammoeg.frostedheart.compat.CreateCompat;
import com.teammoeg.frostedheart.compat.CuriosCompat;
import com.teammoeg.frostedheart.compat.tetra.TetraCompat;
import com.teammoeg.frostedheart.content.climate.player.SurroundingTemperatureSimulator;
import com.teammoeg.frostedheart.content.research.FHResearch;
import com.teammoeg.frostedheart.data.FHRecipeReloadListener;
import com.teammoeg.frostedheart.events.FTBTeamsEvents;
import com.teammoeg.frostedheart.loot.FHLoot;
import com.teammoeg.frostedheart.mixin.minecraft.FoodAccess;
import com.teammoeg.frostedheart.util.RegistryUtils;
import com.teammoeg.frostedheart.util.TranslateUtils;
import com.teammoeg.frostedheart.util.constants.FHProps;
import com.teammoeg.frostedheart.util.creativeTab.ICreativeModeTabItem;
import com.teammoeg.frostedheart.util.creativeTab.TabType;
import com.teammoeg.frostedheart.util.utility.BlackListPredicate;
import com.teammoeg.frostedheart.util.version.FHRemote;
import com.teammoeg.frostedheart.util.version.FHVersion;
import com.teammoeg.frostedheart.world.FHBiomeModifiers;
import com.teammoeg.frostedheart.world.FHBiomes;
import com.teammoeg.frostedheart.world.FHFeatures;
import dev.ftb.mods.ftbteams.api.event.TeamEvent;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameRules.IntegerValue;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

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
    public static final CreateRegistrate FH_REGISTRATE = CreateRegistrate.create(MODID);
	public static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
	public static final RegistryObject<CreativeModeTab> main = TABS.register("frostedheart_main",
            ()->CreativeModeTab
                    .builder()
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(()->new ItemStack(FHItems.energy_core.get()))
                    .title(TranslateUtils.translate("itemGroup.frostedheart"))
                    .displayItems(FHMain::fillFHTab)
                    .build());
    public static final TabType itemGroup = new TabType(main);

    public static ResourceLocation rl(String path) {
        return new ResourceLocation(MODID, path);
    }

    public static void fillFHTab(CreativeModeTab.ItemDisplayParameters parms, CreativeModeTab.Output out) {
        for (final RegistryObject<Item> itemRef : FHItems.registry.getEntries()) {
            final Item item = itemRef.get();
            if (item instanceof ICreativeModeTabItem) {
                continue;
            }
            else
                out.accept(itemRef.get());
        }
    }
    public FHMain() {
        // Config
        FHConfig.register();

        IEventBus mod = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forge = MinecraftForge.EVENT_BUS;

        // FH Remote Version Check
        local = new FHRemote.FHLocal();
        remote = new FHRemote();
        if (local.fetchVersion().resolve().orElse(FHVersion.empty).getOriginal().contains("pre"))
            pre = new FHRemote.FHPreRemote();
        FHMain.LOGGER.info("TWR Version: " + local.fetchVersion().resolve().orElse(FHVersion.empty).getOriginal());

        // Registrate
        FH_REGISTRATE.registerEventListeners(mod);

        // Forge bus
        forge.addListener(this::serverStart);
        forge.addListener(this::serverSave);
        forge.addListener(this::serverStop);
        forge.register(new FHRecipeReloadListener(null));

        // Mod bus
        mod.addListener(this::setup);
        mod.addListener(this::processIMC);
        mod.addListener(this::enqueueIMC);
        mod.addListener(this::loadComplete);

        // Compat init
        CreateCompat.init();
        TetraCompat.init();
        SpecialDataTypes.init();

        // Early init
        FHProps.init();
        FHItems.init();
        FHBlocks.init();
        FHBiomes.init();
        FHMultiblocks.Multiblock.init();

        // Registration
        FHMain.TABS.register(mod);
        FHItems.registry.register(mod);
        FHBlocks.registry.register(mod);
        FHBlockEntityTypes.REGISTER.register(mod);
        FHEntityTypes.ENTITY_TYPES.register(mod);
        FHFluids.FLUIDS.register(mod);
        FHSoundEvents.SOUNDS.register(mod);
        FHMenuTypes.CONTAINERS.register(mod);
        FHRecipes.RECIPE_SERIALIZERS.register(mod);
        FHRecipes.RECIPE_TYPES.register(mod);
        FHParticleTypes.REGISTER.register(mod);
        FHFeatures.FEATURES.register(mod);
        FHBiomes.BIOME_REGISTER.register(mod);
        FHBiomeModifiers.BIOME_MODIFIERS.register(mod);
        FHAttributes.REGISTER.register(mod);
        FHMobEffects.EFFECTS.register(mod);
        FHLoot.LC_REGISTRY.register(mod);
        FHLoot.LM_REGISTRY.register(mod);

        // Event registration
        TeamEvent.PLAYER_CHANGED.register(FTBTeamsEvents::syncDataWhenTeamChange);
        TeamEvent.CREATED.register(FTBTeamsEvents::syncDataWhenTeamCreated);
        TeamEvent.DELETED.register(FTBTeamsEvents::syncDataWhenTeamDeleted);
        TeamEvent.OWNERSHIP_TRANSFERRED.register(FTBTeamsEvents::syncDataWhenTeamTransfer);
        ItemPredicate.register(new ResourceLocation(MODID, "blacklist"), BlackListPredicate::new);

        // Client setup
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> FHClient::setupClient);
    }

    /**
     * Where miscellaneous setup is done
     * @param event
     */
    private void setup(final FMLCommonSetupEvent event) {
        FHNetwork.register();
        FHCapabilities.setup();
        SurroundingTemperatureSimulator.init();
        // modify default value
        GameRules.GAME_RULE_TYPES.put(GameRules.RULE_SPAWN_RADIUS, IntegerValue.create(0));
    }

    public void serverSave(final LevelEvent.Save event) {
        if (FHTeamDataManager.INSTANCE != null) {
            FHResearch.save();
            FHTeamDataManager.INSTANCE.save();
            //FHScenario.save(); // TODO: Scenrario save
        }
    }

    public void serverStart(final ServerAboutToStartEvent event) {
        new FHTeamDataManager(event.getServer());
        FHResearch.load();
        FHTeamDataManager.INSTANCE.load();

    }

    public void serverStop(final ServerStoppedEvent event) {
        FHTeamDataManager.INSTANCE = null;
    }

    private void enqueueIMC(final InterModEnqueueEvent event) {
        CuriosCompat.sendIMCS();
    }

    private void loadComplete(FMLLoadCompleteEvent event) {
        for (Item i : RegistryUtils.getItems()) {
            if (i.isEdible()) {
                if (RegistryUtils.getRegistryName(i).getNamespace().equals("crockpot")) {
                    ((FoodAccess) i.getFoodProperties()).getEffectsSuppliers().removeIf(t -> t.getFirst().get().getEffect().isBeneficial());
                }
            }
        }
    }

    private void processIMC(final InterModProcessEvent event) {

    }
}
