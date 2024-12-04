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

import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.teammoeg.frostedheart.base.team.FHTeamDataManager;
import com.teammoeg.frostedheart.base.team.SpecialDataTypes;
import com.teammoeg.frostedheart.compat.CreateCompat;
import com.teammoeg.frostedheart.compat.CuriosCompat;
import com.teammoeg.frostedheart.compat.ftbq.FHRewardTypes;
import com.teammoeg.frostedheart.compat.tetra.TetraCompat;
import com.teammoeg.frostedheart.content.climate.player.SurroundingTemperatureSimulator;
import com.teammoeg.frostedheart.content.research.FHResearch;
import com.teammoeg.frostedheart.infrastructure.data.FHRecipeReloadListener;
import com.teammoeg.frostedheart.infrastructure.gen.FHRegistrate;
import com.teammoeg.frostedheart.events.FTBTeamsEvents;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.foundation.loot.FHLoot;
import com.teammoeg.frostedheart.mixin.minecraft.FoodAccess;
import com.teammoeg.frostedheart.util.RegistryUtils;
import com.teammoeg.frostedheart.util.constants.FHProps;
import com.teammoeg.frostedheart.util.utility.BlackListPredicate;
import com.teammoeg.frostedheart.util.version.FHRemote;
import com.teammoeg.frostedheart.util.version.FHVersion;
import com.teammoeg.frostedheart.foundation.world.FHBiomeModifiers;
import com.teammoeg.frostedheart.foundation.world.FHBiomes;
import com.teammoeg.frostedheart.foundation.world.FHFeatures;
import dev.ftb.mods.ftbteams.api.event.TeamEvent;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
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
    public static final FHRegistrate REGISTRATE = FHRegistrate.create(MODID);

    static {
        REGISTRATE.setTooltipModifierFactory(item -> {
            return new ItemDescription.Modifier(item, TooltipHelper.Palette.STANDARD_CREATE);
        });
    }

    public static ResourceLocation rl(String path) {
        return new ResourceLocation(MODID, path);
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

        FHTags.init();

        // Registration
        FHEntityTypes.ENTITY_TYPES.register(mod);
        FHFluids.FLUIDS.register(mod);
        FHFluids.FLUID_TYPES.register(mod);
        FHMobEffects.EFFECTS.register(mod);
        FHParticleTypes.REGISTER.register(mod);
        FHBlockEntityTypes.REGISTER.register(mod);
        FHMenuTypes.CONTAINERS.register(mod);
        FHTabs.TABS.register(mod);
        FHItems.ITEMS.register(mod);
        FHBlocks.BLOCKS.register(mod);

        FHSoundEvents.SOUNDS.register(mod);
        FHRecipes.RECIPE_SERIALIZERS.register(mod);
        FHRecipes.RECIPE_TYPES.register(mod);
        FHFeatures.FEATURES.register(mod);
        FHBiomes.BIOME_REGISTER.register(mod);
        FHBiomeModifiers.BIOME_MODIFIERS.register(mod);
        FHAttributes.REGISTER.register(mod);
        FHLoot.LC_REGISTRY.register(mod);
        FHLoot.LM_REGISTRY.register(mod);

        FHItems.init();
        FHProps.init();
        FHBlocks.init();
        FHBiomes.init();
        FHMultiblocks.Multiblock.init();

        // Compat init
        CreateCompat.init();
        TetraCompat.init();
        SpecialDataTypes.init();
        FHRewardTypes.init();

        // Event registration
        TeamEvent.PLAYER_CHANGED.register(FTBTeamsEvents::syncDataWhenTeamChange);
        TeamEvent.CREATED.register(FTBTeamsEvents::syncDataWhenTeamCreated);
        TeamEvent.DELETED.register(FTBTeamsEvents::syncDataWhenTeamDeleted);
        TeamEvent.OWNERSHIP_TRANSFERRED.register(FTBTeamsEvents::syncDataWhenTeamTransfer);
        ItemPredicate.register(new ResourceLocation(MODID, "blacklist"), BlackListPredicate::new);

        // Registrate
        REGISTRATE.registerEventListeners(mod);

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
        SurroundingTemperatureSimulator.init();
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
