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

import com.teammoeg.chorda.client.CInputHelper;
import com.teammoeg.chorda.text.Components;
import com.teammoeg.chorda.text.LangBuilder;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.agriculture.FertilizedDirt;
import com.teammoeg.frostedheart.content.agriculture.Fertilizer.FertilizerGrade;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import com.simibubi.create.foundation.item.TooltipModifier;
import com.teammoeg.chorda.util.TextProgressBarHelper;
import com.teammoeg.frostedheart.content.climate.TemperatureDisplayHelper;
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
        List<Component> stats = getStats(block, context.getItemStack(), context.getEntity(), null, null, 0);
        
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

    public static LangBuilder getFertilizerStorageBar(BlockState farmland) {
        int storage;

        FertilizerGrade grade = farmland.getValue(FertilizedDirt.GRADE);
        storage = farmland.getValue(FertilizedDirt.STORAGE);

        int low = 0;

        int storagePercent = Mth.ceil(Mth.clampedMap(storage, 0, 8, 0, 100));

        // bar
        String s = TextProgressBarHelper.makeProgressBarInterval(8, 0, storage);
        String s1 = s.substring(0, 3);
        String s2 = s.substring(3);

        MutableComponent icon = FHTextIcon.SOIL_THERMOMETER.getIcon();
        switch(farmland.getValue(FertilizedDirt.FERTILIZER)) {
        case ACCELERATED:
        	icon=FHTextIcon.ACCELERATED_FERTILIZER[grade.ordinal()].getIcon();break;
        case INCREASING:
        	icon=FHTextIcon.INCREASING_FERTILIZER[grade.ordinal()].getIcon();break;
        case PRESERVED:
        	icon=FHTextIcon.PRESERVED_FERTILIZER[grade.ordinal()].getIcon();break;
        }
        LangBuilder builder = Lang.builder()
                .add(icon)
                .add(Lang.text(" " + storagePercent + "%")
                        .style(ChatFormatting.GREEN))
                .add(Lang.text(" "))
                .add(Lang.text(s1).style(ChatFormatting.GREEN))
                .add(Lang.text(s2).style(ChatFormatting.GREEN));

        return builder;
    }

    public static LangBuilder getSurvivability(boolean tempCanSurvive, boolean lightCanSurvive, boolean canGrowth, boolean fertilize, boolean withIcon) {
        Component desc;
        if (!tempCanSurvive)
            desc = Lang.tooltip("temp.plant.survivability.will_die").withStyle(ChatFormatting.RED).component();
        else if (lightCanSurvive && canGrowth)
            desc = Lang.tooltip("temp.plant.survivability." + (fertilize ? "suitable" : "can_grow")).withStyle(ChatFormatting.GREEN).component();
        else
            desc = Lang.tooltip("temp.plant.survivability.survive").withStyle(ChatFormatting.YELLOW).component();

        return withIcon
                ? Lang.builder().add(FHTextIcon.SOIL_THERMOMETER.getIcon()).add(Lang.text(" ")).add(desc)
                : Lang.builder().add(desc);
    }

    public static List<Component> getStats(Block block, @Nullable ItemStack stack, @Nullable Player player, @Nullable BlockState farmland, @Nullable BlockPos cropPos, float soilTemp) {
        List<Component> list = new ArrayList<>();
        PlantTempData data = PlantTempData.getPlantData(block);
        // boolean bonemealable = block instanceof BonemealableBlock;
        boolean inWorld = player != null && cropPos != null;

        if (data != null) {
            boolean tempCanSurvive = true;
            boolean lightCanSurvive = true;
            boolean canGrowth = true;
            boolean fertilize = true;
            if (inWorld) {
                tempCanSurvive = soilTemp >= data.minSurvive() && soilTemp <= data.maxSurvive();
                int light = player.level().getBrightness(LightLayer.SKY, cropPos);
                if (light == 0 && player.level().getBlockState(cropPos.above()).getBlock() == Blocks.AIR) {
                    light = player.level().getBrightness(LightLayer.SKY, cropPos.above());
                }
                lightCanSurvive = light >= data.minSkylight() && light <= data.maxSkylight();
                canGrowth = soilTemp >= data.minGrow() && soilTemp <= data.maxGrow();
                fertilize = soilTemp >= data.minFertilize() && soilTemp <= data.maxFertilize();

                Lang.translate("tooltip", "temp.plant.survivability.state")
                        .style(ChatFormatting.GRAY)
                        .add(getSurvivability(tempCanSurvive, lightCanSurvive, canGrowth, fertilize, false))
                        .addTo(list);

                if (!CInputHelper.isShiftKeyDown()) {
                    if (!tempCanSurvive || !canGrowth) {
                        boolean tooLow = soilTemp < data.minGrow() || soilTemp < data.minSkylight();
                        Lang.builder()
                                .add(FHTextIcon.SOIL_THERMOMETER.getIcon())
                                .add(Lang.text(" "))
                                .add(Lang.tooltip("temp.plant.survivability.temp" + (tooLow ? ".low" : ".high"), TemperatureDisplayHelper.toTemperatureFloatString(soilTemp)).withStyle(tooLow ? ChatFormatting.AQUA : ChatFormatting.GOLD))
                                .addTo(list);
                    }
                    if (!lightCanSurvive) {
                        boolean tooLow = light < data.minSkylight();
                        Lang.builder()
                                .add(FHTextIcon.LIGHT.getIcon())
                                .add(Lang.text(" "))
                                .add(Lang.tooltip("temp.plant.survivability.light" + (tooLow ? ".low" : ".high"), Math.round(6.667F * light)).withStyle(tooLow ? ChatFormatting.AQUA : ChatFormatting.GOLD))
                                .addTo(list);
                    }
                    if (farmland != null && (farmland.is(FHBlocks.FERTILIZED_FARMLAND.get()) || farmland.is(FHBlocks.FERTILIZED_DIRT.get()))) {
                        list.add(Component.empty());
                        getFertilizerStorageBar(farmland).addTo(list);
                    }
                    if (!tempCanSurvive || !lightCanSurvive || !fertilize) {
                        list.add(Component.empty());
                        Lang.translate("tooltip", "temp.plant.shift").style(ChatFormatting.GRAY).addTo(list);
                    }
                    return list;
                }
                list.add(Component.empty());
            }

            if(data.shouldShowSurvive()) {
                if (!inWorld || !tempCanSurvive) {
                    Lang.translate("tooltip", "temp.plant.survive").style(tempCanSurvive ? ChatFormatting.GRAY : ChatFormatting.RED).addTo(list);
                    getTempProgressBar(data.minSurvive(), data.maxSurvive()).addTo(list);
                }
            }

            if (!inWorld || !canGrowth) {
                Lang.translate("tooltip", "temp.plant.grow").style(canGrowth ? ChatFormatting.GRAY : ChatFormatting.RED).addTo(list);
                getTempProgressBar(data.minGrow(), data.maxGrow()).addTo(list);
            }

            if (data.shouldShowFertilize()) {
                if (!inWorld || !fertilize) {
                    Lang.translate("tooltip", "temp.plant.fertilize").style(fertilize ? ChatFormatting.GRAY : ChatFormatting.RED).addTo(list);
                    getTempProgressBar(data.minFertilize(), data.maxFertilize()).addTo(list);
                }
            }

            if (!inWorld || !lightCanSurvive) {
                Lang.translate("tooltip", "temp.plant.light").style(lightCanSurvive ? ChatFormatting.GRAY : ChatFormatting.RED).addTo(list);
                getLightProgressBar(data.minSkylight(), data.maxSkylight()).addTo(list);
            }

            if (farmland != null && (farmland.is(FHBlocks.FERTILIZED_FARMLAND.get()) || farmland.is(FHBlocks.FERTILIZED_DIRT.get()))) {
                Lang.translate("tooltip", "temp.plant.fertilizer").style(ChatFormatting.GRAY).addTo(list);
                getFertilizerStorageBar(farmland).addTo(list);
            }


            boolean vulnerableSnow = data.snowVulnerable();
            if (vulnerableSnow) {
                Lang.translate("tooltip", "temp.plant.snow_vulnerable")
                        .style(ChatFormatting.RED)
                        .addTo(list);
            } else {
                if (!inWorld) {
                    Lang.translate("tooltip", "temp.plant.snow_resistant")
                            .style(ChatFormatting.GREEN)
                            .addTo(list);
                }
            }

            boolean vulnerableBlizzard = data.blizzardVulnerable();
            if (vulnerableBlizzard) {
                Lang.translate("tooltip", "temp.plant.blizzard_vulnerable")
                        .style(ChatFormatting.RED)
                        .addTo(list);
            } else {
                if (!inWorld) {
                    Lang.translate("tooltip", "temp.plant.blizzard_resistant")
                            .style(ChatFormatting.GREEN)
                            .addTo(list);
                }
            }
        }

        return list;
    }
}
