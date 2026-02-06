/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedheart.content.waypoint;

import com.teammoeg.chorda.client.icon.FlatIcon;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.wheelmenu.SelectionBuilder;
import com.teammoeg.frostedheart.content.wheelmenu.WheelMenuSelectionRegisterEvent;

import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class WaypointClientEvents {

	@SubscribeEvent
	public static void registerSelection(WheelMenuSelectionRegisterEvent event) {
		SelectionBuilder.create()
			.message(Component.translatable("waypoint.frostedheart.quick_waypoint"))
			.icon(FlatIcon.SIGHT.toCIcon())
			.selected(s -> ClientWaypointManager.fromPickedBlock())
			.register(event, FHMain.rl("waypoint/quick_waypoint"));
		SelectionBuilder.create()
			.message(Component.translatable("waypoint.frostedheart.del_quick_waypoint"))
			.icon(FlatIcon.BOX.toCIcon())
			.color(0xFFFFFF)
			.visibleWhen(s -> ClientWaypointManager.containsWaypoint("picked_block"))
			.selected(s -> ClientWaypointManager.removeWaypoint("picked_block"))
			.register(event, FHMain.rl("waypoint/del_quick_waypoint"));
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
		ClientWaypointManager.clear();
	}
}
