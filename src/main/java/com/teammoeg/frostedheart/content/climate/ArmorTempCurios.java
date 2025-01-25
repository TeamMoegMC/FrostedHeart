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

package com.teammoeg.frostedheart.content.climate;

import java.util.UUID;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.teammoeg.frostedheart.bootstrap.common.FHAttributes;
import com.teammoeg.frostedheart.content.climate.data.ArmorTempData;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

public class ArmorTempCurios implements ICurio {
	ArmorTempData data;
	ItemStack stack;
	public ArmorTempCurios(ArmorTempData data,ItemStack stack) {
		this.data=data;
		this.stack=stack;
	}

	@Override
	public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid) {
		Multimap<Attribute, AttributeModifier> mm=HashMultimap.create();
        if(data!=null) {

        	if(data.getInsulation()!=0)
        		mm.put(FHAttributes.INSULATION.get(), new AttributeModifier(uuid,slotContext.identifier(), data.getInsulation(), Operation.ADDITION));
        	if(data.getColdProof()!=0)
        		mm.put(FHAttributes.WIND_PROOF.get(), new AttributeModifier(uuid,slotContext.identifier(), data.getColdProof() , Operation.ADDITION));
        	if(data.getHeatProof()!=0)
        		mm.put(FHAttributes.HEAT_PROOF.get(), new AttributeModifier(uuid,slotContext.identifier(), data.getHeatProof() , Operation.ADDITION));
        }
		return ICurio.super.getAttributeModifiers(slotContext, uuid);
	}

	@Override
	public ItemStack getStack() {
		return stack;
	}

}
