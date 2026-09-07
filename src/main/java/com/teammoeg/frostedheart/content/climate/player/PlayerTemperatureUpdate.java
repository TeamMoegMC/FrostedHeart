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

import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.common.FHMobEffects;
import com.teammoeg.frostedheart.content.climate.network.FHBodyDataSyncPacket;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData.BodyPart;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.LogicalSide;

public class PlayerTemperatureUpdate {
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

                // ConfigValue.get() 为 spec 值表查询：同一 tick 内配置恒定，hoist 到局部变量
                int temperatureUpdateIntervalTicks = FHConfig.SERVER.CLIMATE.temperatureUpdateIntervalTicks.get();
                if (!shouldUpdatePlayer(
                        player, temperatureUpdateIntervalTicks)) {
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
                    int wetEffectDuration = FHConfig.SERVER.CLIMATE.wetEffectDuration.get();
                    int wetClothesDurationMultiplier = FHConfig.SERVER.CLIMATE.wetClothesDurationMultiplier.get();
                    // If armor is on, player gets a longer wet effect
                    if (hasArmor) {
                        player.addEffect(new MobEffectInstance(FHMobEffects.WET.get(),
                                wetEffectDuration * wetClothesDurationMultiplier,
                                0, false, false));// punish for wet clothes
                    }
                    // Otherwise, if there is no wet effect now, add normal wet effect
                    else if (current == null || current.getDuration() < wetEffectDuration) {
                        player.addEffect(new MobEffectInstance(FHMobEffects.WET.get(),
                                wetEffectDuration, 0, false, false));
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
                if (!player.hasEffect(FHMobEffects.INSULATION.get())) {
                    PlayerTemperatureComputation.burning(player, data);
                }

            });
        }
    }

    /**
     * Perform temperature tick logic
     * <p>
     * Updated on the configured player-temperature cadence.
     *
     * @param event fired every tick on player
     */
    public static void updateTemperature(PlayerTickEvent event) {
        if (event.side == LogicalSide.SERVER
                && event.phase == Phase.START
                && event.player instanceof ServerPlayer player) {
            PlayerTemperatureData data = PlayerTemperatureData
                    .getCapability(player).orElse(null);
            if (data == null) return;
            int intervalTicks = FHConfig.SERVER.CLIMATE
                    .temperatureUpdateIntervalTicks.get();
            if (!shouldUpdatePlayer(player, intervalTicks)) return;

            PlayerTemperatureComputation.updatePlayer(
                    player, data, intervalTicks);
            if (data.shouldSyncThermalState()) {
                FHNetwork.INSTANCE.sendPlayer(
                        player, new FHBodyDataSyncPacket(player));
            }
        }
    }

    private static boolean shouldUpdatePlayer(
            ServerPlayer player,
            int intervalTicks
    ) {
        if (intervalTicks <= 0) return false;
        long mixed = player.getUUID().getMostSignificantBits()
                ^ Long.rotateLeft(
                        player.getUUID().getLeastSignificantBits(), 17);
        return Math.floorMod(player.tickCount, intervalTicks)
                == Math.floorMod(mixed, intervalTicks);
    }

}
