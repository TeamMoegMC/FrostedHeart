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

package com.teammoeg.frostedheart.content.climate.player;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import java.util.Collection;

import com.teammoeg.frostedheart.bootstrap.common.FHAttributes;
import com.teammoeg.frostedheart.content.climate.data.ArmorTempData;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData.BodyPart;

import lombok.Getter;

public class BodyPartData {
	public final ItemStackHandler clothes;
	@Getter
	float temperature=0;
	BodyPartData(int max_count) {
		this.clothes = new ItemStackHandler(max_count) {

			@Override
			protected void onContentsChanged(int slot) {
				super.onContentsChanged(slot);
				setChanged();
			}

		};
		reset();
	}

	public void load(CompoundTag itemsTag) {
		clothes.deserializeNBT(itemsTag);
		temperature=itemsTag.getFloat("temp");
	}
	public CompoundTag save() {
		CompoundTag tag= clothes.serializeNBT();
		tag.putFloat("temp", temperature);
		return tag;
	}
	public void reset() {
		for (int i = 0; i < clothes.getSlots(); i++)
			clothes.setStackInSlot(i, ItemStack.EMPTY);
	}

	public float getThermalConductivity(BodyPart part, ItemStack equipment) {
		float res = 0f;
		//TODO determine if alphaGem wrote this incorrectly
		//float rate = 0.4f;
		float rate = 0.3f;
		for(int i=0;i<this.clothes.getSlots();i++)
			if(!clothes.getStackInSlot(i).isEmpty())
				rate-= 0.1f;
		if (!equipment.isEmpty()) {
			rate += 0.1f;
			res += rate * sumAttributes(equipment.getAttributeModifiers(part.slot).get(FHAttributes.INSULATION.get()));
			//rate -= 0.1f;
		}
		for (int i = 0; i < clothes.getSlots(); i++) {
			ItemStack it = clothes.getStackInSlot(i);
			if (!it.isEmpty()) {
				ArmorTempData data=ArmorTempData.getData(it, part);
				if(data!=null) {
					rate += 0.1f;
					res += rate * data.getInsulation();
					//rate -= 0.1f;
				}
			}
		}
		return 100 / (100 + res);
	}

	public float getWindResistance(BodyPart part, ItemStack equipment) {
		double res = 0f;
		//TODO determine if alphaGem wrote this incorrectly
		//float rate = 0.3f - this.clothes.getSlots() * 0.1f;
		float rate = 0.4f;
		if (!equipment.isEmpty()) {
			//rate += 0.1f;
			
			res += rate * sumAttributesPercentage(equipment.getAttributeModifiers(part.slot).get(FHAttributes.WIND_PROOF.get()));
			rate-=0.1f;
		}
		for (int i = 0; i < clothes.getSlots(); i++) {
			ItemStack it = clothes.getStackInSlot(i);
			if (!it.isEmpty()) {
				ArmorTempData data=ArmorTempData.getData(it, part);
				if(data!=null) {
					//rate += 0.1f;
					res += rate * data.getColdProof();
					rate-=0.1f;
				}
			}
		}
		return (float) res;
	}

	public static double sumAttributes(Collection<AttributeModifier> attribute) {
		double base = 0;
		double mbase = 1;
		double mtotal = 1;
		for (AttributeModifier attrib : attribute) {
			if (attrib.getOperation() == Operation.ADDITION)
				base += attrib.getAmount();
			if (attrib.getOperation() == Operation.MULTIPLY_BASE)
				mbase *= attrib.getAmount();
			if (attrib.getOperation() == Operation.MULTIPLY_TOTAL)
				mtotal *= 1+attrib.getAmount();
		}
		return base * mbase * mtotal;
	}
	public static double sumAttributesPercentage(Collection<AttributeModifier> attribute) {
		double base = 0;
		for (AttributeModifier attrib : attribute) {
			if (attrib.getOperation() == Operation.MULTIPLY_TOTAL)
				base += attrib.getAmount();
		}
		return base;
	}
	public void setChanged() {

	}

	public int getSize() {
		return clothes.getSlots();
	}
}
