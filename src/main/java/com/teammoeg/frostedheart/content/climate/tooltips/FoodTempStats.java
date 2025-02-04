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
import com.teammoeg.frostedheart.content.climate.player.ITempAdjustFood;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.util.client.FHTextIcon;
import com.teammoeg.frostedheart.util.client.Lang;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.chorda.lang.LangBuilder;
import com.teammoeg.frostedheart.content.climate.TemperatureDisplayHelper;
import com.teammoeg.frostedheart.content.climate.data.FoodTempData;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class FoodTempStats implements TooltipModifier {
    protected final Item item;
    public FoodTempStats(Item item) {
        this.item = item;
    }

    // Important: We cannot do the getTemp check because data is not available at this time.
//    @Nullable
//    public static FoodTempStats create(Item item) {
//        FHMain.LOGGER.debug("Creating FoodTempStats for " + item);
//        if (FHDataManager.getTempAdjustFood(item) != null) {
//            FHMain.LOGGER.debug("Created FoodTempStats for " + item);
//            return new FoodTempStats(item);
//        }
//        FHMain.LOGGER.debug("Failed to create FoodTempStats for " + item);
//        return null;
//    }

    @Override
    public void modify(ItemTooltipEvent context) {
    	final ITempAdjustFood data = FoodTempData.getTempAdjustFood(context.getItemStack());
    	final ItemStack stack = context.getItemStack();
		final Player player = context.getEntity();
        
        if (data!=null) {
            KeyControlledDesc desc = new KeyControlledDesc(()->getFoodStats(data, stack, player),
                    GLFW.GLFW_KEY_S,
                    "S", 
                    "holdForTemperature"
            );
            List<Component> tooltip = context.getToolTip();
            tooltip.add(Components.immutableEmpty());
            tooltip.addAll(desc.getCurrentLines());
        }
    }

    public static List<Component> getFoodStats(ITempAdjustFood data, ItemStack stack, Player player) {
        List<Component> list = new ArrayList<>();
        
        if (data != null) {
            float env = PlayerTemperatureData.getCapability(player).map(PlayerTemperatureData::getEnvTemp).orElse(0f);
            float heat = (float) (data.getHeat(stack, env) * FHConfig.SERVER.tempSpeed.get());
            String s = TemperatureDisplayHelper.toTemperatureDeltaFloatString(heat);
            Lang.translate("tooltip", "temp.food")
                    .style(ChatFormatting.GRAY)
                    .addTo(list);

            // create a progress that maps heat from -1 to 1 to 0 to 10 based on absolute value
            int progress = Mth.ceil(Mth.clamp(Math.abs(heat) * 3, 0, 3));

            LangBuilder builder = Lang.builder()
                    .add(FHTextIcon.thermometer.getIcon())
                    .add(Lang.text(" " + s + " " + TooltipHelper.makeProgressBar(3, progress))
                            .style(heat < 0 ? ChatFormatting.AQUA : ChatFormatting.GOLD));
            builder.addTo(list);
        }
        return list;
    }
}
