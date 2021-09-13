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

package com.teammoeg.frostedheart.item;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.climate.IWarmKeepingEquipment;
import com.teammoeg.frostedheart.content.FHContent;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.IArmorMaterial;
import net.minecraft.item.ItemStack;

public class FHBaseArmorItem extends ArmorItem implements IWarmKeepingEquipment{
    public FHBaseArmorItem(String name, IArmorMaterial materialIn, EquipmentSlotType slot, Properties builderIn) {
        super(materialIn, slot, builderIn);
        setRegistryName(FHMain.MODID, name);
        FHContent.registeredFHItems.add(this);
    }

	@Override
	public float getFactor(ItemStack stack) {
		float modifier=1;
		switch(this.getEquipmentSlot()) {
		case FEET:modifier=0.5F;break;
		case LEGS:modifier=1.5F;break;
		case CHEST:modifier=2.5F;break;
		case HEAD:modifier=1.5F;break;
		}
		IArmorMaterial imi=this.getArmorMaterial();
		if(imi==FHArmorMaterial.HAY)
			return modifier*0.05F;
		if(imi==FHArmorMaterial.HIDE)
			return modifier*0.15F;
		if(imi==FHArmorMaterial.WOOL)
			return modifier*0.17F;
		return 0;
	}
}
