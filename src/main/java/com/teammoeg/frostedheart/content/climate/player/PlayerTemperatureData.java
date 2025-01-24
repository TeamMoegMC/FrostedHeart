package com.teammoeg.frostedheart.content.climate.player;

import javax.annotation.Nullable;

import com.mojang.datafixers.util.Pair;
import com.teammoeg.chorda.io.NBTSerializable;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.content.climate.FHTemperatureDifficulty;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

// https://ierga.com/hr/wp-content/uploads/sites/2/2017/10/ASHRAE-55-2013.pdf

public class PlayerTemperatureData implements NBTSerializable  {
	public enum BodyPart {
		TORSO, // 40% area
		LEGS, // 40% area
		HANDS, // 5% area
		FEET, // 5% area
		HEAD, // 10% area
		REMOVEALL, // debug only
	}
	public static Map<BodyPart, Float> bodyPartAreaMap = Map.of(
			BodyPart.HEAD, 0.1f,
			BodyPart.TORSO, 0.4f,
			BodyPart.HANDS, 0.05f,
			BodyPart.LEGS, 0.4f,
			BodyPart.FEET, 0.05f
	);

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

	public final Map<BodyPart, BodyPartClothingData> clothesOfParts = new EnumMap<>(BodyPart.class);

	public PlayerTemperatureData() {
		clothesOfParts.put(BodyPart.HEAD, new BodyPartClothingData("head_clothing", 1));
		clothesOfParts.put(BodyPart.HANDS, new BodyPartClothingData("hands_clothing", 1));
		clothesOfParts.put(BodyPart.FEET, new BodyPartClothingData("feet_clothing", 1));
		clothesOfParts.put(BodyPart.TORSO, new BodyPartClothingData("torso_clothing", 3));
		clothesOfParts.put(BodyPart.LEGS, new BodyPartClothingData("legs_clothing", 3));
		temperatureOfParts.put(BodyPart.HEAD, new Pair<>("head_temperature", 0.0f));
		temperatureOfParts.put(BodyPart.HANDS, new Pair<>("hands_temperature", 0.0f));
		temperatureOfParts.put(BodyPart.FEET, new Pair<>("feet_temperature", 0.0f));
		temperatureOfParts.put(BodyPart.TORSO, new Pair<>("torso_temperature", 0.0f));
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
		clearAllClothes();
	}
    public void update(float current_env, float conductivity) {
        // update delta before body
    	previousTemp=bodyTemp;
    	bodyTemp=0;
		for(Map.Entry<BodyPart, Pair<String, Float>> e : temperatureOfParts.entrySet()) {
			bodyTemp += e.getValue().getSecond() * bodyPartAreaMap.get(e.getKey());
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

	public float getThermalConductivityByPart(Player player, BodyPart bodyPart) {
		ItemStack equipment = switch (bodyPart) {
			case TORSO -> player.getInventory().getArmor(2); // Chestplate slot
			case LEGS -> player.getInventory().getArmor(1); // Leggings slot
			case FEET -> player.getInventory().getArmor(0); // Boots slot
			case HEAD -> player.getInventory().getArmor(3); // Helmet slot
			case HANDS -> player.getMainHandItem(); // Main hand
			default -> ItemStack.EMPTY; // Default to empty
		};
		// TODO remove out
//		System.out.printf("Part %s Cond %f\n", bodyPart, clothesOfParts.get(bodyPart).getThermalConductivity(equipment));
		return clothesOfParts.get(bodyPart).getThermalConductivity(equipment);
	}

	public float getTemperatureByPart(BodyPart bodyPart) {
		return temperatureOfParts.get(bodyPart).getSecond();
	}
	public void setTemperatureByPart(BodyPart bodyPart, float t) {
		temperatureOfParts.put(bodyPart, new Pair<>(temperatureOfParts.get(bodyPart).getFirst(), t));
	}
}
