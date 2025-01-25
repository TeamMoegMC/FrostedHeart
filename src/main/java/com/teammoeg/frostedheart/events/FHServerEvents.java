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

package com.teammoeg.frostedheart.events;

import com.teammoeg.chorda.team.CTeamDataManager;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.player.SurroundingTemperatureSimulator;
import com.teammoeg.frostedheart.content.research.FHResearch;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Events fired only on logical server side.
 *
 * It really can be part of FHCommonEvents, but this is just for organization.
 */
@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FHServerEvents {
    @SubscribeEvent
    public static void serverLevelSave(final LevelEvent.Save event) {
        if (CTeamDataManager.INSTANCE != null) {
            FHResearch.save();
            CTeamDataManager.INSTANCE.save();
            //FHScenario.save(); // TODO: Scenrario save
        }
    }

    // Server Lifecycle Events

    @SubscribeEvent
    public static void serverAboutToStart(final ServerAboutToStartEvent event) {
        new CTeamDataManager(event.getServer());
        FHResearch.load();
        CTeamDataManager.INSTANCE.load();
        SurroundingTemperatureSimulator.init();
    }

    @SubscribeEvent
    public static void serverStarting(final ServerStartingEvent event) {

    }

    @SubscribeEvent
    public static void serverStarted(final ServerStartedEvent event) {

    }

    @SubscribeEvent
    public static void serverStopping(final ServerStoppingEvent event) {

    }

    @SubscribeEvent
    public static void serverStopped(final ServerStoppedEvent event) {
        CTeamDataManager.INSTANCE = null;
    }
}
