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

import java.util.Map.Entry;
import java.util.UUID;

import com.mojang.datafixers.util.Pair;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.*;
import com.teammoeg.frostedheart.bootstrap.common.FHAttributes;
import com.teammoeg.frostedheart.bootstrap.common.FHMobEffects;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata.FHBodyDataSyncPacket;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData.BodyPart;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.LogicalSide;

public class TemperatureUpdate {
	public static final UUID envTempId = UUID.fromString("95c1eab4-8f3a-4878-aaa7-a86722cdfb07");
	//Do not use static final in config because this is reloaded each world
    /*public static final Double HEAT_EXCHANGE_CONSTANT = FHConfig.SERVER.heatExchangeConstant.get();
    
    public static final int TEMP_SKY_LIGHT_THRESHOLD = FHConfig.SERVER.tempSkyLightThreshold.get();
    public static final int SNOW_TEMP_MODIFIER = FHConfig.SERVER.snowTempModifier.get();
    public static final int BLIZZARD_TEMP_MODIFIER = FHConfig.SERVER.blizzardTempModifier.get();
    public static final int DAY_NIGHT_TEMP_AMPLITUDE = FHConfig.SERVER.dayNightTempAmplitude.get();
    public static final int ON_FIRE_TEMP_MODIFIER = FHConfig.SERVER.onFireTempModifier.get();
    public static final double HURTING_HEAT_UPDATE = FHConfig.SERVER.hurtingHeatUpdate.get();
    public static final int MIN_BODY_TEMP_CHANGE = FHConfig.SERVER.minBodyTempChange.get();
    public static final int MAX_BODY_TEMP_CHANGE = FHConfig.SERVER.maxBodyTempChange.get();*/
    public static final float FOOD_EXHAUST_COLD=.05F;

    /**
     * Perform temperature effect
     *
     * @param event fired every tick on player
     */
    public static void regulateTemperature(PlayerTickEvent event) {
        if (event.side == LogicalSide.SERVER && event.phase == Phase.END
                && event.player instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) event.player;
            double calculatedTarget = PlayerTemperatureData.getCapability(event.player).map(PlayerTemperatureData::getBodyTemp).orElse(0f);
            if (!(player.isCreative() || player.isSpectator())) {
                if (calculatedTarget > 1 || calculatedTarget < -1) {
                    if (!player.hasEffect(FHMobEffects.HYPERTHERMIA.get())
                            && !player.hasEffect(FHMobEffects.HYPOTHERMIA.get())) {
                        if (calculatedTarget > 1) { // too hot
                            if (calculatedTarget <= 2) {
                                player.addEffect(new MobEffectInstance(FHMobEffects.HYPERTHERMIA.get(), 100, 0));
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0)));
                            } else if (calculatedTarget <= 3) {
                                player.addEffect(new MobEffectInstance(FHMobEffects.HYPERTHERMIA.get(), 100, 1));
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.CONFUSION, 100, 2)));
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0)));
                            } else if (calculatedTarget <= 5) {
                                player.addEffect(new MobEffectInstance(FHMobEffects.HYPERTHERMIA.get(), 100, 2));
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.CONFUSION, 100, 2)));
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0)));
                            } else {
                                player.addEffect(
                                        new MobEffectInstance(FHMobEffects.HYPERTHERMIA.get(), 100, (int) (calculatedTarget - 2)));
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.CONFUSION, 100, 2)));
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0)));
                            }
                        } else { // too cold
                            if (calculatedTarget >= -2) {
                                player.addEffect(new MobEffectInstance(FHMobEffects.HYPOTHERMIA.get(), 100, 0));
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0)));
                            } else if (calculatedTarget >= -3) {
                                player.addEffect(new MobEffectInstance(FHMobEffects.HYPOTHERMIA.get(), 100, 1));
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.CONFUSION, 100, 2)));
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0)));
                            } else if (calculatedTarget >= -5) {
                                player.addEffect(new MobEffectInstance(FHMobEffects.HYPOTHERMIA.get(), 100, 2));
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.CONFUSION, 100, 2)));
                                player.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0)));
                            } else {
                                player.addEffect(
                                        new MobEffectInstance(FHMobEffects.HYPOTHERMIA.get(), 100, (int) (-calculatedTarget - 2)));
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
            // ignore creative and spectator players
            // no longer ignore for easier debug
            // TODO: find another way...we should save resources
            // if (player.isCreative() || player.isSpectator())
            // return;
            PlayerTemperatureData.getCapability(event.player).ifPresent((data) -> {
                if (player.tickCount % FHConfig.SERVER.temperatureUpdateIntervalTicks.get() == 0) {
                    // Soak in water modifier
                    if (player.isInWater()) {
                        boolean hasArmor = false;
                        for (ItemStack is : player.getArmorSlots()) {
                            if (!is.isEmpty()) {
                                hasArmor = true;
                                break;
                            }
                        }
                        MobEffectInstance current = player.getEffect(FHMobEffects.WET.get());
                        if (hasArmor)
                            player.addEffect(new MobEffectInstance(FHMobEffects.WET.get(), FHConfig.SERVER.wetEffectDuration.get() * FHConfig.SERVER.wetClothesDurationMultiplier.get(), 0, false ,false));// punish for wet clothes
                        else if (current == null || current.getDuration() < FHConfig.SERVER.wetEffectDuration.get())
                            player.addEffect(new MobEffectInstance(FHMobEffects.WET.get(), FHConfig.SERVER.wetEffectDuration.get(), 0, false ,false));
                    }

                    /* Initialization */

                    // Load config
                    double tspeed = FHConfig.SERVER.tempSpeed.get();

                    /* Environment temperature */

                    // World and chunk temperature: Climate, time, heat adjusts
                    Level world = player.level();
                    BlockPos pos = new BlockPos((int) player.getX(), (int) player.getEyeY(), (int) player.getZ());
                    float envtemp = WorldTemperature.air(world, pos) - 37F; // 37-based

                    // Surrounding block temperature
                    Pair<Float, Float> btp = new SurroundingTemperatureSimulator(player).getBlockTemperatureAndWind(player.getX(), player.getEyeY() - 0.7f, player.getZ());
                    float bt = btp.getFirst();
                    envtemp += bt;
                    //int wind=btp.getSecond()+WorldTemperature.getClimateWind(world);

                    // Day-night temperature
                    int skyLight = world.getChunkSource().getLightEngine().getLayerListener(LightLayer.SKY).getLightValue(pos);
                    float dayTime = world.getDayTime() % 24000L;
                    float relativeTime = Mth.sin((float) Math.toRadians(dayTime / ((float) 200 / 3))); // range from -1 to 1
                    if (skyLight < FHConfig.SERVER.tempSkyLightThreshold.get()) {
                        relativeTime = -1;
                    }

                    // Weather temperature modifier
                    float weatherMultiplier = 1.0F;
                    if (world.isRaining() && WorldTemperature.isRainingAt(player.blockPosition(), world)) {
                        envtemp -= FHConfig.SERVER.snowTempModifier.get();
                        if (world.isThundering()) {
                            envtemp -= FHConfig.SERVER.blizzardTempModifier.get();
                        }
                        weatherMultiplier = 0.2F;
                    }

                    envtemp += relativeTime * FHConfig.SERVER.dayNightTempAmplitude.get() * weatherMultiplier;

                    // Burning temperature
                    if (player.isOnFire())
                        envtemp += FHConfig.SERVER.onFireTempModifier.get();

                    // Handle Attributes
                    player.getAttribute(FHAttributes.ENV_TEMPERATURE.get()).removeModifier(envTempId);
                    player.getAttribute(FHAttributes.ENV_TEMPERATURE.get()).addTransientModifier(new AttributeModifier(envTempId, "player environment modifier", envtemp, Operation.ADDITION));

                    // Getting environment temperature end

                    // Calculating body temperature change start

                    // Insulation
                    envtemp=(float) player.getAttributeValue(FHAttributes.ENV_TEMPERATURE.get());
                    // float insulation = (float) player.getAttributeValue(FHAttributes.INSULATION.get());
                    float totalConductivity = 0.0f;
                    // If the player has the insulation effect, insulation is set to 1, so no heat exchange with the environment
                    // Also disable when player is creative
                    if (!player.hasEffect(FHMobEffects.INSULATION.get())&&!player.getAbilities().invulnerable) {
                        for (BodyPart part:PlayerTemperatureData.BodyPart.values()) {

                            float ratio = part.area;
                            float thermalConductivity = data.getThermalConductivityByPart(player, part);
                            totalConductivity += ratio*thermalConductivity;
                            float temperature = data.getTemperatureByPart(part);
                            float dt = (temperature - envtemp) * thermalConductivity;
                            float unit = 0.006f; // 1 unit per tick is 0.012 degree per second
                            temperature -= (2*unit) * (dt/10); // charge 2 units for every 10 dt

                            // still: 10 dt
                            // walking: 15 dt
                            // sprinting: 25 dt

                            // 1 unit = 60W
                            float selfHeatRate=data.getDifficulty().heat_unit;
                            unit*=selfHeatRate;
                            // base generation when cold: 1 unit
                            if (temperature < 0.0&&player.getFoodData().getFoodLevel()>0) {
                                temperature += unit;
                                // TODO: cost hunger for cold, adjust for difficulty
                                player.causeFoodExhaustion(FOOD_EXHAUST_COLD);
                            }

                            double speedSquared = player.getDeltaMovement().horizontalDistanceSqr(); // Horizontal movement speed squared
                            boolean isSprinting = player.isSprinting();
                            boolean isOnVehicle = player.getVehicle() != null;
                            boolean isWalking = speedSquared > 0.001 && !isSprinting && !isOnVehicle;
                            if (isSprinting) {
                                temperature += 4*unit; // Running increases temperature by 4 units
                            } else if (isWalking) { // Assuming there's a method to check walking
                                temperature += 2*unit; // Walking increases temperature by 2 units
                            } else {
                                temperature += unit; // Standing still or being in a vehicle increases temperature by 1 unit
                            }
                            //gain an extra unit if too cold
                            /*if(!isSprinting&&temperature < 0.0) {
                            	 temperature += unit;
                                 player.causeFoodExhaustion(FOOD_EXHAUST_COLD);
                            }*/
                            // TODO: degree I/II/III burn if dt=+20/+30/+40
                            // TODO: degree I/II/III freeze if dt=-50/-60/-70
                            data.setTemperatureByPart(part, temperature);
                        }
                    } else {
                        MobEffectInstance insulationEffect = player.getEffect(FHMobEffects.INSULATION.get());
                        if (insulationEffect != null) {
                            int amp = insulationEffect.getAmplifier();
                            // clamp to 0 to 100
                            amp = Mth.clamp(amp, 0, 100);
                            totalConductivity  = amp / 100.0f;
                        }
                    }
                    

                    // Equipments
                    // TODO: heat up equipments!
                    if(!player.isCreative()&&!player.isSpectator())
                    data.update(envtemp, totalConductivity);
                    //System.out.println("===================================");
                    //for(BodyPart bp:BodyPart.values())
                    //System.out.println(bp+":"+data.getTemperatureByPart(bp));
                    //FHNetwork.send(PacketDistributor.PLAYER.with(() -> player), new FHBodyDataSyncPacket(player));
                }

                FHNetwork.sendPlayer(player, new FHBodyDataSyncPacket(player));
            });
        }
    }
}
