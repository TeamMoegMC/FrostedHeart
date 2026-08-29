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

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import com.teammoeg.chorda.io.NBTSerializable;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.climate.FHTemperatureDifficulty;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.ClimateType;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WorldClimate;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.util.Lang;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;

// https://ierga.com/hr/wp-content/uploads/sites/2/2017/10/ASHRAE-55-2013.pdf

public class PlayerTemperatureData implements NBTSerializable {
    public static final int THERMAL_SCHEMA_VERSION = 1;
    public enum BodyPart implements StringRepresentable {
        HEAD(EquipmentSlot.HEAD, 0.10f, 0.10f, 1, 0.85f, 1.00f),
        TORSO(EquipmentSlot.CHEST, 0.45f, 0.50f, 3, 0.55f, 0.85f),
        HANDS(EquipmentSlot.MAINHAND, 0.05f, 0.00f, 1, 0.40f, 0.70f),
        LEGS(EquipmentSlot.LEGS, 0.35f, 0.40f, 3, 0.15f, 0.55f),
        FEET(EquipmentSlot.FEET, 0.05f, 0.00f, 1, 0.00f, 0.15f);
        static final BodyPart[] VALUES = values();
        public static final BodyPart[] CoreParts = new BodyPart[]{HEAD, TORSO, LEGS};
        public final EquipmentSlot slot;
        public final float area;
        public final float affectsCore;
        public final int slotNum;
        public final float immersionLower;
        public final float immersionUpper;
        private final static Map<EquipmentSlot, BodyPart> VANILLA_MAP = Util.make(new EnumMap<>(EquipmentSlot.class), t -> {
            for (BodyPart part : VALUES)
                if (part.slot != null)
                    t.put(part.slot, part);
        });

        BodyPart(
                EquipmentSlot slot,
                float area,
                float affectsCore,
                int slotNum,
                float immersionLower,
                float immersionUpper
        ) {
            this.slot = slot;
            this.area = area;
            this.affectsCore = affectsCore;
            this.slotNum = slotNum;
            this.immersionLower = immersionLower;
            this.immersionUpper = immersionUpper;
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
                case LEGS:
                case HEAD:
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

    }

    public static final int INVALID_TEMPERATURE = 99999;
    @Setter
    private FHTemperatureDifficulty difficulty = null;//in case null, get it from  FHConfig.SERVER.tdiffculty.get()
    float prevCoreBodyTemp;
    @Getter
    float coreBodyTemp;
    @Setter
    float envTemp = INVALID_TEMPERATURE;
    private float netBodyPowerW;
    private float sampledAirTemperatureC;
    private float sampledRadiantFluxWPerM2;
    private float sampledOutdoorWindMPerS;
    private float sampledLocalWindMPerS;
    private boolean sampledCanSeeSky;
    private byte thermalStatusFlags;
    private int lastSyncEnvironment = Integer.MIN_VALUE;
    private int lastSyncCore = Integer.MIN_VALUE;
    private int lastSyncPower = Integer.MIN_VALUE;
    private byte lastSyncFlags;
    private int clientPowerDirection;
    private int clientPowerDirectionHoldTicks;
    private float clientPresentedPowerW;
    private HeatingDeviceContext thermalContext;
    float blockTemp;
    float windStrengh;

    float updateInterval = 0;
    public float smoothedBody;//Client only, smoothed body temperature
    public float smoothedBodyPrev;//Client only, smoothed body temperature


    public final Map<BodyPart, BodyPartData> clothesOfParts = new EnumMap<>(BodyPart.class);

    public void deathResetTemperature() {
        prevCoreBodyTemp = 0;
        coreBodyTemp = 0;
        envTemp = INVALID_TEMPERATURE;
        updateInterval = 0;

        for (BodyPartData i : clothesOfParts.values()) {
            i.bodyEnergyOffsetJ = 0.0D;
			i.feelingTemperatureC = 37.0F;
        }
        netBodyPowerW = 0.0F;
        forceThermalSync();
    }

    public PlayerTemperatureData() {
        for (BodyPart bp : BodyPart.VALUES)
            clothesOfParts.put(bp, new BodyPartData(bp.slotNum));
    }

    public FHTemperatureDifficulty getDifficulty() {
        if (difficulty == null)
            return FHConfig.SERVER.CLIMATE.tdiffculty.get();
        return difficulty;
    }

    public void load(CompoundTag nbt, boolean isPacket) {
        if (isPacket) return;

        int schema = nbt.getInt("thermal_schema");
        if (nbt.contains("difficulty")) {
            try {
                difficulty = FHTemperatureDifficulty.valueOf(
                        nbt.getString("difficulty").toLowerCase());
            } catch (IllegalArgumentException e) {
                difficulty = FHTemperatureDifficulty.normal;
            }
        }

        CompoundTag partClothes = nbt.getCompound("body_parts");
        for (Map.Entry<BodyPart, BodyPartData> entry
                : clothesOfParts.entrySet()) {
            entry.getValue().load(
                    partClothes.getCompound(
                            entry.getKey().getSerializedName()),
                    schema >= THERMAL_SCHEMA_VERSION);
        }
        refreshCoreTemperature();
        prevCoreBodyTemp = coreBodyTemp;
        envTemp = INVALID_TEMPERATURE;
        netBodyPowerW = 0.0F;
        forceThermalSync();
    }

    public void save(CompoundTag nc, boolean isPacket) {
        if (isPacket) return;

        nc.putInt("thermal_schema", THERMAL_SCHEMA_VERSION);
        if (difficulty != null) {
            nc.putString("difficulty", difficulty.name().toLowerCase());
        }
        CompoundTag partClothes = new CompoundTag();
        for (Entry<BodyPart, BodyPartData> entry
                : clothesOfParts.entrySet()) {
            partClothes.put(entry.getKey().getSerializedName(),
                    entry.getValue().save());
        }
        nc.put("body_parts", partClothes);
    }

    public void reset() {
        prevCoreBodyTemp = 0;
        coreBodyTemp = 0;
        envTemp = INVALID_TEMPERATURE;
        smoothedBody = 0;
        netBodyPowerW = 0;
        clearAllClothes();
        for (BodyPartData part : clothesOfParts.values()) {
            part.bodyEnergyOffsetJ = 0.0D;
            part.feelingTemperatureC = 37.0F;
        }
        forceThermalSync();
    }

    public void tick() {
        if (updateInterval > 0)
            updateInterval--;
    }

    void applyThermalObservation(
            double environmentEquivalentTemperatureC,
            double netBodyPowerW,
            double sampledAirTemperatureC,
            double sampledRadiantFluxWPerM2,
            double sampledOutdoorWindMPerS,
            double sampledLocalWindMPerS,
            boolean sampledCanSeeSky,
            byte statusFlags
    ) {
        prevCoreBodyTemp = coreBodyTemp;
        refreshCoreTemperature();
        envTemp = finiteFloat(environmentEquivalentTemperatureC, -20.0F);
        this.netBodyPowerW = finiteFloat(netBodyPowerW, 0.0F);
        this.sampledAirTemperatureC = finiteFloat(
                sampledAirTemperatureC, envTemp);
        this.sampledRadiantFluxWPerM2 = finiteFloat(
                sampledRadiantFluxWPerM2, 0.0F);
        this.sampledOutdoorWindMPerS = finiteFloat(
                sampledOutdoorWindMPerS, 0.0F);
        this.sampledLocalWindMPerS = finiteFloat(
                sampledLocalWindMPerS, 0.0F);
        this.sampledCanSeeSky = sampledCanSeeSky;
        this.thermalStatusFlags = statusFlags;
    }

    private static float finiteFloat(double value, float fallback) {
        return Double.isFinite(value) ? (float) value : fallback;
    }

    void refreshCoreTemperature() {
        float next = 0.0F;
        for (BodyPart part : BodyPart.CoreParts) {
            next += getBodyTempByPart(part) * part.affectsCore;
        }
        coreBodyTemp = next;
    }

    public static LazyOptional<PlayerTemperatureData> getCapability(@Nullable Player player) {
        return FHCapabilities.PLAYER_TEMP.getCapability(player);
    }

    public float getPreviousCoreBodyTemp() {
        return prevCoreBodyTemp;
    }

    public float getEnvTemp() {
        if (envTemp == INVALID_TEMPERATURE)
            return -20;
        return envTemp;
    }

    public float getTotalFeelTemp() {
        return getEnvTemp();
    }

    public void setAllPartsBodyTemp(float t) {
        for (BodyPart bp : BodyPart.VALUES) {
            setBodyTempByPart(bp, t);
        }
        refreshCoreTemperature();
    }

    public void addAllPartsBodyTemp(float added) {
        for (BodyPart bp : BodyPart.VALUES) {
            addBodyTempByPart(bp, added);
        }
        refreshCoreTemperature();
    }

    public void setAllPartsFeelTemp(float t) {
        for (BodyPart bp : BodyPart.VALUES) {
            this.clothesOfParts.get(bp).feelingTemperatureC = t;
        }
    }

    public void addAllPartsFeelTemp(float added) {
        for (BodyPart bp : BodyPart.VALUES) {
            this.clothesOfParts.get(bp).feelingTemperatureC += added;
        }
    }

    // Body clothes methods
    public ItemStackHandler getClothesByPart(BodyPart bodyPart) {
        return clothesOfParts.get(bodyPart).clothes; // Return a copy to prevent direct modification
    }

    public void setClothesByPart(BodyPart bodyPart, int index, ItemStack stack) {
        clothesOfParts.get(bodyPart).clothes.setStackInSlot(index, stack);
    }

    public void clearClothesByPart(BodyPart bodyPart, int index) {
        setClothesByPart(bodyPart, index, ItemStack.EMPTY);
    }

    public void clearAllClothes() {
        for (Map.Entry<BodyPart, BodyPartData> e : clothesOfParts.entrySet()) {
            e.getValue().reset();
        }
    }

    public PartClothData getClothDataByPart(Player player, BodyPart bodyPart) {
        return clothesOfParts.get(bodyPart).getClothData(player, bodyPart);
    }

    void fillClothDataByPart(
            Player player,
            BodyPart bodyPart,
            PartClothData result
    ) {
        clothesOfParts.get(bodyPart).fillClothData(
                player, bodyPart, result);
    }


    public float getBodyTempByPart(BodyPart bodyPart) {
        return (float) (clothesOfParts.get(bodyPart).bodyEnergyOffsetJ
                / TemperatureComputation.partHeatCapacityJPerK(bodyPart));
    }

    public float getAbsoluteBodyTempByPart(BodyPart bodyPart) {
        return (float) (TemperatureComputation.CORE_REFERENCE_TEMPERATURE_C
                + getBodyTempByPart(bodyPart));
    }

    public float getFeelTempByPart(BodyPart bodyPart) {
        return clothesOfParts.get(bodyPart).feelingTemperatureC;
    }

    public void setBodyTempByPart(BodyPart bodyPart, float t) {
        clothesOfParts.get(bodyPart).bodyEnergyOffsetJ = t
                * TemperatureComputation.partHeatCapacityJPerK(bodyPart);
    }

    public void setFeelTempByPart(BodyPart bodyPart, float t) {
        clothesOfParts.get(bodyPart).feelingTemperatureC = t;
    }

    public void addBodyTempByPart(BodyPart bodyPart, float t) {
        addBodyEnergyJ(bodyPart, t
                * TemperatureComputation.partHeatCapacityJPerK(bodyPart));
    }

    public void addFeelTempByPart(BodyPart bodyPart, float t) {
        clothesOfParts.get(bodyPart).feelingTemperatureC += t;
    }

    double getBodyEnergyJ(BodyPart part) {
        return clothesOfParts.get(part).bodyEnergyOffsetJ;
    }

    void addBodyEnergyJ(BodyPart part, double energyJ) {
        if (Double.isFinite(energyJ)) {
            clothesOfParts.get(part).bodyEnergyOffsetJ += energyJ;
        }
    }

    public void addUniformBodyEnergyJ(
            double energyJ,
            float minimumOffsetC,
            float maximumOffsetC
    ) {
        if (!Double.isFinite(energyJ)) return;
        double temperatureDeltaC = energyJ
                / TemperatureComputation.WHOLE_BODY_HEAT_CAPACITY_J_PER_K;
        for (BodyPart part : BodyPart.VALUES) {
            double next = Mth.clamp(
                    getBodyTempByPart(part) + temperatureDeltaC,
                    minimumOffsetC, maximumOffsetC);
            setBodyTempByPart(part, (float) next);
        }
        refreshCoreTemperature();
        forceThermalSync();
    }

    public float getAbsoluteCoreBodyTemp() {
        return coreBodyTemp
                + (float) TemperatureComputation.CORE_REFERENCE_TEMPERATURE_C;
    }

    public float getNetBodyPowerW() { return netBodyPowerW; }
    public float getSampledAirTemperatureC() { return sampledAirTemperatureC; }
    public float getSampledRadiantFluxWPerM2() { return sampledRadiantFluxWPerM2; }
    public float getSampledOutdoorWindMPerS() { return sampledOutdoorWindMPerS; }
    public float getSampledLocalWindMPerS() { return sampledLocalWindMPerS; }
    public boolean isSampledCanSeeSky() { return sampledCanSeeSky; }
    public byte getThermalStatusFlags() { return thermalStatusFlags; }

    public void applyClientThermalSync(
            float environmentTemperatureC,
            float absoluteCoreTemperatureC,
            float netBodyPowerW,
            byte statusFlags
    ) {
        prevCoreBodyTemp = coreBodyTemp;
        coreBodyTemp = absoluteCoreTemperatureC
                - (float) TemperatureComputation.CORE_REFERENCE_TEMPERATURE_C;
        envTemp = environmentTemperatureC;
        this.netBodyPowerW = netBodyPowerW;
        this.thermalStatusFlags = statusFlags;
    }

    public boolean shouldSyncThermalState() {
        int environment = Math.round(getEnvTemp() * 10.0F);
        int core = Math.round(getAbsoluteCoreBodyTemp() * 100.0F);
        int power = Math.round(netBodyPowerW);
        if (environment == lastSyncEnvironment
                && core == lastSyncCore
                && power == lastSyncPower
                && thermalStatusFlags == lastSyncFlags) {
            return false;
        }
        lastSyncEnvironment = environment;
        lastSyncCore = core;
        lastSyncPower = power;
        lastSyncFlags = thermalStatusFlags;
        return true;
    }

    public void forceThermalSync() {
        lastSyncEnvironment = Integer.MIN_VALUE;
        lastSyncCore = Integer.MIN_VALUE;
        lastSyncPower = Integer.MIN_VALUE;
    }

    public void tickClientThermalPresentation() {
        int direction = netBodyPowerW > TemperatureComputation.ORB_POWER_DEADBAND_W
                ? 1 : netBodyPowerW < -TemperatureComputation.ORB_POWER_DEADBAND_W
                ? -1 : 0;
        if (direction != 0) {
            clientPowerDirection = direction;
            clientPowerDirectionHoldTicks = 20;
            clientPresentedPowerW = netBodyPowerW;
        } else if (clientPowerDirectionHoldTicks > 0) {
            clientPowerDirectionHoldTicks--;
            clientPresentedPowerW = clientPowerDirection
                    * TemperatureComputation.ORB_POWER_DEADBAND_W;
        } else {
            clientPowerDirection = 0;
            clientPresentedPowerW = 0.0F;
        }
    }

    public float getClientPresentedPowerW() {
        return clientPresentedPowerW;
    }

    public float getHighestFeelTemp() {
        float highestTemp = Float.NEGATIVE_INFINITY;

        for (BodyPart p : BodyPart.VALUES) {
        	if(p==BodyPart.HANDS)continue;
            float temp = getFeelTempByPart(p);
            if (temp > highestTemp) {
                highestTemp = temp;
            }
        }

        return highestTemp;
    }

    public float getLowestFeelTemp() {
        float lowestTemp = Float.POSITIVE_INFINITY;

        for (BodyPart p : BodyPart.VALUES) {
        	if(p==BodyPart.HANDS)continue;
            float temp = getFeelTempByPart(p);
            if (temp < lowestTemp) {
                lowestTemp = temp;
            }
        }

        return lowestTemp;
    }

    public float getExtremeFeelTemp() {
        // get the one with largest abs value
        float extremeAbsTemp = Float.NEGATIVE_INFINITY;
        float extremeTemp = Float.NEGATIVE_INFINITY;
        for (BodyPart p : BodyPart.VALUES) {
            if(p==BodyPart.HANDS)continue;
            float temp = getFeelTempByPart(p);
            float absTemp = Math.abs(temp);
            if (absTemp > extremeAbsTemp) {
                extremeAbsTemp = absTemp;
                extremeTemp = temp;
            }
        }
        return extremeTemp;
    }

    HeatingDeviceContext thermalContext() {
        if (thermalContext == null) {
            thermalContext = new HeatingDeviceContext();
        }
        return thermalContext;
    }


	@Override
	public String toString() {
		return "PlayerTemperatureData [difficulty=" + difficulty
                + ", bodyTemp=" + coreBodyTemp + ", envTemp=" + envTemp
                + ", netBodyPowerW=" + netBodyPowerW
                + ", clothesOfParts=" + clothesOfParts + "]";
	}
	float oThunderLevel,thunderLevel;

	float oRainLevel, rainLevel;
	ClimateType lastClimate = ClimateType.NONE;

	public void sendInitWeather(ServerPlayer sp,ServerLevel level) {
		WorldClimate wc = WorldClimate.get(level);
		ClimateType climate = level.isThundering() ? ClimateType.BLIZZARD : (level.isRaining() ? ClimateType.SNOW : ClimateType.NONE);
		if (wc != null) {
			climate = wc.getClimate(new ChunkPos(sp.blockPosition()));
		}
		PlayerWeatherCompatibilityModel.VanillaWeatherState state =
				PlayerWeatherCompatibilityModel.fromClimate(climate);
		this.rainLevel = state.rainStrength();
		this.thunderLevel = state.thunderStrength();
		this.oRainLevel = rainLevel;
		this.oThunderLevel = thunderLevel;
		this.lastClimate = climate;
		if (state.raining()) {
			sp.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.START_RAINING, 0.0F));
		} else {
			sp.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.STOP_RAINING, 0.0F));
		}
		sp.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, rainLevel));
		sp.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, thunderLevel));
	}

	public void advanceWeatherCycle(ServerPlayer sp, ClimateType climate) {
		if (climate == lastClimate) {
			return;
		}
		PlayerWeatherCompatibilityModel.VanillaWeatherState previous =
				PlayerWeatherCompatibilityModel.fromClimate(lastClimate);
		PlayerWeatherCompatibilityModel.VanillaWeatherState next =
				PlayerWeatherCompatibilityModel.fromClimate(climate);
		this.oRainLevel = this.rainLevel;
		this.oThunderLevel = this.thunderLevel;
		this.rainLevel = next.rainStrength();
		this.thunderLevel = next.thunderStrength();
		if (previous.raining() != next.raining()) {
			sp.connection.send(new ClientboundGameEventPacket(
					next.raining() ? ClientboundGameEventPacket.START_RAINING : ClientboundGameEventPacket.STOP_RAINING,
					0.0F));
		}
		if (oRainLevel != rainLevel) {
			sp.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, rainLevel));
		}
		if (oThunderLevel != thunderLevel) {
			sp.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, thunderLevel));
		}
		lastClimate = climate;
	}
    
}
