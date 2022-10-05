/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.content.foods;

import java.util.List;

import com.teammoeg.frostedheart.base.item.FHBaseItem;
import com.teammoeg.frostedheart.client.util.GuiUtils;

import gloridifice.watersource.common.capability.WaterLevelCapability;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

public class CannedFoodItem extends FHBaseItem {


    public CannedFoodItem(String name, Properties properties) {
        super(name, properties);
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World worldIn, LivingEntity entityLiving) {
        ItemStack itemstack = super.onItemUseFinish(stack, worldIn, entityLiving);
        entityLiving.getCapability(WaterLevelCapability.PLAYER_WATER_LEVEL).ifPresent(e->e.reduceLevel(5));
        return entityLiving instanceof PlayerEntity && ((PlayerEntity) entityLiving).abilities.isCreativeMode ? itemstack : new ItemStack(Items.BOWL);
    }

	@Override
	public void addInformation(ItemStack stack, World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
		tooltip.add(GuiUtils.translateTooltip("canned_food"));
	}

}
