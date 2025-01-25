/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.scenario.event;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.scenario.EventTriggerType;
import com.teammoeg.frostedheart.content.scenario.FHScenario;
import com.teammoeg.frostedheart.content.scenario.runner.ScenarioConductor;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ScenarioCommonEvents {
    @SubscribeEvent
    public static void attachToPlayer(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof ServerPlayer) {//server-side only capabilities
            ServerPlayer player = (ServerPlayer) event.getObject();
            if (!(player instanceof FakePlayer)) {
                event.addCapability(new ResourceLocation(FHMain.MODID, "scenario"       ), FHCapabilities.SCENARIO.provider());
            }
        }
    }

    @SubscribeEvent
    public static void tickPlayer(TickEvent.PlayerTickEvent event) {
        if (FHConfig.COMMON.enableScenario.get() && event.side == LogicalSide.SERVER && event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            // Scenario runner
            ScenarioConductor runner= FHScenario.getNullable(player);
            if (runner != null)
                runner.tick(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void doPlayerInteract(PlayerInteractEvent ite) {
        if(FHConfig.COMMON.enableScenario.get() && ite.getEntity() instanceof ServerPlayer&&!(ite.getEntity() instanceof FakePlayer)) {
            FHScenario.trigVar(ite.getEntity(), EventTriggerType.PLAYER_INTERACT);
        }
    }
}
