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

package com.teammoeg.frostedheart.content.climate.player;

import com.teammoeg.frostedheart.bootstrap.common.FHAttributes;
import com.teammoeg.frostedheart.content.climate.data.ArmorTempData;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData.BodyPart;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

public class BodyPartData {
    private static final float[] SINGLE_SLOT_FACTORS = {0.4F, 0.6F};
    private static final float[] TRIPLE_SLOT_FACTORS = {
            0.1F, 0.2F, 0.3F, 0.4F
    };
    public final ItemStackHandler clothes;
	double bodyEnergyOffsetJ;
	float feelingTemperatureC = 37.0F;

    BodyPartData(int max_count) {
        this.clothes = new ItemStackHandler(max_count) {

            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                setChanged();
            }

        };
        //reset();
    }

    public void load(CompoundTag itemsTag, boolean energySchema) {
        if (itemsTag.contains("Size")) {
            clothes.deserializeNBT(itemsTag);
        }
		if (energySchema && itemsTag.contains("energy_j")) {
			bodyEnergyOffsetJ = itemsTag.getDouble("energy_j");
		} else {
			bodyEnergyOffsetJ = 0.0D;
		}
    }

    public CompoundTag save() {
        CompoundTag tag = clothes.serializeNBT();
        tag.putDouble("energy_j", bodyEnergyOffsetJ);
        return tag;
    }

    public void reset() {
        for (int i = 0; i < clothes.getSlots(); i++)
            clothes.setStackInSlot(i, ItemStack.EMPTY);
    }

	/**
	 * Get the thermal conductivity and fluid resistance based on player clothing,
	 * with proper weighting on which layer the clothes is on,
	 * and accounting for the armor layer if it exists, though
	 * it contributes only a little bit.
	 *
	 * In general, the heat insulation (inversely proportional
	 * to thermal conductivity by 1/(1+x), is more heavily
	 * weighted when the clothes is wore more inside.
	 *
	 * Fluid resistance is the ability for the clothes
	 * to resist the invasion of fluids to touch the skin.
	 * Both air and water are common fluids to resist,
	 * corresponding to wind-breaking and water-diving suits.
	 *
	 * In general, the fluid resistance is more heavily
	 * weighted when the clothes is wore more outside,
	 * simulating the effects of outdoor jackets.
	 * @param player this is important, because we need to know if player is wet etc.
	 * @param part BodyPart
	 * @return thermal conductivity, range (0, 1]
	 */
    public PartClothData getClothData(Player player, BodyPart part) {
		PartClothData result = new PartClothData();
		fillClothData(player, part, result);
		return result;
    }

	void fillClothData(Player player, BodyPart part, PartClothData result) {
		ItemStack equipment = player.getItemBySlot(part.slot);
		int layerCount = equipment.isEmpty() ? 0 : 1;
		for (int slot = 0; slot < clothes.getSlots(); slot++) {
			ItemStack stack = clothes.getStackInSlot(slot);
			if (!stack.isEmpty() && ArmorTempData.getData(stack, part) != null) {
				layerCount++;
			}
		}

		double resistance = 0.0D;
		double heatProof = 0.0D;
		double windProof = 0.0D;
		double waterResistance = 0.0D;
		int layer = 0;
		if (!equipment.isEmpty()) {
			double insulationFactor = ClothData.sumAttributes(
					equipment.getAttributeModifiers(part.slot)
							.get(FHAttributes.INSULATION.get()));
			double wind = ClothData.sumAttributesPercentage(
					equipment.getAttributeModifiers(part.slot)
							.get(FHAttributes.WIND_PROOF.get()));
			double radiant = ClothData.sumAttributesPercentage(
					equipment.getAttributeModifiers(part.slot)
							.get(FHAttributes.HEAT_PROOF.get()));
			double innerWeight = innerWeight(part, layerCount, layer);
			double outerWeight = outerWeight(part, layer);
			resistance += innerWeight * insulationFactor
					* PlayerTemperatureComputation.LEGACY_INSULATION_TO_RESISTANCE;
			heatProof += outerWeight * radiant;
			windProof += outerWeight * wind;
			waterResistance += outerWeight * wind;
			layer++;
		}
		for (int slot = 0; slot < clothes.getSlots(); slot++) {
			ItemStack stack = clothes.getStackInSlot(slot);
			ArmorTempData armor = stack.isEmpty()
					? null : ArmorTempData.getData(stack, part);
			if (armor == null) continue;
			double innerWeight = innerWeight(part, layerCount, layer);
			double outerWeight = outerWeight(part, layer);
			resistance += innerWeight * armor.getInsulation()
					* PlayerTemperatureComputation.LEGACY_INSULATION_TO_RESISTANCE;
			heatProof += outerWeight * armor.getHeatProof();
			windProof += outerWeight * armor.getFluidResistance();
			waterResistance += outerWeight * armor.getFluidResistance();
			layer++;
		}
		result.set(resistance, heatProof, windProof, waterResistance);
	}

	private static double innerWeight(
			BodyPart part, int layerCount, int layer
	) {
		float[] weights = part.slotNum == 1
				? SINGLE_SLOT_FACTORS : TRIPLE_SLOT_FACTORS;
		return weights[weights.length - layerCount + layer];
	}

	private static double outerWeight(BodyPart part, int layer) {
		float[] weights = part.slotNum == 1
				? SINGLE_SLOT_FACTORS : TRIPLE_SLOT_FACTORS;
		return weights[weights.length - layer - 1];
	}



    public void setChanged() {

    }

    public int getSize() {
        return clothes.getSlots();
    }

    @Override
    public String toString() {
        return "BodyPartData [clothes=" + clothes.getStackInSlot(0)
				+ ", bodyEnergyOffsetJ=" + bodyEnergyOffsetJ + "]";
    }
}
