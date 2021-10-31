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

package com.teammoeg.frostedheart.content.heating;

import com.teammoeg.frostedheart.base.item.FHBaseItem;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.climate.IHeatingEquipment;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import javax.annotation.Nullable;
import java.util.List;

public class MushroomBed extends FHBaseItem implements IHeatingEquipment {

    public MushroomBed(String name, Properties properties) {
        super(name, properties);
    }



    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag flag) {
        list.add(GuiUtils.translateTooltip("meme.mushroom").mergeStyle(TextFormatting.GRAY));
    }

    @Override
    public float compute(ItemStack stack, float bodyTemp, float environmentTemp) {
    	if(stack.getDamage()>0) {
	        if (bodyTemp > -1) {
	            this.setDamage(stack,this.getDamage(stack)-1);
	            bodyTemp += this.getMax(stack);
	        }
    	}
        return bodyTemp;
    }


    @Override
	public void fillItemGroup(ItemGroup group, NonNullList<ItemStack> items) {
    	ItemStack is=new ItemStack(this);
    	is.setDamage(is.getMaxDamage());
		items.add(is);
	}



	@Override
    public float getMax(ItemStack stack) {
        return 0.005F;
    }

}
