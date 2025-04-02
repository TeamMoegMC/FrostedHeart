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

		// hands, feet, head: 1 layer of clothes and 1 equipment
		if (part.canOnlyWearOneLayer()) {
			ItemStack stack = clothes.getStackInSlot(0);
			boolean hasEquipment = !equipment.isEmpty();
			boolean hasClothes = !stack.isEmpty();

			// case 1
			if (hasEquipment && hasClothes) {
				insulation += 0.5F * sumAttributes(equipment.getAttributeModifiers(slot).get(FHAttributes.INSULATION.get()));
				ArmorTempData data = ArmorTempData.getData(stack, part);
				if (data != null) {
					insulation += 0.5F * data.getInsulation();
				}
			}

			// case 2
			if (hasEquipment && !hasClothes) {
				insulation += sumAttributes(equipment.getAttributeModifiers(slot).get(FHAttributes.INSULATION.get()));
			}

			// case 3
			if (!hasEquipment && hasClothes) {
				ArmorTempData data = ArmorTempData.getData(stack, part);
				if (data != null) {
					insulation += data.getInsulation();
				}
			}
		}

		// torso, legs: 3 layers of clothes, 1 armor
		else {
			boolean hasEquipment = !equipment.isEmpty();
			// three layers of clothes
			ItemStack outer = clothes.getStackInSlot(0);
			ItemStack middle = clothes.getStackInSlot(1);
			ItemStack inside = clothes.getStackInSlot(2);
			boolean hasOuter = !outer.isEmpty();
			boolean hasMiddle = !middle.isEmpty();
			boolean hasInner = !inside.isEmpty();

			// Count how many layers are present
			int layerCount = (hasEquipment ? 1 : 0) + (hasOuter ? 1 : 0) +
					(hasMiddle ? 1 : 0) + (hasInner ? 1 : 0);

			// Define base weights - these will be adjusted based on present layers
			float equipmentWeight = 0.1f;
			float outerWeight = 0.2f;
			float middleWeight = 0.3f;
			float innerWeight = 0.4f;

			// Adjust weights to ensure sum is 1.0
			if (layerCount > 0) {
				float totalBaseWeight = 0f;
				if (hasEquipment) totalBaseWeight += equipmentWeight;
				if (hasOuter) totalBaseWeight += outerWeight;
				if (hasMiddle) totalBaseWeight += middleWeight;
				if (hasInner) totalBaseWeight += innerWeight;

				// Scale all weights proportionally
				float scaleFactor = 1.0f / totalBaseWeight;

				if (hasEquipment) {
					float scaledWeight = equipmentWeight * scaleFactor;
					insulation += scaledWeight * sumAttributes(equipment.getAttributeModifiers(slot).get(FHAttributes.INSULATION.get()));
				}

				if (hasOuter) {
					ArmorTempData data = ArmorTempData.getData(outer, part);
					if (data != null) {
						float scaledWeight = outerWeight * scaleFactor;
						insulation += scaledWeight * data.getInsulation();
					}
				}

				if (hasMiddle) {
					ArmorTempData data = ArmorTempData.getData(middle, part);
					if (data != null) {
						float scaledWeight = middleWeight * scaleFactor;
						insulation += scaledWeight * data.getInsulation();
					}
				}

				if (hasInner) {
					ArmorTempData data = ArmorTempData.getData(inside, part);
					if (data != null) {
						float scaledWeight = innerWeight * scaleFactor;
						insulation += scaledWeight * data.getInsulation();
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
			ItemStack stack = clothes.getStackInSlot(0);
			boolean hasEquipment = !equipment.isEmpty();
			boolean hasClothes = !stack.isEmpty();

			// case 1
			if (hasEquipment && hasClothes) {
				fluidResistance += 0.5F * sumAttributes(equipment.getAttributeModifiers(slot).get(FHAttributes.WIND_PROOF.get()));
				 ArmorTempData data = ArmorTempData.getData(stack, part);
				 if (data != null) {
					fluidResistance += 0.5F * data.getFluidResistance();
				 }
			}

			// case 2
			if (hasEquipment && !hasClothes) {
				fluidResistance += sumAttributes(equipment.getAttributeModifiers(slot).get(FHAttributes.WIND_PROOF.get()));
			}

			// case 3
			if (!hasEquipment && hasClothes) {
				ArmorTempData data = ArmorTempData.getData(stack, part);
				if (data != null) {
					fluidResistance += data.getFluidResistance();
				}
			}
		}

		// torso, legs: 3 layers
		else {
			boolean hasEquipment = !equipment.isEmpty();
			// three layers of clothes
			ItemStack outer = clothes.getStackInSlot(0);
			ItemStack middle = clothes.getStackInSlot(1);
			ItemStack inside = clothes.getStackInSlot(2);
			boolean hasOuter = !outer.isEmpty();
			boolean hasMiddle = !middle.isEmpty();
			boolean hasInner = !inside.isEmpty();

			// Count how many layers are present
			int layerCount = (hasEquipment ? 1 : 0) + (hasOuter ? 1 : 0) +
					(hasMiddle ? 1 : 0) + (hasInner ? 1 : 0);

			// Define base weights - these are reversed compared to insulation
			// since fluid resistance is more important in outer layers
			float equipmentWeight = 0.4f;  // Most outer layer has highest weight
			float outerWeight = 0.3f;
			float middleWeight = 0.2f;
			float innerWeight = 0.1f;      // Inner layer has lowest weight

			// Adjust weights to ensure sum is 1.0
			if (layerCount > 0) {
				float totalBaseWeight = 0f;
				if (hasEquipment) totalBaseWeight += equipmentWeight;
				if (hasOuter) totalBaseWeight += outerWeight;
				if (hasMiddle) totalBaseWeight += middleWeight;
				if (hasInner) totalBaseWeight += innerWeight;

				// Scale all weights proportionally
				float scaleFactor = 1.0f / totalBaseWeight;

				if (hasEquipment) {
					float scaledWeight = equipmentWeight * scaleFactor;
					fluidResistance += scaledWeight * sumAttributes(equipment.getAttributeModifiers(slot).get(FHAttributes.WIND_PROOF.get()));
				}

				if (hasOuter) {
					ArmorTempData data = ArmorTempData.getData(outer, part);
					if (data != null) {
						float scaledWeight = outerWeight * scaleFactor;
						fluidResistance += scaledWeight * data.getFluidResistance();
					}
				}

				if (hasMiddle) {
					ArmorTempData data = ArmorTempData.getData(middle, part);
					if (data != null) {
						float scaledWeight = middleWeight * scaleFactor;
						fluidResistance += scaledWeight * data.getFluidResistance();
					}
				}

				if (hasInner) {
					ArmorTempData data = ArmorTempData.getData(inside, part);
					if (data != null) {
						float scaledWeight = innerWeight * scaleFactor;
						fluidResistance += scaledWeight * data.getFluidResistance();
					}
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
