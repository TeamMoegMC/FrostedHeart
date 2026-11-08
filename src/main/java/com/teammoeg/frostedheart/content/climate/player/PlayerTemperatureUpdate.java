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

import com.teammoeg.chorda.CompatModule;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.common.FHFluids;
import com.teammoeg.frostedheart.bootstrap.common.FHMobEffects;
import com.teammoeg.frostedheart.compat.curios.CuriosCompat;
import com.teammoeg.frostedheart.content.climate.network.FHBodyDataSyncPacket;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData.BodyPart;
import com.teammoeg.frostedheart.content.climate.player.thermalitem.WearableThermalExchangeHandler;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.LogicalSide;

public class PlayerTemperatureUpdate {
    private static final WearableThermalExchangeHandler
            WEARABLE_THERMAL_EXCHANGE_HANDLER =
            new WearableThermalExchangeHandler();

    /**
     * Perform temperature effect
     *
     * @param event fired every tick on player
     */
    public static void regulateTemperature(PlayerTickEvent event) {
        var stageObserver = com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics.observer;
        boolean observeStage = stageObserver != null && event.side == LogicalSide.SERVER && event.phase == Phase.END;
        if (observeStage) stageObserver.stage("player_thermal_regulation", true);
        try {
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
                    if (isSoaked(player)) {
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

                    double torso = data.getBodyTempByPart(BodyPart.TORSO);
                    updateBodyEffect(player, FHMobEffects.HYPERTHERMIA.get(),
                            torso > 1.0D ? severity(torso) : -1);
                    updateBodyEffect(player, FHMobEffects.HYPOTHERMIA.get(),
                            torso < -1.0D ? severity(-torso) : -1);

                    applyPartSymptom(player, MobEffects.CONFUSION,
                            Math.abs(data.getBodyTempByPart(BodyPart.HEAD)));
                    applyPartSymptom(player, MobEffects.MOVEMENT_SLOWDOWN,
                            Math.max(Math.abs(data.getBodyTempByPart(BodyPart.FEET)),
                                    Math.abs(data.getBodyTempByPart(BodyPart.LEGS))));
                    applyPartSymptom(player, MobEffects.DIG_SLOWDOWN,
                            Math.abs(data.getBodyTempByPart(BodyPart.HANDS)));

                    // Severe actual medium exposure is separate from accumulated body illness.
                    if (!player.hasEffect(FHMobEffects.INSULATION.get())) {
                        PlayerThermalInjury.apply(player, data, temperatureUpdateIntervalTicks / 20.0D);
                    }

                });
            }
        } finally { if (observeStage) stageObserver.stage("player_thermal_regulation", false); }

    }

    /**
     * Perform temperature tick logic
     * <p>
     * Updated on the configured player-temperature cadence.
     *
     * @param event fired every tick on player
     */
    public static void updateTemperature(PlayerTickEvent event) {
        var stageObserver = com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics.observer;
        boolean observeStage = stageObserver != null && event.side == LogicalSide.SERVER && event.phase == Phase.START;
        if (observeStage) stageObserver.stage("player_thermal_update", true);
        try {
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
                boolean invulnerable = player.isInvulnerable()
                        || player.getAbilities().invulnerable;
                if (CompatModule.isCuriosLoaded()
                        && WearableThermalExchangeHandler.allowsPlayerExchange(
                                player.isCreative(), player.isSpectator(), invulnerable)) {
                    ItemStack reservoirStack = CuriosCompat
                            .getWearableThermalReservoirInWarmStoneSlot(player);
                    WEARABLE_THERMAL_EXCHANGE_HANDLER.exchangeInto(
                            data,
                            reservoirStack,
                            data.getSampledAirTemperatureC(),
                            intervalTicks / 20.0D);
                }
                if (data.shouldSyncThermalState()) {
                    FHNetwork.INSTANCE.sendPlayer(
                            player, new FHBodyDataSyncPacket(player));
                }
            }
        } finally { if (observeStage) stageObserver.stage("player_thermal_update", false); }

    }

    private static int severity(double offsetMagnitude) {
        return offsetMagnitude <= 1.0D ? -1 : offsetMagnitude <= 2.0D ? 0
                : offsetMagnitude <= 3.0D ? 1 : offsetMagnitude <= 5.0D ? 2
                : (int) (offsetMagnitude - 2.0D);
    }

    private static void updateBodyEffect(ServerPlayer player,
                                         MobEffect effect, int level) {
        MobEffectInstance current = player.getEffect(effect);
        if (level < 0) {
            if (current != null) player.removeEffect(effect);
        } else if (current == null || current.getAmplifier() != level || !current.isInfiniteDuration()) {
            // These two effects belong to body temperature and end when the body recovers.
            player.removeEffect(effect);
            player.addEffect(new MobEffectInstance(effect, -1, level));
        }
    }

    private static void applyPartSymptom(ServerPlayer player,
                                         MobEffect effect, double offsetMagnitude) {
        int level = severity(offsetMagnitude);
        // Vanilla effects can have other owners; retain their existing expiry/ownership.
        if (level >= 0 && !player.hasEffect(effect)) {
            player.addEffect(CUtils.noHeal(new MobEffectInstance(effect, 100, Math.min(3, level))));
        }
    }

    static boolean isSoaked(ServerPlayer player) {
        return player.isInWater()
                || player.getFluidTypeHeight(FHFluids.HOT_SPRING.get().getFluidType()) > 0.0D;
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
