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

package com.teammoeg.frostedheart.compat.tetra;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.util.CRegistryHelper;
import com.teammoeg.frostedheart.bootstrap.common.ToolCompat;
import com.teammoeg.frostedheart.content.wheelmenu.Selection;
import com.teammoeg.frostedheart.content.wheelmenu.WheelMenuSelectionRegisterEvent;

import dev.ftb.mods.ftbquests.item.FTBQuestsItems;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import se.mickelus.tetra.blocks.workbench.gui.WorkbenchStatsGui;
import se.mickelus.tetra.gui.stats.StatsHelper;
import se.mickelus.tetra.gui.stats.bar.GuiStatBarTool;
import se.mickelus.tetra.items.modular.impl.holo.ModularHolosphereItem;
import se.mickelus.tetra.items.modular.impl.holo.gui.HoloGui;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloStatsGui;

public class TetraClient {
    public static void init() {
        WorkbenchStatsGui.addBar(new GuiStatBarTool(0, 0, StatsHelper.barLength, ToolCompat.coreSpade));
        WorkbenchStatsGui.addBar(new GuiStatBarTool(0, 0, StatsHelper.barLength, ToolCompat.geoHammer));
        WorkbenchStatsGui.addBar(new GuiStatBarTool(0, 0, StatsHelper.barLength, ToolCompat.proPick));
        HoloStatsGui.addBar(new GuiStatBarTool(0, 0, StatsHelper.barLength, ToolCompat.coreSpade));
        HoloStatsGui.addBar(new GuiStatBarTool(0, 0, StatsHelper.barLength, ToolCompat.geoHammer));
        HoloStatsGui.addBar(new GuiStatBarTool(0, 0, StatsHelper.barLength, ToolCompat.proPick));
        MinecraftForge.EVENT_BUS.addListener(TetraClient::registerWheelSelections);
    }
    public static void registerWheelSelections(WheelMenuSelectionRegisterEvent event) {
    	event.register(new ResourceLocation("tetra","holo"),new Selection(Component.translatable("block.tetra.holosphere"), CIcons.getIcon(new ResourceLocation("tetra","textures/item/module/holo/frame/default.png")),
			s ->{
				HoloGui gui = HoloGui.getInstance();

		        Minecraft.getInstance().setScreen(gui);
		        gui.onShow();
			}));
    }
}
