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

import java.util.UUID;

import com.mojang.datafixers.util.Pair;
import com.teammoeg.chorda.CompatModule;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.chorda.util.struct.FastEnumMap;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.common.FHAttributes;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.bootstrap.common.FHMobEffects;
import com.teammoeg.frostedheart.compat.curios.CuriosCompat;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.gamedata.chunkheat.FHBodyDataSyncPacket;
import com.teammoeg.frostedheart.content.climate.player.HeatingDeviceContext.BodyPartContext;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData.BodyPart;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.LogicalSide;
import top.theillusivec4.curios.api.type.ISlotType;

public class TemperatureUpdate {
	public static final UUID ENV_TEMP_ATTRIBUTE_UUID = UUID.fromString("95c1eab4-8f3a-4878-aaa7-a86722cdfb07");
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
    public static final float FOOD_EXHAUST_COLD=.05F;

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

            // The Temperature Deviation
            final float TEMP_DIV = 5;

        	// Fetch the player temperature data
            PlayerTemperatureData.getCapability(player).ifPresent((data) -> {

                // Interval-based Environment Temperature Update: 20 ticks by default.
                data.tick();
                if (data.updateInterval <= 0) {
                    // Multithreaded Environment Simulation
                    if (threadingPool.tryCommitWork(player))
                        data.updateInterval = FHConfig.SERVER.envTempUpdateIntervalTicks.get();
                }
                // Interval Update on Player Temperature
                if (player.tickCount % FHConfig.SERVER.temperatureUpdateIntervalTicks.get() == 0) {

                    /* ENVIRONMENT TEMPERATURE COMPUTATION STARTS */

                    // World Temp: Dimension, Biome, Climate, Time, heat adjusts
                    Level world = player.level();
                    BlockPos pos = new BlockPos((int) player.getX(), (int) player.getEyeY(), (int) player.getZ());
                    // We use 37C based temperature here.
                    // The base temperature means around -10C, which becomes -47C.
                    float envtemp = WorldTemperature.air(world, pos) - 37F; // 37-based

                    // Surrounding block temperature.
                    // We calculate the block temperature using a separate pool.
                    // See blockTemp usage for more details.
                    // This shift ranges a lot.
                    float bt = data.blockTemp;
                    envtemp += bt;

                    // Day-night temperature
                    int skyLight = world.getChunkSource().getLightEngine().getLayerListener(LightLayer.SKY).getLightValue(pos);
                    float dayTime = world.getDayTime() % 24000L;
                    float relativeTime = Mth.sin((float) Math.toRadians(dayTime / ((float) 200 / 3))); // range from -1 to 1
                    if (skyLight < FHConfig.SERVER.tempSkyLightThreshold.get()) {
                        relativeTime = -1;
                    }

                    // Weather temperature modifier
                    // This shift ranges [-10, 0]
                    float weatherMultiplier = 1.0F;
                    if (world.isRaining() && WorldTemperature.isRainingAt(player.blockPosition(), world)) {
                        // Decrement by 5C
                        envtemp -= FHConfig.SERVER.snowTempModifier.get();
                        if (world.isThundering()) {
                            // Decrement by 10C
                            envtemp -= FHConfig.SERVER.blizzardTempModifier.get();
                        }
                        // Due to wetness, daily day-night amplitude shrinks
                        weatherMultiplier = 0.2F;
                    }

                    // Apply day-night amplitude modification
                    // This shift ranges [-10, 10]
                    envtemp += relativeTime * FHConfig.SERVER.dayNightTempAmplitude.get() * weatherMultiplier;

                    // Burning temperature
                    // This shift ranges [150, 150]
                    if (player.isOnFire())
                        envtemp += FHConfig.SERVER.onFireTempModifier.get();

                    // Apply the calculated environment temperature to player attribute
                    AttributeInstance envTempAttribute = player.getAttribute(FHAttributes.ENV_TEMPERATURE.get());
                    if (envTempAttribute != null) {
                        envTempAttribute.removeModifier(ENV_TEMP_ATTRIBUTE_UUID);
                        envTempAttribute.addTransientModifier(new AttributeModifier(ENV_TEMP_ATTRIBUTE_UUID, "player environment modifier", envtemp, Operation.ADDITION));
                    }

                    /* ENVIRONMENT TEMPERATURE COMPUTATION ENDS */

                    /* BODY TEMPERATURE CHANGE COMPUTATION STARTS */

                    // Fetch the current environment temperature from player attribute
                    envtemp = (float) player.getAttributeValue(FHAttributes.ENV_TEMPERATURE.get());
                    float totalConductivity = 0.0f;
                    // If the player has the insulation effect, insulation is set to 1,
                    // so no heat exchange with the environment
                    // Also disable when player is invulnerable
                    // Temporary storage context handled in each update cycle
                    HeatingDeviceContext ctx = new HeatingDeviceContext(player);
                    if (!player.hasEffect(FHMobEffects.INSULATION.get()) && !player.getAbilities().invulnerable) {

                        // Environment-Body Exchange
                        for (BodyPart part : PlayerTemperatureData.BodyPart.values()) {
                            // ranges [0, 1]
                            float partConductivity = data.getThermalConductivityByPart(player, part);
                            // all part areas add up to 100%
                            totalConductivity += part.area * partConductivity;
                            // This is a body part's "Body Temperature" from last time
                            float partBodyTemp = data.getTemperatureByPart(part);
                            // Body ends have a 5C additional effect
                            float partEnvTemp = envtemp - (part.isBodyEnd() ? 5 : 0);
                            // Env and Body exchanges temperature
                            float partBodyEnvExchangeTemp = (partEnvTemp - partBodyTemp) * partConductivity;
                            float partEffectiveTemp = partBodyTemp + partBodyEnvExchangeTemp;
                            // Store them in context
                            ctx.setPartData(part, partBodyTemp, partEffectiveTemp);
                        }

                        // Equipment Heating
                        // Curios slots
                        if (CompatModule.isCuriosLoaded())
                            for (Pair<ISlotType, ItemStack> i : CuriosCompat.getAllCuriosAndSlotsIfVisible(player)) {
                                HeatingDeviceSlot slot = new HeatingDeviceSlot(i.getFirst());
                                LazyOptional<BodyHeatingCapability> cap = FHCapabilities.EQUIPMENT_HEATING.getCapability(i.getSecond());
                                if (cap.isPresent()) {
                                    BodyHeatingCapability eq = cap.resolve().get();
                                    eq.tickHeating(slot, i.getSecond(), ctx);
                                }
                            }
                        // Equipment slots
                        for (EquipmentSlot eslot : EquipmentSlot.values()) {
                            HeatingDeviceSlot slot = new HeatingDeviceSlot(eslot);
                            ItemStack item = player.getItemBySlot(eslot);
                            LazyOptional<BodyHeatingCapability> cap = FHCapabilities.EQUIPMENT_HEATING.getCapability(item);
                            if (cap.isPresent()) {
                                BodyHeatingCapability eq = cap.resolve().get();
                                eq.tickHeating(slot, item, ctx);
                            }
                        }

                        // Apply Exchanged Temperature, and Self-Heating
                        // Temporary storage map
                        FastEnumMap<BodyPart, Float> fem = new FastEnumMap<>(BodyPart.values());
                        for (BodyPart part : BodyPart.values()) {
                            // Apply effective heat exchange to part temperature
                            BodyPartContext pctx = ctx.getPartData(part);
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

                            // May be negative! (when dt < 0)
                            float heatExchangedUnits = (float) (unit * (dt / FHConfig.SERVER.heatExchangeTempConstant.get()));

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
                                movementHeatedUnits += 1 * selfHeatRate *unit;
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

//                            FHMain.LOGGER.debug("Deviation: " + deviation);
//                            FHMain.LOGGER.debug("Homeostasis: " + homeostasisUnits);
//                            FHMain.LOGGER.debug("Movement: " + movementHeatedUnits);
//                            FHMain.LOGGER.debug("Exchange: " + heatExchangedUnits);

                            // Apply all to pbTemp
                            pbTemp += heatExchangedUnits + movementHeatedUnits + homeostasisUnits;

                            // FHMain.LOGGER.debug("pbTemp: " + pbTemp);

                            // base generation when cold: 1 unit
                            /*
                            Let's compute here:
                            Max foodLevel = 20, saturationLevel = 5
                            When exhaustion accumulates to 4 it:
                             decrements saturation;
                             when empty, food by 1
                             and exhaustion is cleared.
                            Every 20 ticks (1 sec) this logic is invoked.
                            When FOOD_EXHAUST_COLD = 0.05, this means
                            4 / 0.05 = 80 seconds for one food level drop
                             */

                            // TODO: degree I/II/III burn if dt=+20/+30/+40
                            // TODO: degree I/II/III freeze if dt=-50/-60/-70
                            fem.put(part, pbTemp);
                        }

                        // Calculate heat transfer between each part
                        //From leg/chest/head share temperature.
                        float coreTemp = 0;
                        for (BodyPart corePart : BodyPart.CoreParts) {
                            coreTemp += fem.get(corePart) * corePart.affectsCore;
                        }
                        for (BodyPart corePart : BodyPart.CoreParts)
                            fem.put(corePart, coreTemp);

                        //From leg to feets
                        final float transferRate = 0.1F;
                        final float maxDelta = 3F;
                        final float minDelta = 0.1F;

                        float dlegfeet = Mth.clamp(fem.get(BodyPart.LEGS) - fem.get(BodyPart.FEET), -maxDelta, maxDelta);
                        if (Mth.abs(dlegfeet) > minDelta) {
                            float newfeet = fem.get(BodyPart.FEET) + dlegfeet * transferRate;
                            float newleg = fem.get(BodyPart.LEGS) - dlegfeet * transferRate;
                            fem.put(BodyPart.FEET, newfeet);
                            fem.put(BodyPart.LEGS, newleg);
                        }

                        //from chest to hands
                        float dhandchest = Mth.clamp(fem.get(BodyPart.TORSO) - fem.get(BodyPart.HANDS), -maxDelta, maxDelta);
                        if (Mth.abs(dhandchest) > minDelta) {
                            float newhands = fem.get(BodyPart.HANDS) + dhandchest * transferRate;
                            float newtorso = fem.get(BodyPart.TORSO) - dhandchest * transferRate;
                            fem.put(BodyPart.HANDS, newhands);
                            fem.put(BodyPart.TORSO, newtorso);
                        }

                        // Recycle the FEM, set back to context
                        for (BodyPart part : BodyPart.values()) {
                            ctx.setBodyTemperature(part, fem.get(part));
                        }

                        // Update data and do the relevant display purpose computation there
                        data.update(envtemp, ctx);

                    }
                    // Apply insulation effect. Above 100 amplifiers are treated as 100.
                    else {
                        MobEffectInstance insulationEffect = player.getEffect(FHMobEffects.INSULATION.get());
                        if (insulationEffect != null) {
                            totalConductivity = Mth.clamp(insulationEffect.getAmplifier(), 0, 100) / 100.0f;
                        }

                        data.updateWhenInsulated(envtemp, totalConductivity);
                    }

                }

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
