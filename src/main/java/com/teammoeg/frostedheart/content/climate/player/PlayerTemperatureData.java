package com.teammoeg.frostedheart.content.climate.player;

import javax.annotation.Nullable;

import com.mojang.datafixers.util.Pair;
import com.teammoeg.frostedheart.FHCapabilities;
import com.teammoeg.frostedheart.base.item.FHBaseClothesItem;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.util.constants.FHTemperatureDifficulty;
import com.teammoeg.frostedheart.util.io.NBTSerializable;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

// https://ierga.com/hr/wp-content/uploads/sites/2/2017/10/ASHRAE-55-2013.pdf



class BodyPartClothingData {
	String name;
	final ItemStack[] clothes;
	BodyPartClothingData(String name, int max_count) {
		this.name = name;
		this.clothes = new ItemStack[max_count];
		reset();
	}

	void set(ListTag itemsTag) {
		for (int i = 0; i < itemsTag.size() && i < clothes.length; i++) {
			clothes[i] = ItemStack.of((CompoundTag) itemsTag.get(i));
		}
	}

	void reset() {
		Arrays.fill(clothes, ItemStack.EMPTY);
	}

	float getThermalConductivity() {
		float res=0f;
		float rate=0.4f;
		for(ItemStack it : this.clothes) {
			if(!it.isEmpty()) {
				res += rate * ((FHBaseClothesItem) it.getItem()).getWarmthLevel();
				rate -= 0.1f;
			}
		}
		return 100/(100+res);
	}

	float getWindResistance() {
		float res=0f;
		float rate=0.4f-this.clothes.length*0.1f;
		for(ItemStack it : this.clothes) {
			if(!it.isEmpty()) {
				rate += 0.1f;
				res += rate * ((FHBaseClothesItem) it.getItem()).getWindResistance();
			}
		}
		return res;
	}
}

public class PlayerTemperatureData implements NBTSerializable  {
	public enum BodyPart {
		BODY, // 40% area
		LEGS, // 40% area
		HANDS, // 5% area
		FEET, // 5% area
		HEAD, // 10% area
		REMOVEALL, // debug only
	}

	@Getter
	@Setter
	private FHTemperatureDifficulty difficulty = FHConfig.SERVER.tdiffculty.get();
	float previousTemp;
	float bodyTemp;
	float envTemp;
	float feelTemp;
	public float smoothedBody;//Client only, smoothed body temperature
	public float smoothedBodyPrev;//Client only, smoothed body temperature

	private final Map<BodyPart, Pair<String, Float>> temperatureOfParts = new EnumMap<>(BodyPart.class);
	private final Map<BodyPart, String> namesOfParts = new EnumMap<>(BodyPart.class);

	private final Map<BodyPart, BodyPartClothingData> clothesOfParts = new EnumMap<>(BodyPart.class);

	public PlayerTemperatureData() {
		clothesOfParts.put(BodyPart.HEAD, new BodyPartClothingData("head_clothing", 1));
		clothesOfParts.put(BodyPart.HANDS, new BodyPartClothingData("hands_clothing", 1));
		clothesOfParts.put(BodyPart.FEET, new BodyPartClothingData("feet_clothing", 1));
		clothesOfParts.put(BodyPart.BODY, new BodyPartClothingData("body_clothing", 4));
		clothesOfParts.put(BodyPart.LEGS, new BodyPartClothingData("legs_clothing", 4));
		temperatureOfParts.put(BodyPart.HEAD, new Pair<>("head_temperature", 0.0f));
		temperatureOfParts.put(BodyPart.HANDS, new Pair<>("hands_temperature", 0.0f));
		temperatureOfParts.put(BodyPart.FEET, new Pair<>("feet_temperature", 0.0f));
		temperatureOfParts.put(BodyPart.BODY, new Pair<>("body_temperature", 0.0f));
		temperatureOfParts.put(BodyPart.LEGS, new Pair<>("legs_temperature", 0.0f));
	}
	public void load(CompoundTag nbt,boolean isPacket) {
		// load the difficulty
		// this can cause issue if the nbt.getstring returns invalid string
		// do a catch here, and default to normal
		try {
			difficulty = FHTemperatureDifficulty.valueOf(nbt.getString("difficulty").toLowerCase());
		} catch (IllegalArgumentException e) {
			difficulty = FHTemperatureDifficulty.normal;
		}

		previousTemp=nbt.getFloat("previous_body_temperature");
		bodyTemp=nbt.getFloat("bodytemperature");
		envTemp=nbt.getFloat("envtemperature");
		feelTemp=nbt.getFloat("feeltemperature");

		for(Map.Entry<BodyPart, BodyPartClothingData> e : clothesOfParts.entrySet()) {
			BodyPartClothingData subData = e.getValue();
			ListTag itemsTag = nbt.getList(subData.name, Tag.TAG_COMPOUND);
			e.getValue().set(itemsTag);
		}

		for (Map.Entry<BodyPart, Pair<String, Float>> e : temperatureOfParts.entrySet()) {
			Pair<String, Float> p = e.getValue();
			Pair<String, Float> updatedPair = new Pair<>(p.getFirst(), nbt.getFloat(p.getFirst()));
			e.setValue(updatedPair); // Update the map with the new Pair
		}
	}
	public void save(CompoundTag nc,boolean isPacket) {
		// save the difficulty
		nc.putString("difficulty", difficulty.name().toLowerCase());
        nc.putFloat("previous_body_temperature",previousTemp);
        nc.putFloat("bodytemperature",bodyTemp);
        nc.putFloat("envtemperature",envTemp);
        nc.putFloat("feeltemperature",feelTemp);

		for(Map.Entry<BodyPart, BodyPartClothingData> e : clothesOfParts.entrySet()) {
			BodyPartClothingData subData = e.getValue();
			ListTag itemsTag = new ListTag();
			for(ItemStack stack : subData.clothes) {
				CompoundTag itemTag = new CompoundTag();
				if (stack != null) {
					stack.save(itemTag);
				}
				itemsTag.add(itemTag);
			}
			nc.put(subData.name, (Tag) itemsTag);
		}

		for(Map.Entry<BodyPart, Pair<String, Float>> e : temperatureOfParts.entrySet()) {
			Pair<String, Float> p = e.getValue();
			nc.putFloat(p.getFirst(), p.getSecond());
		}
	}
	public void reset() {
		previousTemp=0;
		bodyTemp=0;
		envTemp=0;
		feelTemp=0;
		smoothedBody=0;
		for(Map.Entry<BodyPart, BodyPartClothingData> e : clothesOfParts.entrySet()) {
			e.getValue().reset();
		}
	}
    public void update(float body, float env,float feel) {
        // update delta before body
    	previousTemp=bodyTemp;
    	bodyTemp=body;
    	envTemp=env;
    	feelTemp=feel;
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
	public ItemStack[] getClothesByPart(BodyPart bodyPart) {
		return clothesOfParts.get(bodyPart).clothes; // Return a copy to prevent direct modification
	}

	public void setClothes(BodyPart bodyPart, int index, ItemStack stack) {
		if (index < 0 || index >= clothesOfParts.get(bodyPart).clothes.length) {
			throw new IndexOutOfBoundsException("Invalid index for bodyClothes: " + index);
		}
		clothesOfParts.get(bodyPart).clothes[index] = stack != null ? stack : ItemStack.EMPTY;
	}

	public void clearClothes(BodyPart bodyPart, int index) {
		setClothes(bodyPart, index, ItemStack.EMPTY);
	}

	public void clearAllClothes() {
		for(Map.Entry<BodyPart, BodyPartClothingData> e : clothesOfParts.entrySet()) {
			e.getValue().reset();
		}
	}

	public float getThermalConductivityByPart(BodyPart bodyPart) {
		return clothesOfParts.get(bodyPart).getThermalConductivity();
	}

	public float getTemperatureByPart(BodyPart bodyPart) {
		return temperatureOfParts.get(bodyPart).getSecond();
	}
	public void setTemperatureByPart(BodyPart bodyPart, float t) {
		temperatureOfParts.put(bodyPart, new Pair<>(temperatureOfParts.get(bodyPart).getFirst(), t));
	}
}
