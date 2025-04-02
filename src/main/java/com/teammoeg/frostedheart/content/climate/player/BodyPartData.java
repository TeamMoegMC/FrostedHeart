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

import java.util.Collection;

import com.teammoeg.frostedheart.bootstrap.common.FHAttributes;
import com.teammoeg.frostedheart.content.climate.data.ArmorTempData;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData.BodyPart;

import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

public class BodyPartData {
    public final ItemStackHandler clothes;
    @Getter
    float temperature = 0;

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

    public void load(CompoundTag itemsTag) {
        clothes.deserializeNBT(itemsTag);
        temperature = itemsTag.getFloat("temp");
    }

    public CompoundTag save() {
        CompoundTag tag = clothes.serializeNBT();
        tag.putFloat("temp", temperature);
        return tag;
    }

    public void reset() {
        for (int i = 0; i < clothes.getSlots(); i++)
            clothes.setStackInSlot(i, ItemStack.EMPTY);
    }

	/**
	 * Get the thermal conductivity based on player clothing,
	 * with proper weighting on which layer the clothes is on,
	 * and accounting for the armor layer if it exists, though
	 * it contributes only a little bit.
	 *
	 * In general, the heat insulation (inversely proportional
	 * to thermal conductivity by 1/(1+x), is more heavily
	 * weighted when the clothes is wore more inside.
	 *
	 * @param player this is important, because we need to know if player is wet etc.
	 * @param part BodyPart
	 * @return thermal conductivity, range (0, 1]
	 */
    public float getThermalConductivity(Player player, BodyPart part) {
		// Equipment on the normal "armor slot" or "hand slot"
		EquipmentSlot slot = part.slot;
		ItemStack equipment = player.getItemBySlot(slot);

		// heat insulation: non-negative
        double insulation = 0f;

		// hands, feet, head: 1 layer
		if (part.canOnlyWearOneLayer()) {
			// equipment on outside, shares contribution with inside
			if (!equipment.isEmpty()) {
				// result from sumAttributes is a non-negative number, can be large like 1000m or small like 0
				insulation += 0.1F * sumAttributes(equipment.getAttributeModifiers(slot).get(FHAttributes.INSULATION.get()));
			}

			// clothes contribution
			ItemStack stack = clothes.getStackInSlot(0);
			if (!stack.isEmpty()) {
				ArmorTempData data = ArmorTempData.getData(stack, part);
				if (data != null) {
					// account for the full ratio when armor is not on
					if (equipment.isEmpty()) {
						insulation += data.getInsulation();
					} else {
						insulation += 0.9F * data.getInsulation();
					}

				}
			}
		}

		// torso, legs: 3 layers
		else {
			// armor on outside
			if (!equipment.isEmpty()) {
				// result from sumAttributes is a non-negative number, can be large like 1000m or small like 0
				insulation += 0.1F * sumAttributes(equipment.getAttributeModifiers(slot).get(FHAttributes.INSULATION.get()));
			}

			// three layers of clothes
			ItemStack outer = clothes.getStackInSlot(0);
			ItemStack middle = clothes.getStackInSlot(1);
			ItemStack inside = clothes.getStackInSlot(2);

			if (!outer.isEmpty()) {
				ArmorTempData data = ArmorTempData.getData(outer, part);
				if (data != null) {
					insulation += 0.1 * data.getInsulation();
				}
			}

			if (!middle.isEmpty()) {
				ArmorTempData data = ArmorTempData.getData(middle, part);
				if (data != null) {
					insulation += 0.3 * data.getInsulation();
				}
			}

			if (!inside.isEmpty()) {
				ArmorTempData data = ArmorTempData.getData(inside, part);
				if (data != null) {
					// account for the full ratio when armor is not on
					if (equipment.isEmpty()) {
						insulation += 0.6 * data.getInsulation();
					} else {
						insulation += 0.5 * data.getInsulation();
					}
				}
			}

		}

        return (float) (100 / (100 + insulation));
    }

	/**
	 * Get the fluid resistance based on player clothing,
	 * with proper weighting on which layer the clothes is on,
	 * and accounting for the armor layer if it exists, though
	 * it contributes only a little bit.
	 *
	 * Fluid resistance is the ability for the clothes
	 * to resist the invasion of fluids to touch the skin.
	 * Both air and water are common fluids to resist,
	 * corresponding to wind-breaking and water-diving suits.
	 *
	 * In general, the fluid resistance is more heavily
	 * weighted when the clothes is wore more outside,
	 * simulating the effects of outdoor jackets.
	 *
	 * @param player this is important, because we need to know if player is wet etc.
	 * @param part BodyPart
	 * @return wind resistance, range [0,1]
	 */
	public float getFluidResistance(Player player, BodyPart part) {
		// Equipment on the normal "armor slot" or "hand slot"
		EquipmentSlot slot = part.slot;
		ItemStack equipment = player.getItemBySlot(slot);

		// fluid resistance: [0,1]
		double fluidResistance = 0f;

		// hands, feet, head: 1 layer
		if (part.canOnlyWearOneLayer()) {
			// equipment on outside, shares contribution with inside
			if (!equipment.isEmpty()) {
				// result from sumAttributes is a non-negative number, can be large like 1000m or small like 0
				fluidResistance += 0.9F * sumAttributes(equipment.getAttributeModifiers(slot).get(FHAttributes.WIND_PROOF.get()));
			}

			// clothes contribution
			ItemStack stack = clothes.getStackInSlot(0);
			if (!stack.isEmpty()) {
				ArmorTempData data = ArmorTempData.getData(stack, part);
				if (data != null) {
					// account for the full ratio when armor is not on
					if (equipment.isEmpty()) {
						fluidResistance += data.getFluidResistance();
					} else {
						fluidResistance += 0.1F * data.getFluidResistance();
					}

				}
			}
		}

		// torso, legs: 3 layers
		else {
			// armor on outside
			if (!equipment.isEmpty()) {
				// result from sumAttributes is a non-negative number, can be large like 1000m or small like 0
				fluidResistance += 0.1F * sumAttributes(equipment.getAttributeModifiers(slot).get(FHAttributes.WIND_PROOF.get()));
			}

			// three layers of clothes
			ItemStack outer = clothes.getStackInSlot(0);
			ItemStack middle = clothes.getStackInSlot(1);
			ItemStack inside = clothes.getStackInSlot(2);

			if (!outer.isEmpty()) {
				ArmorTempData data = ArmorTempData.getData(outer, part);
				if (data != null) {
					// account for the full ratio when armor is not on
					if (equipment.isEmpty()) {
						fluidResistance += 0.6 * data.getFluidResistance();
					} else {
						fluidResistance += 0.5 * data.getFluidResistance();
					}
				}
			}

			if (!middle.isEmpty()) {
				ArmorTempData data = ArmorTempData.getData(middle, part);
				if (data != null) {
					fluidResistance += 0.3 * data.getFluidResistance();
				}
			}

			if (!inside.isEmpty()) {
				ArmorTempData data = ArmorTempData.getData(inside, part);
				if (data != null) {
					fluidResistance += 0.1 * data.getFluidResistance();
				}
			}

		}

		return (float) fluidResistance;
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
                mtotal *= 1 + attrib.getAmount();
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

    @Override
    public String toString() {
        return "BodyPartData [clothes=" + clothes.getStackInSlot(0) + ", temperature=" + temperature + "]";
    }
}
