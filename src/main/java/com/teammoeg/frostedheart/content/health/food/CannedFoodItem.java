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

package com.teammoeg.frostedheart.content.health.food;

import java.util.List;

import com.teammoeg.frostedheart.item.FHBaseItem;
import com.teammoeg.frostedheart.util.client.Lang;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

public class CannedFoodItem extends FHBaseItem {

    boolean showtt = true;

    public CannedFoodItem(Properties properties) {
        super(properties);
    }

    public CannedFoodItem(Properties properties, boolean showtt) {
        super( properties);
        this.showtt = showtt;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        if (showtt)
            tooltip.add(Lang.translateTooltip("canned_food").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level worldIn, LivingEntity entityLiving) {
        ItemStack itemstack = super.finishUsingItem(stack, worldIn, entityLiving);
        //entityLiving.getCapability(WaterLevelCapability.PLAYER_WATER_LEVEL).ifPresent(e -> e.reduceLevel(this.getFoodProperties().getNutrition()));
        return itemstack;
    }

}
