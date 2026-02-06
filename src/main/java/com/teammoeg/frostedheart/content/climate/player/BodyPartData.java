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

import java.util.ArrayList;
import java.util.List;

import com.teammoeg.frostedheart.content.climate.data.ArmorTempData;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData.BodyPart;

import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

public class BodyPartData {
	public static final float[] SINGLE_SLOT_FACTORS=new float[] {.4f,.6f};
	public static final float[] TRIPLE_SLOT_FACTORS=new float[] {.1f,.2f,.3f,.4f};
    public final ItemStackHandler clothes;
    @Getter
    float temperature = 0;
	@Getter
	float feelTemp = 0;

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
		feelTemp = itemsTag.getFloat("feel_temp");
    }

    public CompoundTag save() {
        CompoundTag tag = clothes.serializeNBT();
        tag.putFloat("temp", temperature);
		tag.putFloat("feel_temp", feelTemp);
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
		// heat insulation: non-negative
        double insulation = 0f;
        double fluidResist = 0f;
        List<ClothData> slots=this.getClothDataBySlot(player, part);
		// hands, feet, head: 1 layer of clothes and 1 equipment
		if (part.slotNum==1) {
			int delta=2-slots.size();
			for(int i=0;i<slots.size();i++) {
				insulation+=SINGLE_SLOT_FACTORS[i+delta]*slots.get(i).insulation;
				fluidResist+=SINGLE_SLOT_FACTORS[SINGLE_SLOT_FACTORS.length-i-1]*slots.get(i).fluidResist;
			}
		}
		// torso, legs: 3 layers of clothes, 1 armor
		else {
			int delta=4-slots.size();
			for(int i=0;i<slots.size();i++) {
				insulation+=TRIPLE_SLOT_FACTORS[i+delta]*slots.get(i).insulation;
				fluidResist+=TRIPLE_SLOT_FACTORS[TRIPLE_SLOT_FACTORS.length-i-1]*slots.get(i).fluidResist;
			}
		}

        return new PartClothData((float)(100 / (100 + insulation)),(float)fluidResist);
    }
    /**
     * Get cloth data without handling the stack
     * 
     * */
    public List<ClothData> getClothDataBySlot(Player player,BodyPart part){
    	List<ClothData> snst=new ArrayList<>();
    	ItemStack equipment=player.getItemBySlot(part.slot);
    	if(!equipment.isEmpty())
    		snst.add(new ClothData(equipment.getAttributeModifiers(part.slot)));
    	for(int i=0;i<clothes.getSlots();i++) {
    		ItemStack item=clothes.getStackInSlot(i);
    		if(!item.isEmpty()) {
	    		ArmorTempData atd=ArmorTempData.getData(item, part);
	    		if(atd!=null)
	    			snst.add(new ClothData(atd));
    		}
    	}
    	return snst;
    }



    public void setChanged() {

    }

    public int getSize() {
        return clothes.getSlots();
    }

    @Override
    public String toString() {
        return "BodyPartData [clothes=" + clothes.getStackInSlot(0) + ", temperature=" + temperature + ", feelTemp=" + feelTemp + "]";
    }
}
