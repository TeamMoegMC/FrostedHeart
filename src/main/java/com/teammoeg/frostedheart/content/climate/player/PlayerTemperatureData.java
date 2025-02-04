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

import javax.annotation.Nullable;

import com.teammoeg.chorda.io.NBTSerializable;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.util.client.Lang;
import com.teammoeg.frostedheart.content.climate.FHTemperatureDifficulty;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

// https://ierga.com/hr/wp-content/uploads/sites/2/2017/10/ASHRAE-55-2013.pdf

public class PlayerTemperatureData implements NBTSerializable  {
	public enum BodyPart implements StringRepresentable{
		HEAD(EquipmentSlot.HEAD, 0.1f), // 10% area
		TORSO(EquipmentSlot.CHEST, 0.4f), // 40% area
		
		HANDS(EquipmentSlot.MAINHAND, 0.05f), // 5% area
		LEGS(EquipmentSlot.LEGS, 0.4f), // 40% area
		FEET(EquipmentSlot.FEET, 0.05f); // 5% area
		public final EquipmentSlot slot;
		public final float area;
		private final static Map<EquipmentSlot,BodyPart> VANILLA_MAP=Util.make(new EnumMap<>(EquipmentSlot.class),t->{
			for(BodyPart part:BodyPart.values())
				if(part.slot!=null)
					t.put(part.slot, part);
		});
		private BodyPart(EquipmentSlot slot,float area) {
			this.slot = slot;
			this.area=area;
		}

		@Override
		public String getSerializedName() {
			return this.name().toLowerCase();
		}
		public static BodyPart fromVanilla(EquipmentSlot es) {
			if(es==null)return null;
			return VANILLA_MAP.get(es);
		}
		public Component getName() {
			return Lang.translateGui("body_part."+getSerializedName());
		}
	}
	@Setter
	private FHTemperatureDifficulty difficulty = null;//in case null, get it from  FHConfig.SERVER.tdiffculty.get()
	float previousTemp;
	float bodyTemp;
	float envTemp;
	float feelTemp;
	public float smoothedBody;//Client only, smoothed body temperature
	public float smoothedBodyPrev;//Client only, smoothed body temperature


	public final Map<BodyPart, BodyPartData> clothesOfParts = new EnumMap<>(BodyPart.class);
	public void deathResetTemperature() {
		previousTemp=0;
		bodyTemp=0;
		envTemp=0;
		feelTemp=0;
		for(BodyPartData i:clothesOfParts.values()) {
			i.temperature=0;
		}
	}
	public PlayerTemperatureData() {
		clothesOfParts.put(BodyPart.HEAD, new BodyPartData(1));
		clothesOfParts.put(BodyPart.HANDS, new BodyPartData(1));
		clothesOfParts.put(BodyPart.FEET, new BodyPartData(1));
		clothesOfParts.put(BodyPart.TORSO, new BodyPartData(3));
		clothesOfParts.put(BodyPart.LEGS, new BodyPartData(3));
	}
	public FHTemperatureDifficulty getDifficulty() {
		if(difficulty==null)
			return FHConfig.SERVER.tdiffculty.get();
		return difficulty;
	}
	public void load(CompoundTag nbt,boolean isPacket) {
		// load the difficulty
		// this can cause issue if the nbt.getstring returns invalid string
		// do a catch here, and default to normal
		if(nbt.contains("difficulty"))
			try {
				difficulty = FHTemperatureDifficulty.valueOf(nbt.getString("difficulty").toLowerCase());
			} catch (IllegalArgumentException e) {
				difficulty = FHTemperatureDifficulty.normal;
			}

		previousTemp=nbt.getFloat("previous_body_temperature");
		bodyTemp=nbt.getFloat("bodytemperature");
		envTemp=nbt.getFloat("envtemperature");
		feelTemp=nbt.getFloat("feeltemperature");
		CompoundTag partClothes=nbt.getCompound("body_parts");
		for(Map.Entry<BodyPart, BodyPartData> e : clothesOfParts.entrySet()) {
			e.getValue().load(partClothes.getCompound(e.getKey().getSerializedName()));;
		}

	}
	public void save(CompoundTag nc,boolean isPacket) {
		// save the difficulty
		if(difficulty!=null)
			nc.putString("difficulty", difficulty.name().toLowerCase());
        nc.putFloat("previous_body_temperature",previousTemp);
        nc.putFloat("bodytemperature",bodyTemp);
        nc.putFloat("envtemperature",envTemp);
        nc.putFloat("feeltemperature",feelTemp);
        CompoundTag partClothes=new CompoundTag();
		for(Entry<BodyPart, BodyPartData> bp:clothesOfParts.entrySet()) {
			partClothes.put(bp.getKey().getSerializedName(),bp.getValue().save());
		}
		nc.put("body_parts", partClothes);
	}
	public void reset() {
		previousTemp=0;
		bodyTemp=0;
		envTemp=0;
		feelTemp=0;
		smoothedBody=0;
		clearAllClothes();
	}
    public void update(float current_env, float conductivity) {
        // update delta before body
    	previousTemp=bodyTemp;
    	bodyTemp=0;
		for(Entry<BodyPart, BodyPartData> e : clothesOfParts.entrySet()) {
			bodyTemp += e.getValue().temperature * e.getKey().area;
		}
    	envTemp=(current_env + 37F) * .2f + envTemp * .8f;
		float current_feel = bodyTemp - conductivity * (bodyTemp - current_env);
    	feelTemp=(current_feel + 37F) * .2f + feelTemp * .8f;
    }
    public static LazyOptional<PlayerTemperatureData> getCapability(@Nullable Player player) {
        return FHCapabilities.PLAYER_TEMP.getCapability(player);
    }
	public float getPreviousTemp() {
		return previousTemp;
	}
	public float getBodyTemp() {
		return bodyTemp;
	}
	public float getEnvTemp() {
		return envTemp;
	}
	public float getFeelTemp() {
		return feelTemp;
	}
	public void setPreviousTemp(float previousTemp) {
		this.previousTemp = previousTemp;
	}
	public void setBodyTemp(float bodyTemp) {
		this.bodyTemp = bodyTemp;
	}
	public void setEnvTemp(float envTemp) {
		this.envTemp = envTemp;
	}
	public void setFeelTemp(float feelTemp) {
		this.feelTemp = feelTemp;
	}

	// Body clothes methods
	public ItemStackHandler getClothesByPart(BodyPart bodyPart) {
		return clothesOfParts.get(bodyPart).clothes; // Return a copy to prevent direct modification
	}

	public void setClothes(BodyPart bodyPart, int index, ItemStack stack) {
		clothesOfParts.get(bodyPart).clothes.setStackInSlot(index, stack);
	}

	public void clearClothes(BodyPart bodyPart, int index) {
		setClothes(bodyPart, index, ItemStack.EMPTY);
	}

	public void clearAllClothes() {
		for(Map.Entry<BodyPart, BodyPartData> e : clothesOfParts.entrySet()) {
			e.getValue().reset();
		}
	}

	public float getThermalConductivityByPart(Player player, BodyPart bodyPart) {
		ItemStack equipment;
		if(bodyPart.slot.isArmor())
			equipment=player.getInventory().getArmor(bodyPart.slot.getIndex());
		else
			equipment=player.getMainHandItem();
		// TODO remove out
//		System.out.printf("Part %s Cond %f\n", bodyPart, clothesOfParts.get(bodyPart).getThermalConductivity(equipment));
		return clothesOfParts.get(bodyPart).getThermalConductivity(bodyPart,equipment);
	}
	public float getWindResistanceByPart(Player player, BodyPart bodyPart) {
		ItemStack equipment;
		if(bodyPart.slot.isArmor())
			equipment=player.getInventory().getArmor(bodyPart.slot.getIndex());
		else
			equipment=player.getMainHandItem();
		// TODO remove out
//		System.out.printf("Part %s Cond %f\n", bodyPart, clothesOfParts.get(bodyPart).getThermalConductivity(equipment));
		return clothesOfParts.get(bodyPart).getWindResistance(bodyPart,equipment);
	}

	public float getTemperatureByPart(BodyPart bodyPart) {
		return clothesOfParts.get(bodyPart).temperature;
	}
	public void setTemperatureByPart(BodyPart bodyPart, float t) {
		clothesOfParts.get(bodyPart).temperature=t;
	}
}
