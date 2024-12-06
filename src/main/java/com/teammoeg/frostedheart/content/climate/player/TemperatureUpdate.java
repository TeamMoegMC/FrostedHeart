/*
 * Copyright (c) 2021-2024 TeamMoeg
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

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.teammoeg.frostedheart.*;
import com.teammoeg.frostedheart.FHMobEffects;
import com.teammoeg.frostedheart.compat.CuriosCompat;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata.FHBodyDataSyncPacket;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.util.FHUtils;

import com.teammoeg.frostedheart.content.climate.FHTemperatureDifficulty;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
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
import top.theillusivec4.curios.api.type.ISlotType;

public class TemperatureUpdate {
    public static final Double HEAT_EXCHANGE_CONSTANT = FHConfig.SERVER.heatExchangeConstant.get();
    public static final UUID envTempId = UUID.fromString("95c1eab4-8f3a-4878-aaa7-a86722cdfb07");
    public static final int TEMP_SKY_LIGHT_THRESHOLD = FHConfig.SERVER.tempSkyLightThreshold.get();
    public static final int SNOW_TEMP_MODIFIER = FHConfig.SERVER.snowTempModifier.get();
    public static final int BLIZZARD_TEMP_MODIFIER = FHConfig.SERVER.blizzardTempModifier.get();
    public static final int DAY_NIGHT_TEMP_AMPLITUDE = FHConfig.SERVER.dayNightTempAmplitude.get();
    public static final int ON_FIRE_TEMP_MODIFIER = FHConfig.SERVER.onFireTempModifier.get();
    public static final double HURTING_HEAT_UPDATE = FHConfig.SERVER.hurtingHeatUpdate.get();
    public static final int MIN_BODY_TEMP_CHANGE = FHConfig.SERVER.minBodyTempChange.get();
    public static final int MAX_BODY_TEMP_CHANGE = FHConfig.SERVER.maxBodyTempChange.get();


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
                                player.addEffect(FHUtils.noHeal(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0)));
                            } else if (calculatedTarget <= 3) {
                                player.addEffect(new MobEffectInstance(FHMobEffects.HYPERTHERMIA.get(), 100, 1));
                                player.addEffect(FHUtils.noHeal(new MobEffectInstance(MobEffects.CONFUSION, 100, 2)));
                                player.addEffect(FHUtils.noHeal(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0)));
                            } else if (calculatedTarget <= 5) {
                                player.addEffect(new MobEffectInstance(FHMobEffects.HYPERTHERMIA.get(), 100, 2));
                                player.addEffect(FHUtils.noHeal(new MobEffectInstance(MobEffects.CONFUSION, 100, 2)));
                                player.addEffect(FHUtils.noHeal(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0)));
                            } else {
                                player.addEffect(
                                        new MobEffectInstance(FHMobEffects.HYPERTHERMIA.get(), 100, (int) (calculatedTarget - 2)));
                                player.addEffect(FHUtils.noHeal(new MobEffectInstance(MobEffects.CONFUSION, 100, 2)));
                                player.addEffect(FHUtils.noHeal(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0)));
                            }
                        } else { // too cold
                            if (calculatedTarget >= -2) {
                                player.addEffect(new MobEffectInstance(FHMobEffects.HYPOTHERMIA.get(), 100, 0));
                                player.addEffect(FHUtils.noHeal(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0)));
                            } else if (calculatedTarget >= -3) {
                                player.addEffect(new MobEffectInstance(FHMobEffects.HYPOTHERMIA.get(), 100, 1));
                                player.addEffect(FHUtils.noHeal(new MobEffectInstance(MobEffects.CONFUSION, 100, 2)));
                                player.addEffect(FHUtils.noHeal(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0)));
                            } else if (calculatedTarget >= -5) {
                                player.addEffect(new MobEffectInstance(FHMobEffects.HYPOTHERMIA.get(), 100, 2));
                                player.addEffect(FHUtils.noHeal(new MobEffectInstance(MobEffects.CONFUSION, 100, 2)));
                                player.addEffect(FHUtils.noHeal(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0)));
                            } else {
                                player.addEffect(
                                        new MobEffectInstance(FHMobEffects.HYPOTHERMIA.get(), 100, (int) (-calculatedTarget - 2)));
                                player.addEffect(FHUtils.noHeal(new MobEffectInstance(MobEffects.CONFUSION, 100, 2)));
                                player.addEffect(FHUtils.noHeal(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0)));
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
                            player.addEffect(new MobEffectInstance(FHMobEffects.WET.get(), FHConfig.SERVER.wetEffectDuration.get() * FHConfig.SERVER.wetClothesDurationMultiplier.get(), 0));// punish for wet clothes
                        else if (current == null || current.getDuration() < FHConfig.SERVER.wetEffectDuration.get())
                            player.addEffect(new MobEffectInstance(FHMobEffects.WET.get(), FHConfig.SERVER.wetEffectDuration.get(), 0));
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
                    if (skyLight < TEMP_SKY_LIGHT_THRESHOLD) {
                        relativeTime = -1;
                    }

                    // Weather temperature modifier
                    float weatherMultiplier = 1.0F;
                    if (world.isRaining() && WorldTemperature.isRainingAt(player.blockPosition(), world)) {
                        envtemp -= SNOW_TEMP_MODIFIER;
                        if (world.isThundering()) {
                            envtemp -= BLIZZARD_TEMP_MODIFIER;
                        }
                        weatherMultiplier = 0.2F;
                    }

                    envtemp += relativeTime * DAY_NIGHT_TEMP_AMPLITUDE * weatherMultiplier;

                    // Burning temperature
                    if (player.isOnFire())
                        envtemp += ON_FIRE_TEMP_MODIFIER;

                    // Handle Attributes
                    player.getAttribute(FHAttributes.ENV_TEMPERATURE.get()).removeModifier(envTempId);
                    player.getAttribute(FHAttributes.ENV_TEMPERATURE.get()).addTransientModifier(new AttributeModifier(envTempId, "player environment modifier", envtemp, Operation.ADDITION));

                    /* Effective & Body Temperature */

                    // Current body temperature
                    float current = PlayerTemperatureData.getCapability(event.player).map(PlayerTemperatureData::getBodyTemp).orElse(0f);

                    // Self heating
                    if (current < 0) {
                        // Fetch the self heating function from the difficulty
                        float delt = (float) (PlayerTemperatureData.getCapability(event.player).map(PlayerTemperatureData::getDifficulty).orElse(FHTemperatureDifficulty.normal).self_heat.apply(player) * tspeed);
                        // float delt = (float) (FHConfig.SERVER.tdiffculty.get().self_heat.apply(player) * tspeed);
                        player.causeFoodExhaustion(Math.min(delt, -current) * 0.5f);//cost hunger for cold.
                        current += delt;
                    }

                    // Insulation
                    envtemp=(float) player.getAttributeValue(FHAttributes.ENV_TEMPERATURE.get());
                    // float insulation = (float) player.getAttributeValue(FHAttributes.INSULATION.get());
                    PlayerTemperatureData.BodyPart[] parts = {
                            PlayerTemperatureData.BodyPart.HEAD,
                            PlayerTemperatureData.BodyPart.BODY,
                            PlayerTemperatureData.BodyPart.HANDS,
                            PlayerTemperatureData.BodyPart.LEGS,
                            PlayerTemperatureData.BodyPart.FEET
                    };
                    float[] ratios = {0.1f, 0.4f, 0.05f, 0.4f, 0.05f};
                    float insulation = 1;
                    // If the player has the insulation effect, insulation is set to 1, so no heat exchange with the environment
                    if (!player.hasEffect(FHMobEffects.INSULATION.get())) {
                        for (int i = 0; i < 5; ++i) {
                            PlayerTemperatureData.BodyPart part = parts[i];
                            float ratio = ratios[i];
                            float thermalConductivity = data.getThermalConductivityByPart(part);
                            insulation -= ratio * thermalConductivity;
                            float temperature = data.getTemperatureByPart(part);
                            float dt = (temperature - envtemp) * thermalConductivity;
                            temperature -= 0.001f * 6 * dt; // 10 dt -> temperature change 0.06 per tick
                            if (temperature < 0.0) {
                                temperature += 0.09f;
                            } else {
                                temperature += 0.06f;
                            }
                            data.setTemperatureByPart(part, temperature);
                        }
                    } else {
                        MobEffectInstance insulationEffect = player.getEffect(FHMobEffects.INSULATION.get());
                        if (insulationEffect != null) {
                            int amp = insulationEffect.getAmplifier();
                            // clamp to 0 to 100
                            amp = Mth.clamp(amp, 0, 100);
                            insulation = 1 - amp / 100.0f;
                        }
                    }

                    float efftemp = current - (1 - insulation) * (current - envtemp); //Effective temperature, 37-based


                    // Equipments
                    for (Pair<ISlotType, ItemStack> is : CuriosCompat.getAllCuriosAndSlotsIfVisible(player)) {
                        if (is == null)
                            continue;
                        Item it = is.getSecond().getItem();
                        if (it instanceof IHeatingEquipment)
                            efftemp += ((IHeatingEquipment) it).getEffectiveTempAdded(Either.left(is.getFirst()), is.getSecond(), efftemp, current);
                    }
                    for (EquipmentSlot slot : EquipmentSlot.values()) {
                        ItemStack is = player.getItemBySlot(slot);
                        if (is.isEmpty())
                            continue;
                        Item it = is.getItem();
                        if (it instanceof IHeatingEquipment) {
                            if (it instanceof IHeatingEquipment)
                                efftemp += ((IHeatingEquipment) it).getEffectiveTempAdded(Either.right(EquipmentSlotType.fromVanilla(slot)), is, efftemp, current);
                        }
                    /*if (it instanceof IWarmKeepingEquipment) {
                        keepwarm += ((IWarmKeepingEquipment) it).getFactor(player, is);
                    } else {//include inner
                        String s = ItemNBTHelper.getString(is, "inner_cover");
                        IWarmKeepingEquipment iw = null;
                        EquipmentSlotType aes = MobEntity.getSlotForItemStack(is);
                        if (s.length() > 0 && aes != null) {
                            iw = FHDataManager.getArmor(s + "_" + aes.getName());
                        } else
                            iw = FHDataManager.getArmor(is);
                        if (iw != null)
                            keepwarm += iw.getFactor(player, is);
                    }*/
                    }

                    /* Heat exchange section */

                    //environment heat exchange
                    float dheat = (float) (HEAT_EXCHANGE_CONSTANT * (efftemp - current));
                    //Attack player if temperature changes too much
                    if (dheat > HURTING_HEAT_UPDATE)
                        player.hurt(FHDamageTypes.createSource(world, FHDamageTypes.HYPERTHERMIA_INSTANT, player), (dheat) * 10);
                    else if (dheat < -HURTING_HEAT_UPDATE)
                        player.hurt(FHDamageTypes.createSource(world, FHDamageTypes.HYPOTHERMIA_INSTANT, player), (-dheat) * 10);
                    if (!player.isCreative() && !player.isSpectator())//no modify body temp when creative or spectator
                        current += (float) (dheat * tspeed);
                    if (current < MIN_BODY_TEMP_CHANGE)
                        current = MIN_BODY_TEMP_CHANGE;
                    else if (current > MAX_BODY_TEMP_CHANGE)
                        current = MAX_BODY_TEMP_CHANGE;
                    float lenvtemp = data.getEnvTemp();//get a smooth change in display
                    float lfeeltemp = data.getFeelTemp();
                    data.update(current, (envtemp + 37F) * .2f + lenvtemp * .8f, (efftemp + 37F) * .2f + lfeeltemp * .8f);
                    //FHNetwork.send(PacketDistributor.PLAYER.with(() -> player), new FHBodyDataSyncPacket(player));
                }

                FHNetwork.sendPlayer(player, new FHBodyDataSyncPacket(player));
            });
        }
    }
}
