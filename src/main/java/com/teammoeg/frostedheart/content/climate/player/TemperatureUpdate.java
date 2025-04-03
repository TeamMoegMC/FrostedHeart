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

import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.chorda.util.struct.FastEnumMap;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.common.FHAttributes;
import com.teammoeg.frostedheart.bootstrap.common.FHMobEffects;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.gamedata.chunkheat.FHBodyDataSyncPacket;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData.BodyPart;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.LogicalSide;

import static com.teammoeg.frostedheart.content.climate.player.TemperatureComputation.ENV_TEMP_ATTRIBUTE_UUID;

public class TemperatureUpdate {

	//Do not use static final in config because this is reloaded each world
    public static final float FOOD_EXHAUST_COLD=.05F;
    /*
    public static final int TEMP_SKY_LIGHT_THRESHOLD = FHConfig.SERVER.tempSkyLightThreshold.get();
    public static final int SNOW_TEMP_MODIFIER = FHConfig.SERVER.snowTempModifier.get();
    public static final int BLIZZARD_TEMP_MODIFIER = FHConfig.SERVER.blizzardTempModifier.get();
    public static final int DAY_NIGHT_TEMP_AMPLITUDE = FHConfig.SERVER.dayNightTempAmplitude.get();
    public static final int ON_FIRE_TEMP_MODIFIER = FHConfig.SERVER.onFireTempModifier.get();
    public static final double HURTING_HEAT_UPDATE = FHConfig.SERVER.hurtingHeatUpdate.get();
    public static final int MIN_BODY_TEMP_CHANGE = FHConfig.SERVER.minBodyTempChange.get();
    public static final int MAX_BODY_TEMP_CHANGE = FHConfig.SERVER.maxBodyTempChange.get();*/

    public static TemperatureThreadingPool threadingPool;
    /**
     * Perform temperature effect
     *
     * @param event fired every tick on player
     */
    public static void regulateTemperature(PlayerTickEvent event) {
        if (event.side == LogicalSide.SERVER && event.phase == Phase.END && event.player instanceof ServerPlayer player) {

            // Fetch the player temperature data
            PlayerTemperatureData.getCapability(player).ifPresent((data) -> {
                if (player.isCreative() || player.isSpectator() || player.isInvulnerable()) {
                    return;
                }

                if (player.tickCount % FHConfig.SERVER.temperatureUpdateIntervalTicks.get() != 0) {
                    return;
                }

                // Rest of update logic is handled every second.

                // Soaked in water wetness
                if (player.isInWater()) {
                    // Check if an armor piece is on
                    boolean hasArmor = false;
                    for (ItemStack is : player.getArmorSlots()) {
                        if (!is.isEmpty()) {
                            hasArmor = true;
                            break;
                        }
                    }
                    // Check the current Wet Effect
                    MobEffectInstance current = player.getEffect(FHMobEffects.WET.get());
                    // If armor is on, player gets a longer wet effect
                    if (hasArmor) {
                        player.addEffect(new MobEffectInstance(FHMobEffects.WET.get(),
                                FHConfig.SERVER.wetEffectDuration.get() *
                                        FHConfig.SERVER.wetClothesDurationMultiplier.get(),
                                0, false, false));// punish for wet clothes
                    }
                    // Otherwise, if there is no wet effect now, add normal wet effect
                    else if (current == null || current.getDuration() < FHConfig.SERVER.wetEffectDuration.get()) {
                        player.addEffect(new MobEffectInstance(FHMobEffects.WET.get(),
                                FHConfig.SERVER.wetEffectDuration.get(), 0, false, false));
                    }
                }

                // Torso leads to Hypothermia and Hyperthermia
                double torso = data.getBodyTempByPart(BodyPart.TORSO);
                if (torso > 1 || torso < -1) {
                    if (!player.hasEffect(FHMobEffects.HYPERTHERMIA.get())
                            && !player.hasEffect(FHMobEffects.HYPOTHERMIA.get())) {
                        if (torso > 1) { // too hot
                            if (torso <= 2) {
                                player.addEffect(new MobEffectInstance(FHMobEffects.HYPERTHERMIA.get(), 100, 0));
                            } else if (torso <= 3) {
                                player.addEffect(new MobEffectInstance(FHMobEffects.HYPERTHERMIA.get(), 100, 1));
                            } else if (torso <= 5) {
                                player.addEffect(new MobEffectInstance(FHMobEffects.HYPERTHERMIA.get(), 100, 2));
                            } else {
                                player.addEffect(
                                        new MobEffectInstance(FHMobEffects.HYPERTHERMIA.get(), 100, (int) (torso - 2)));

                            }
                        } else { // too cold
                            if (torso >= -2) {
                                player.addEffect(new MobEffectInstance(FHMobEffects.HYPOTHERMIA.get(), 100, 0));
                            } else if (torso >= -3) {
                                player.addEffect(new MobEffectInstance(FHMobEffects.HYPOTHERMIA.get(), 100, 1));
                            } else if (torso >= -5) {
                                player.addEffect(new MobEffectInstance(FHMobEffects.HYPOTHERMIA.get(), 100, 2));
                            } else {
                                player.addEffect(
                                        new MobEffectInstance(FHMobEffects.HYPOTHERMIA.get(), 100, (int) (-torso - 2)));
                            }
                        }
                    }
                }

                // Head leads to confusion
                double head = data.getBodyTempByPart(BodyPart.HEAD);
                if (head > 1 || head < -1) {
                    if (!player.hasEffect(MobEffects.CONFUSION)) {
                        if (head > 1) { // too hot
                            if (head <= 2) {
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.CONFUSION, 100, 0)));
                            } else if (head <= 3) {
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.CONFUSION, 100, 1)));
                            } else if (head <= 5) {
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.CONFUSION, 100, 2)));
                            } else {
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.CONFUSION, 100, 3)));
                            }
                        } else { // too cold
                            if (head >= -2) {
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.CONFUSION, 100, 0)));
                            } else if (head >= -3) {
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.CONFUSION, 100, 1)));
                            } else if (head >= -5) {
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.CONFUSION, 100, 2)));
                            } else {
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.CONFUSION, 100, 3)));
                            }
                        }
                    }
                }

                // Feet lead to slowness
                double feet = data.getBodyTempByPart(BodyPart.FEET);
                double feetAbs = Math.abs(feet);
                double legs = data.getBodyTempByPart(BodyPart.LEGS);
                double legsAbs = Math.abs(legs);
                double lowerLimb;
                if (feetAbs > legsAbs) {
                    lowerLimb = feet;
                } else {
                    lowerLimb = legs;
                }
                if (lowerLimb > 1 || lowerLimb < -1) {
                    if (!player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
                        if (lowerLimb > 1) { // too hot
                            if (lowerLimb <= 2) {
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0)));
                            } else if (lowerLimb <= 3) {
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1)));
                            } else if (lowerLimb <= 5) {
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2)));
                            } else {
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3)));
                            }
                        } else { // too cold
                            if (lowerLimb >= -2) {
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0)));
                            } else if (lowerLimb >= -3) {
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1)));
                            } else if (lowerLimb >= -5) {
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2)));
                            } else {
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3)));
                            }
                        }
                    }
                }

                // Hands lead to slow digging
                double hands = data.getBodyTempByPart(BodyPart.HANDS);
                if (hands > 1 || hands < -1) {
                    if (!player.hasEffect(MobEffects.DIG_SLOWDOWN)) {
                        if (hands > 1) { // too hot
                            if (hands <= 2) {
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0)));
                            } else if (hands <= 3) {
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 1)));
                            } else if (hands <= 5) {
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 2)));
                            } else {
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 3)));
                            }
                        } else { // too cold
                            if (hands >= -2) {
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0)));
                            } else if (hands >= -3) {
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 1)));
                            } else if (hands >= -5) {
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 2)));
                            } else {
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 3)));
                            }
                        }
                    }
                }

                // Frostbite and Burning effects due to effective temp
                TemperatureComputation.burning(player, data);

            });
        }
    }

    /**
     * Perform temperature tick logic
     * <p>
     * Updated every 10 ticks (0.5s)
     *
     * @param event fired every tick on player
     */
    public static void updateTemperature(PlayerTickEvent event) {
        if (event.side == LogicalSide.SERVER && event.phase == Phase.START && event.player instanceof ServerPlayer player) {

        	// Fetch the player temperature data
            PlayerTemperatureData.getCapability(player).ifPresent((data) -> {

                /* MULTI-THREADED SURROUNDING BLOCK TEMPERATURE SIMULATION STARTS */

                // Interval-based Environment Temperature Update: 20 ticks by default.
                data.tick();
                if (data.updateInterval <= 0) {
                    // Multithreaded Environment Simulation
                    if (threadingPool.tryCommitWork(player))
                        data.updateInterval = FHConfig.SERVER.envTempUpdateIntervalTicks.get();
                }

                /* MULTI-THREADED SURROUNDING BLOCK TEMPERATURE SIMULATION ENDS */

                // Rest of update logic is handled every second.
                if (player.tickCount % FHConfig.SERVER.temperatureUpdateIntervalTicks.get() == 0) {

                    /* ENVIRONMENT TEMPERATURE COMPUTATION STARTS */

                    // Compute environment
                    float rawenvtemp = TemperatureComputation.environment(player, data);

                    


                    /* ENVIRONMENT TEMPERATURE COMPUTATION ENDS */

                    /* EFFECTIVE AND BODY TEMPERATURE COMPUTATION STARTS */

                    // Temporary storage context handled in each update cycle
                    HeatingDeviceContext ctx = new HeatingDeviceContext(player);
                    if (!player.hasEffect(FHMobEffects.INSULATION.get()) && !player.getAbilities().invulnerable) {
                    	// Store environment temperature in attribute
                        AttributeInstance envTempAttribute = player.getAttribute(FHAttributes.ENV_TEMPERATURE.get());
                        if (envTempAttribute != null) {
                            envTempAttribute.removeModifier(ENV_TEMP_ATTRIBUTE_UUID);
                            envTempAttribute.addTransientModifier(new AttributeModifier(ENV_TEMP_ATTRIBUTE_UUID, "player environment modifier", rawenvtemp, AttributeModifier.Operation.ADDITION));
                        }
                        // Environment-Body Exchange based on clothing, computes effective temperature
                        float envtemp = (float) player.getAttributeValue(FHAttributes.ENV_TEMPERATURE.get());
                        FastEnumMap<BodyPart,PartClothData> clothDataMap=new FastEnumMap<>(BodyPart.values());
                        // Environment-Body Exchange
                        for (PlayerTemperatureData.BodyPart part : PlayerTemperatureData.BodyPart.values()) {
                            // ranges [0, 1]
                        	PartClothData  clothData=data.getClothDataByPart(player, part);
                            clothDataMap.put(part, clothData);
                            // This is a body part's "Body Temperature" from last time
                            float partBodyTemp = data.getBodyTempByPart(part);
                            // according to wiki, body ends are 5 degrees lower than core parts, so they are 32-based
                            float partEnvTemp = envtemp - (part.isBodyEnd() ? 5 : 0);
                            // Env and Body exchanges temperature
                            float partBodyEnvExchangeTemp = (partEnvTemp - partBodyTemp) * clothData.heatConductivity;
                            float partEffectiveTemp = partBodyTemp + partBodyEnvExchangeTemp;
                            // Store them in context
                            ctx.setPartData(part, partBodyTemp, partEffectiveTemp);
                        }

                        // Equipment Heating modifies effective temp
                        TemperatureComputation.equipmentHeating(player, data, ctx);

                        // Body temperature exchange: from effective and from self-heating
                        Level world = player.level();
                        // Range 0-100
                        int wind = WorldTemperature.wind(world);
                        // Range 0-1
                        float openness = data.getAirOpenness();
                        // [0,1]
                        float effectiveWind = openness * Mth.clamp(wind, 0, 100) / 100F;

                        // Apply Exchanged Temperature, and Self-Heating
                        // Temporary storage map
                        FastEnumMap<PlayerTemperatureData.BodyPart, Float> partBodyTemps = new FastEnumMap<>(PlayerTemperatureData.BodyPart.values());
                        for (PlayerTemperatureData.BodyPart part : PlayerTemperatureData.BodyPart.values()) {
                            // Apply effective heat exchange to part temperature
                            HeatingDeviceContext.BodyPartContext pctx = ctx.getPartData(part);
                            // Part Body Temperature
                            float pbTemp = pctx.getBodyTemperature();
                            float dt = pctx.getEffectiveTemperature() - pbTemp;
                            // By default heatExchangeTimeConstant = 167
                            // Since this logic is invoked every 20 ticks (1s), this means
                            // 1 unit = 0.006 degrees per second
                            float unit = 1F / FHConfig.SERVER.heatExchangeTimeConstant.get();
                            // Since 1Y degree deviation leads to hypothermia, that means at one unit rate,
                            // it takes 167 (= 1/0.006) seconds to reach hypothermia
                            // For every heatExchangeTimeConstant (default = 5) degrees of deviation, we increase the loss rate by 1 unit.
                            // Time = 167 / (Deviation / 5)
                            // Scenarios: (no self-heating, environment effective temp -13C)
                            // Deviation 50Y, Rate 10 units, 16.7 sec to hypothermia
                            // Deviation 53% * 50Y = 24Y, Rate 5 units, 34.8 sec to hypothermia (effectively, straw suit)
                            // Deviation 36% * 50Y = 18Y, Rate 3.6 units, 46.4 sec to hypothermia (effectively, leather suit)
                            // Deviation 26% * 50Y = 13Y, Rate 2.6 units, 64.2 sec to hypothermia (effectively, wool suit)

                            // fluid conductivity is different in different medium,
                            // fluid resistance [0,1] from clothing helps dealing with this
                            // by linearly diminishing the conductivity multiplier due to various fluid movement

                            // wikipedia: https://en.wikipedia.org/wiki/Thermal_conductivity_and_resistivity
                            // thermal conductivity:
                            // water: 0.6089
                            // air: 0.026
                            // powdered snow: 0.05
                            // water/air = 23.41
                            // we use ratio = 25

                            float fluidModifier = 0F;
                            float partFluidResist = clothDataMap.get(part).windResist;
                            if (player.isInWater())
                                fluidModifier = 25F * (1 - partFluidResist);
                            // interestingly powdered snow does not affect conductivity that much, it just makes envtemp low
                            // however, the human body melts snow, and that generates water, which may go into the body,
                            // if clothing is not fluid resisting enough, and take away heats.
                            // thus a solution here is an average...
                            else if (player.isInPowderSnow)
                                fluidModifier = 15F * (1 - partFluidResist);
                            else {
                                // gets up to 5F
                                fluidModifier += 5F * effectiveWind * (1 - partFluidResist);
                                // evaporation takes away a LOT of heat. it gets up to 10F
                                if (player.hasEffect(FHMobEffects.WET.get())) {
                                    fluidModifier += 10F * (1 - partFluidResist);
                                }
                            }
                            
                            // May be negative! (when dt < 0)
                            float heatExchangedUnits = (float) ((1 + fluidModifier) * unit * (dt / FHConfig.SERVER.heatExchangeTempConstant.get()));
                            System.out.println("fm:"+fluidModifier);
                            // Self-Heating
                            float selfHeatRate = data.getDifficulty().heat_unit; // normally 1
                            float movementHeatedUnits = 0;
                            // Apply Self-heating based on movement status
                            // Food exhaustion is handled by Vanilla, so we don't repeat here
                            double speedSquared = player.getDeltaMovement().horizontalDistanceSqr(); // Horizontal movement speed squared
                            boolean isSprinting = player.isSprinting();
                            boolean isOnVehicle = player.getVehicle() != null;
                            boolean isWalking = speedSquared > 0.001 && !isSprinting && !isOnVehicle;
                            if (isSprinting) {
                                movementHeatedUnits += 4 * selfHeatRate * unit; // Running increases temperature by 4 units
                            } else if (isWalking) { // Assuming there's a method to check walking
                                movementHeatedUnits += 2 * selfHeatRate * unit; // Walking increases temperature by 2 units
                            } else {
                                movementHeatedUnits += 1F * selfHeatRate *unit;
                            }

                            // Additional Homeostasis using Stored (Food) Energy
                            float homeostasisUnits = 0;
                            // homeostasis only happens when deviation is negative even after heat exchange and movement
                            // Note 0Y here represents the normal body temperature of 37C
                            final float deviation = 0 + (pbTemp + heatExchangedUnits + movementHeatedUnits);
                            // We apply additional units based on a deviation need, exhausting more food
                            if (deviation < 0 && player.getFoodData().getFoodLevel() > 0) {
                                if (deviation > -0.5) {
                                    homeostasisUnits += 2F * selfHeatRate * unit;
                                    player.causeFoodExhaustion(FOOD_EXHAUST_COLD * 2F * part.area);
                                } else if (deviation > -1) {
                                    homeostasisUnits += 3F * selfHeatRate * unit;
                                    player.causeFoodExhaustion(FOOD_EXHAUST_COLD * 3F * part.area);
                                } else {
                                    homeostasisUnits += 4F * selfHeatRate * unit;
                                    player.causeFoodExhaustion(FOOD_EXHAUST_COLD * 4F * part.area);
                                }
                            }

                                            /*
                                            FHMain.LOGGER.debug("Deviation: " + deviation);
                                            FHMain.LOGGER.debug("Homeostasis: " + homeostasisUnits);
                                            FHMain.LOGGER.debug("Movement: " + movementHeatedUnits);
                                            FHMain.LOGGER.debug("Exchange: " + heatExchangedUnits);
                                             */

                            // Apply all to pbTemp
                            pbTemp += heatExchangedUnits;

                            if (part.canGenerateHeat()) {
                                pbTemp += movementHeatedUnits + homeostasisUnits;
                            } else {
                                pbTemp += movementHeatedUnits;
                            }

                            // FHMain.LOGGER.debug("pbTemp: " + pbTemp);

                            partBodyTemps.put(part, pbTemp);
                        }

                        // Calculate heat transfer between each part
                        // Core parts share temperature
                        float coreTemp = 0;
                        for (PlayerTemperatureData.BodyPart corePart : PlayerTemperatureData.BodyPart.CoreParts) {
                            coreTemp += partBodyTemps.get(corePart) * corePart.affectsCore;
                        }
                        for (PlayerTemperatureData.BodyPart corePart : PlayerTemperatureData.BodyPart.CoreParts)
                            partBodyTemps.put(corePart, coreTemp);

                        //From leg to feets
                        final float transferRate = 0.1F;
                        final float maxDelta = 3F;
                        final float minDelta = 0.1F;

                        float dlegfeet = Mth.clamp(partBodyTemps.get(PlayerTemperatureData.BodyPart.LEGS) - partBodyTemps.get(PlayerTemperatureData.BodyPart.FEET), -maxDelta, maxDelta);
                        if (Mth.abs(dlegfeet) > minDelta) {
                            float newfeet = partBodyTemps.get(PlayerTemperatureData.BodyPart.FEET) + dlegfeet * transferRate;
                            float newleg = partBodyTemps.get(PlayerTemperatureData.BodyPart.LEGS) - dlegfeet * transferRate;
                            partBodyTemps.put(PlayerTemperatureData.BodyPart.FEET, newfeet);
                            partBodyTemps.put(PlayerTemperatureData.BodyPart.LEGS, newleg);
                        }

                        //from chest to hands
                        float dhandchest = Mth.clamp(partBodyTemps.get(PlayerTemperatureData.BodyPart.TORSO) - partBodyTemps.get(PlayerTemperatureData.BodyPart.HANDS), -maxDelta, maxDelta);
                        if (Mth.abs(dhandchest) > minDelta) {
                            float newhands = partBodyTemps.get(PlayerTemperatureData.BodyPart.HANDS) + dhandchest * transferRate;
                            float newtorso = partBodyTemps.get(PlayerTemperatureData.BodyPart.TORSO) - dhandchest * transferRate;
                            partBodyTemps.put(PlayerTemperatureData.BodyPart.HANDS, newhands);
                            partBodyTemps.put(PlayerTemperatureData.BodyPart.TORSO, newtorso);
                        }


                        // Recycle the FEM, set back to context
                        for (BodyPart part : BodyPart.values()) {
                            ctx.setBodyTemperature(part, partBodyTemps.get(part));
                        }

                        // Update data and do the relevant display purpose computation there
                        data.update((float) player.getAttributeValue(FHAttributes.ENV_TEMPERATURE.get()), ctx);

                    }
                    // Apply insulation effect. Above 100 amplifiers are treated as 100.
                    else {
                        float totalConductivity = 0;
                        MobEffectInstance insulationEffect = player.getEffect(FHMobEffects.INSULATION.get());
                        if (insulationEffect != null) {
                            totalConductivity = Mth.clamp(insulationEffect.getAmplifier(), 0, 100) / 100.0f;
                        }

                        data.updateWhenInsulated(rawenvtemp, totalConductivity);
                    }

                    /* EFFECTIVE AND BODY TEMPERATURE COMPUTATION ENDS */

                }

                // Sync to client
                FHNetwork.INSTANCE.sendPlayer(player, new FHBodyDataSyncPacket(player));
            });
        }
    }

	public static void init() {
		threadingPool=new TemperatureThreadingPool(FHConfig.SERVER.envTempThreadCount.get());
		
	}
	public static void shutdown() {
		threadingPool.close();
		threadingPool=null;
	}
}
