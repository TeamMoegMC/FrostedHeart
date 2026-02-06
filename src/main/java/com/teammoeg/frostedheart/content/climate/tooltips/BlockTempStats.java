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

package com.teammoeg.frostedheart.content.climate.tooltips;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.teammoeg.chorda.text.Components;
import com.teammoeg.chorda.text.LangBuilder;
import com.teammoeg.frostedheart.content.climate.TemperatureDisplayHelper;
import com.teammoeg.frostedheart.content.climate.data.BlockTempData;
import com.teammoeg.frostedheart.util.Lang;
import com.teammoeg.frostedheart.util.client.FHTextIcon;
import com.teammoeg.frostedheart.util.client.KeyControlledDesc;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

public class BlockTempStats implements TooltipModifier {
	protected final Block block;

	public BlockTempStats(Block block) {
		this.block = block;
	}

	@Nullable
	public static BlockTempStats create(Item item) {
		if (item instanceof BlockItem blockItem) {
			Block block = blockItem.getBlock();
			return new BlockTempStats(block);
		}
		return null;
	}

	@Override
	public void modify(ItemTooltipEvent context) {
		final BlockTempData data = BlockTempData.getData(block);
		final ItemStack stack = context.getItemStack();
		final Player player = context.getEntity();

		if (data != null) {
			KeyControlledDesc desc = new KeyControlledDesc(() -> getStats(data, stack, player), GLFW.GLFW_KEY_S, "S",
					"holdForTemperature");
			List<Component> tooltip = context.getToolTip();
			tooltip.add(Components.immutableEmpty());
			tooltip.addAll(desc.getCurrentLines());
		}
	}
	public static List<Component> getStats(Block block, @Nullable ItemStack stack, @Nullable Player player) {
		final BlockTempData data = BlockTempData.getData(block);
		return getStats(data,stack,player);
	}

	public static List<Component> getStats(BlockTempData data, @Nullable ItemStack stack, @Nullable Player player) {

		if (data != null) {
			List<Component> list = new ArrayList<>();
			float heat = data.getTemp();
			heat = (Math.round(heat * 10)) / 10.0F;// round
			String s = TemperatureDisplayHelper.toTemperatureDeltaFloatString(heat);
			Lang.translate("tooltip", "temp.block").style(ChatFormatting.GRAY).addTo(list);

			int progress = Mth.ceil(Mth.clamp(Math.abs(heat) * 0.1, 0, 3));

			LangBuilder builder = Lang.builder().add(FHTextIcon.thermometer.getIcon())
					.add(Lang.text(" " + s + " " + TooltipHelper.makeProgressBar(3, progress))
							.style(heat < 0 ? ChatFormatting.AQUA : ChatFormatting.GOLD));
			builder.addTo(list);
			return list;
		}
		return null;
	}
}
