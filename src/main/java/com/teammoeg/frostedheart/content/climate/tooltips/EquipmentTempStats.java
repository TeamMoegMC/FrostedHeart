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

import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.teammoeg.frostedheart.util.client.KeyControlledDesc;
import com.teammoeg.frostedheart.content.climate.player.IHeatingEquipment;
import com.teammoeg.frostedheart.util.client.FHTextIcon;
import com.teammoeg.frostedheart.util.client.Lang;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.chorda.lang.LangBuilder;
import com.teammoeg.frostedheart.content.climate.TemperatureDisplayHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class EquipmentTempStats implements TooltipModifier {
    protected final IHeatingEquipment item;
    public EquipmentTempStats(IHeatingEquipment item) {
        this.item = item;
    }

    @Nullable
    public static EquipmentTempStats create(Item item) {
        if (item instanceof IHeatingEquipment heatingEquipment) {
            return new EquipmentTempStats(heatingEquipment);
        }
        return null;
    }

    @Override
    public void modify(ItemTooltipEvent context) {
        List<Component> stats = getStats(item, context.getItemStack(), context.getEntity());
        
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

    public static List<Component> getStats(IHeatingEquipment item, ItemStack stack, Player player) {
        List<Component> list = new ArrayList<>();
        float heat = item.getEffectiveTempAdded(null, stack,0, 0);
        heat = (Math.round(heat * 2000)) / 1000.0F;
        if (heat != 0) {
            String s = TemperatureDisplayHelper.toTemperatureDeltaFloatString(heat);
            Lang.translate("tooltip", "temp.item")
                    .style(ChatFormatting.GRAY)
                    .addTo(list);

            int progress = Mth.ceil(Mth.clamp(Math.abs(heat) * 0.1, 0, 3));

            LangBuilder builder = Lang.builder()
                    .add(FHTextIcon.thermometer.getIcon())
                    .add(Lang.text(" " + s + " " + TooltipHelper.makeProgressBar(3, progress))
                            .style(heat < 0 ? ChatFormatting.AQUA : ChatFormatting.GOLD));
            builder.addTo(list);
        }
        return list;
    }
}
