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
import com.teammoeg.frostedheart.bootstrap.reference.FHDamageSources;
import com.teammoeg.frostedheart.content.climate.gamedata.chunkheat.FHBodyDataSyncPacket;
import com.teammoeg.frostedheart.content.climate.player.HeatingDeviceContext.BodyPartContext;
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
        if (event.side == LogicalSide.SERVER && event.phase == Phase.END
                && event.player instanceof ServerPlayer player) {
            double bodyTemp = PlayerTemperatureData.getCapability(event.player)
                    .map(PlayerTemperatureData::getBodyTemp).orElse(0f);
            if (!(player.isCreative() || player.isSpectator())) {
                // Soaked in water wetness
                if (player.tickCount % FHConfig.SERVER.temperatureUpdateIntervalTicks.get() == 0 && player.isInWater()) {
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

                // Hypothermia and Hyperthermia
                if (bodyTemp > 1 || bodyTemp < -1) {
                    if (!player.hasEffect(FHMobEffects.HYPERTHERMIA.get())
                            && !player.hasEffect(FHMobEffects.HYPOTHERMIA.get())) {
                        if (bodyTemp > 1) { // too hot
                            if (bodyTemp <= 2) {
                                player.addEffect(new MobEffectInstance(FHMobEffects.HYPERTHERMIA.get(), 100, 0));
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0)));
                            } else if (bodyTemp <= 3) {
                                player.addEffect(new MobEffectInstance(FHMobEffects.HYPERTHERMIA.get(), 100, 1));
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.CONFUSION, 100, 2)));
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0)));
                            } else if (bodyTemp <= 5) {
                                player.addEffect(new MobEffectInstance(FHMobEffects.HYPERTHERMIA.get(), 100, 2));
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.CONFUSION, 100, 2)));
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0)));
                            } else {
                                player.addEffect(
                                        new MobEffectInstance(FHMobEffects.HYPERTHERMIA.get(), 100, (int) (bodyTemp - 2)));
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.CONFUSION, 100, 2)));
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0)));
                            }
                        } else { // too cold
                            if (bodyTemp >= -2) {
                                player.addEffect(new MobEffectInstance(FHMobEffects.HYPOTHERMIA.get(), 100, 0));
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0)));
                            } else if (bodyTemp >= -3) {
                                player.addEffect(new MobEffectInstance(FHMobEffects.HYPOTHERMIA.get(), 100, 1));
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.CONFUSION, 100, 2)));
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0)));
                            } else if (bodyTemp >= -5) {
                                player.addEffect(new MobEffectInstance(FHMobEffects.HYPOTHERMIA.get(), 100, 2));
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.CONFUSION, 100, 2)));
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0)));
                            } else {
                                player.addEffect(
                                        new MobEffectInstance(FHMobEffects.HYPOTHERMIA.get(), 100, (int) (-bodyTemp - 2)));
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.CONFUSION, 100, 2)));
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0)));
                            }
                        }
                    }
                }
            }
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
                    float envtemp = TemperatureComputation.environment(player, data);

                    // Store it in attribute
                    AttributeInstance envTempAttribute = player.getAttribute(FHAttributes.ENV_TEMPERATURE.get());
                    if (envTempAttribute != null) {
                        envTempAttribute.removeModifier(ENV_TEMP_ATTRIBUTE_UUID);
                        envTempAttribute.addTransientModifier(new AttributeModifier(ENV_TEMP_ATTRIBUTE_UUID, "player environment modifier", envtemp, AttributeModifier.Operation.ADDITION));
                    }

                    /* ENVIRONMENT TEMPERATURE COMPUTATION ENDS */

                    /* EFFECTIVE AND BODY TEMPERATURE COMPUTATION STARTS */

                    // Temporary storage context handled in each update cycle
                    HeatingDeviceContext ctx = new HeatingDeviceContext(player);
                    if (!player.hasEffect(FHMobEffects.INSULATION.get()) && !player.getAbilities().invulnerable) {

                        // Environment-Body Exchange based on clothing, computes effective temperature
                        TemperatureComputation.effective(player, data, ctx);

                        // Equipment Heating modifies effective temp
                        TemperatureComputation.equipmentHeating(player, data, ctx);

                        // Body temperature exchange: from effective and from self-heating
                        FastEnumMap<PlayerTemperatureData.BodyPart, Float> partBodyTemps =
                                TemperatureComputation.body(player, data, ctx);

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

                        data.updateWhenInsulated(envtemp, totalConductivity);
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
