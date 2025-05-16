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

package com.teammoeg.frostedheart.content.climate.tooltips;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import com.simibubi.create.foundation.item.TooltipModifier;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.chorda.lang.LangBuilder;
import com.teammoeg.chorda.util.TextProgressBarHelper;
import com.teammoeg.frostedheart.content.climate.TemperatureDisplayHelper;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.data.PlantTemperature;
import com.teammoeg.frostedheart.content.climate.data.PlantTempData;
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
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

public class PlantTempStats implements TooltipModifier {
    protected final Block block;
    public PlantTempStats(Block block) {
        this.block = block;
    }

    @Nullable
    public static PlantTempStats create(Item item) {
        if (item instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            PlantTempData data = PlantTempData.getPlantData(block);
            if (data != null) {
                return new PlantTempStats(block);
            }
        }
        return null;
    }

    @Override
    public void modify(ItemTooltipEvent context) {
        List<Component> stats = getStats(block, context.getItemStack(), context.getEntity());
        
        if (!stats.isEmpty()) {
        	KeyControlledDesc desc = new KeyControlledDesc(()->stats, 
                    GLFW.GLFW_KEY_S, 
                    "S", 
                    "holdForTemperature"
            );
            List<Component> tooltip = context.getToolTip();
            tooltip.add(Components.immutableEmpty());
            tooltip.addAll(desc.getCurrentLines());
        }
    }


    public static LangBuilder getTempProgressBar(float min, float max) {
        // remap from -30 to 30 to 0 to 6
        int low = Mth.ceil(Mth.clampedMap(min, -30, 30, 0, 6));
        int high = Mth.ceil(Mth.clampedMap(max, -30, 30, 0, 6));

        // bar
        String s = TextProgressBarHelper.makeProgressBarInterval(6, low, high);
        String s1 = s.substring(0, 3);
        String s2 = s.substring(3);

        LangBuilder builder = Lang.builder()
                .add(FHTextIcon.SOIL_THERMOMETER.getIcon())
                .add(Lang.text(" " + TemperatureDisplayHelper.toTemperatureFloatString(min))
                        .style(min <= 0 ? ChatFormatting.AQUA : ChatFormatting.GOLD))
                .add(Lang.text(" - ").style(ChatFormatting.GRAY))
                .add(Lang.text(TemperatureDisplayHelper.toTemperatureFloatString(max))
                        .style(max <= 0 ? ChatFormatting.AQUA : ChatFormatting.GOLD))
                .add(Lang.text(" "))
                .add(Lang.text(s1).style(ChatFormatting.AQUA))
                .add(Lang.text(s2).style(ChatFormatting.GOLD));

        return builder;
    }

    public static LangBuilder getLightProgressBar(int min, int max) {
        // remap from 0 to 15 to 0 to 6
        int low = Mth.ceil(Mth.clampedMap(min, 0, 15, 0, 6));
        int high = Mth.ceil(Mth.clampedMap(max, 0, 15, 0, 6));
        if (low == high) {
            if (low == 0) {
                high += 1;
            } else if (low == 6) {
                low -= 1;
            } else {
                low -= 1;
            }
        }

        int minPercent = Mth.ceil(Mth.clampedMap(min, 0, 15, 0, 100));
        int maxPercent = Mth.ceil(Mth.clampedMap(max, 0, 15, 0, 100));

        // bar
        String s = TextProgressBarHelper.makeProgressBarInterval(6, low, high);
        String s1 = s.substring(0, 3);
        String s2 = s.substring(3);

        LangBuilder builder = Lang.builder()
                .add(FHTextIcon.LIGHT.getIcon())
                .add(Lang.text(" " + minPercent + "%")
                        .style(ChatFormatting.YELLOW))
                .add(Lang.text(" - ").style(ChatFormatting.GRAY))
                .add(Lang.text(maxPercent + "%")
                        .style(ChatFormatting.YELLOW))
                .add(Lang.text(" "))
                .add(Lang.text(s1).style(ChatFormatting.YELLOW))
                .add(Lang.text(s2).style(ChatFormatting.YELLOW));

        return builder;
    }

    public static List<Component> getStats(Block block, @Nullable ItemStack stack, @Nullable Player player) {
        List<Component> list = new ArrayList<>();
        PlantTempData data = PlantTempData.getPlantData(block);
        // boolean bonemealable = block instanceof BonemealableBlock;

        if (data != null) {
            if(data.shouldShowSurvive()) {
                Lang.translate("tooltip", "temp.plant.survive").style(ChatFormatting.GRAY).addTo(list);
                getTempProgressBar(data.minSurvive(), data.maxSurvive()).addTo(list);
            }
            Lang.translate("tooltip", "temp.plant.grow").style(ChatFormatting.GRAY).addTo(list);
            getTempProgressBar(data.minGrow(), data.maxGrow()).addTo(list);

            if (data.shouldShowFertilize()) {
                Lang.translate("tooltip", "temp.plant.fertilize").style(ChatFormatting.GRAY).addTo(list);
                getTempProgressBar(data.minFertilize(), data.maxFertilize()).addTo(list);
            }
            Lang.translate("tooltip", "temp.plant.light").style(ChatFormatting.GRAY).addTo(list);
            getLightProgressBar(data.minSkylight(), data.maxSkylight()).addTo(list);


            boolean vulnerableSnow = data.snowVulnerable();
            if (vulnerableSnow) {
                Lang.translate("tooltip", "temp.plant.snow_vulnerable")
                        .style(ChatFormatting.RED)
                        .addTo(list);
            } else {
                Lang.translate("tooltip", "temp.plant.snow_resistant")
                        .style(ChatFormatting.GREEN)
                        .addTo(list);
            }

            boolean vulnerableBlizzard = data.blizzardVulnerable();
            if (vulnerableBlizzard) {
                Lang.translate("tooltip", "temp.plant.blizzard_vulnerable")
                        .style(ChatFormatting.RED)
                        .addTo(list);
            } else {
                Lang.translate("tooltip", "temp.plant.blizzard_resistant")
                        .style(ChatFormatting.GREEN)
                        .addTo(list);
            }
        }

        return list;
    }
}
