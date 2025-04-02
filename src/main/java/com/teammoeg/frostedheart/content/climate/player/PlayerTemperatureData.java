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

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import com.teammoeg.chorda.io.NBTSerializable;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.climate.FHTemperatureDifficulty;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.util.Lang;

import lombok.Setter;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;

// https://ierga.com/hr/wp-content/uploads/sites/2/2017/10/ASHRAE-55-2013.pdf

public class PlayerTemperatureData implements NBTSerializable {
    public enum BodyPart implements StringRepresentable {
        HEAD(EquipmentSlot.HEAD, 0.1f, 0.1f, 1), // 10% area
        TORSO(EquipmentSlot.CHEST, 0.45f, 0.5f, 3), // 40% area

        HANDS(EquipmentSlot.MAINHAND, 0.05f, 0.05f, 1), // 5% area
        LEGS(EquipmentSlot.LEGS, 0.35f, 0.4f, 3), // 40% area
        FEET(EquipmentSlot.FEET, 0.05f, 0.05f, 1); // 5% area
        public static final BodyPart[] CoreParts = new BodyPart[]{HEAD, TORSO, LEGS};
        public final EquipmentSlot slot;
        public final float area;
        public final float affectsCore;
        public final int slotNum;
        private final static Map<EquipmentSlot, BodyPart> VANILLA_MAP = Util.make(new EnumMap<>(EquipmentSlot.class), t -> {
            for (BodyPart part : BodyPart.values())
                if (part.slot != null)
                    t.put(part.slot, part);
        });

        private BodyPart(EquipmentSlot slot, float area, float affectsCore, int slotNum) {
            this.slot = slot;
            this.area = area;
            this.affectsCore = affectsCore;
            this.slotNum = slotNum;
        }

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase();
        }

        public static BodyPart fromVanilla(EquipmentSlot es) {
            if (es == null) return null;
            return VANILLA_MAP.get(es);
        }

        public Component getName() {
            return Lang.translateGui("body_part." + getSerializedName());
        }

        public boolean canGenerateHeat() {
            switch (this) {
                case TORSO:
                    return true;
            }
            return false;

        }

        public boolean isBodyEnd() {
            switch (this) {
                case FEET:
                case HANDS:
                    return true;
            }
            return false;
        }

        public boolean canOnlyWearOneLayer() {
            switch (this) {
                case HEAD:
                case FEET:
                case HANDS:
                    return true;
            }
            return false;
        }
    }

    public static final int INVALID_TEMPERATURE = 99999;
    @Setter
    private FHTemperatureDifficulty difficulty = null;//in case null, get it from  FHConfig.SERVER.tdiffculty.get()
    @Setter
    float previousTemp;
    float bodyTemp;
    @Setter
    float envTemp = INVALID_TEMPERATURE;
    @Setter
    float feelTemp = INVALID_TEMPERATURE;
    float blockTemp = 0;

    float updateInterval = 0;
    public float smoothedBody;//Client only, smoothed body temperature
    public float smoothedBodyPrev;//Client only, smoothed body temperature


    public final Map<BodyPart, BodyPartData> clothesOfParts = new EnumMap<>(BodyPart.class);
    public float windStrengh;

    public void deathResetTemperature() {
        previousTemp = 0;
        bodyTemp = 0;
        envTemp = INVALID_TEMPERATURE;
        feelTemp = INVALID_TEMPERATURE;
        blockTemp = 0;
        windStrengh = 0;
        updateInterval = 0;

        for (BodyPartData i : clothesOfParts.values()) {
            i.temperature = 0;
        }
    }

    public PlayerTemperatureData() {
        for (BodyPart bp : BodyPart.values())
            clothesOfParts.put(bp, new BodyPartData(bp.slotNum));
    }

    public FHTemperatureDifficulty getDifficulty() {
        if (difficulty == null)
            return FHConfig.SERVER.tdiffculty.get();
        return difficulty;
    }

    public void load(CompoundTag nbt, boolean isPacket) {

        previousTemp = nbt.getFloat("previous_body_temperature");
        bodyTemp = nbt.getFloat("bodytemperature");
        envTemp = nbt.getFloat("envtemperature");
        feelTemp = nbt.getFloat("feeltemperature");

        if (!isPacket) {
            blockTemp = nbt.getFloat("blockTemperature");
            windStrengh = nbt.getFloat("wind_strengh");
            // load the difficulty
            // this can cause issue if the nbt.getstring returns invalid string
            // do a catch here, and default to normal
            if (nbt.contains("difficulty"))
                try {
                    difficulty = FHTemperatureDifficulty.valueOf(nbt.getString("difficulty").toLowerCase());
                } catch (IllegalArgumentException e) {
                    difficulty = FHTemperatureDifficulty.normal;
                }

            CompoundTag partClothes = nbt.getCompound("body_parts");
            for (Map.Entry<BodyPart, BodyPartData> e : clothesOfParts.entrySet()) {
                e.getValue().load(partClothes.getCompound(e.getKey().getSerializedName()));
            }
        }

    }

    public void save(CompoundTag nc, boolean isPacket) {
        // save the difficulty

        nc.putFloat("previous_body_temperature", previousTemp);
        nc.putFloat("bodytemperature", bodyTemp);
        nc.putFloat("envtemperature", envTemp);
        nc.putFloat("feeltemperature", feelTemp);
        if (!isPacket) {
            nc.putFloat("blockTemperature", blockTemp);
            nc.putFloat("wind_strengh", windStrengh);
            if (difficulty != null)
                nc.putString("difficulty", difficulty.name().toLowerCase());
            CompoundTag partClothes = new CompoundTag();
            for (Entry<BodyPart, BodyPartData> bp : clothesOfParts.entrySet()) {
                partClothes.put(bp.getKey().getSerializedName(), bp.getValue().save());
            }
            nc.put("body_parts", partClothes);
        }
    }

    public void reset() {
        previousTemp = 0;
        bodyTemp = 0;
        envTemp = INVALID_TEMPERATURE;
        feelTemp = INVALID_TEMPERATURE;
        smoothedBody = 0;
        windStrengh = 0;
        blockTemp = 0;
        clearAllClothes();
    }

    public void tick() {
        if (updateInterval > 0)
            updateInterval--;
    }

    public void updateWhenInsulated(float current_env, float conductivity) {
        // Do not change body temp, update env and feel only
        if (envTemp == INVALID_TEMPERATURE)
            envTemp = current_env;
        else
            envTemp = (current_env + 37F) * .2f + envTemp * .8f;
        float current_feel = bodyTemp + conductivity * (current_env - bodyTemp);
        if (feelTemp == INVALID_TEMPERATURE)
            feelTemp = current_feel;
        else
            feelTemp = (current_feel + 37F) * .2f + feelTemp * .8f;
    }

    public void update(float currentEnv, HeatingDeviceContext ctx) {
        // Since we already averaged the core parts, just take torse
        previousTemp = bodyTemp;
        bodyTemp = ctx.getBodyTemperature(BodyPart.TORSO);
        for (BodyPart part : BodyPart.values()) {
            setTemperatureByPart(part, ctx.getBodyTemperature(part));
        }
        // Interpolate with previous envTemp
        if (envTemp == INVALID_TEMPERATURE)
            envTemp = currentEnv;
        else
            envTemp = (currentEnv + 37F) * .2f + envTemp * .8f;
        // Compute feelTemp as area-weighted average of parts
        float currentFeel = 0;
        for (BodyPart part : BodyPart.values()) {
            currentFeel += ctx.getEffectiveTemperature(part) * part.area;
        }
        // Interpolate with previous feelTemp
        if (feelTemp == INVALID_TEMPERATURE)
            feelTemp = currentFeel;
        else
            feelTemp = (currentFeel + 37F) * .2f + feelTemp * .8f;

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
        if (envTemp == INVALID_TEMPERATURE)
            return -20;
        return envTemp;
    }

    public float getFeelTemp() {
        if (feelTemp == INVALID_TEMPERATURE)
            return -20;
        return feelTemp;
    }

    public void setBodyTemp(float bodyTemp) {
        addBodyTemp(bodyTemp - this.bodyTemp);
    }

    public void addBodyTemp(float bodyTemp) {
        for (BodyPart bp : BodyPart.values()) {
            this.clothesOfParts.get(bp).temperature += bodyTemp * bp.affectsCore;
        }
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
        for (Map.Entry<BodyPart, BodyPartData> e : clothesOfParts.entrySet()) {
            e.getValue().reset();
        }
    }

    public float getThermalConductivityByPart(Player player, BodyPart bodyPart) {
        return clothesOfParts.get(bodyPart).getThermalConductivity(player, bodyPart);
    }

    public float getFluidResistanceByPart(Player player, BodyPart bodyPart) {
        return clothesOfParts.get(bodyPart).getFluidResistance(player, bodyPart);
    }

    public float getTemperatureByPart(BodyPart bodyPart) {
        return clothesOfParts.get(bodyPart).temperature;
    }

    public void setTemperatureByPart(BodyPart bodyPart, float t) {
        clothesOfParts.get(bodyPart).temperature = t;
    }

    public void addTemperatureByPart(BodyPart bodyPart, float t) {
        clothesOfParts.get(bodyPart).temperature += t;
    }

	@Override
	public String toString() {
		return "PlayerTemperatureData [difficulty=" + difficulty + ", bodyTemp=" + bodyTemp + ", envTemp=" + envTemp + ", feelTemp=" + feelTemp + ", blockTemp=" + blockTemp + ", clothesOfParts="
			+ clothesOfParts + ", windStrengh=" + windStrengh + "]";
	}
    
}
