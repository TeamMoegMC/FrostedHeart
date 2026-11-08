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

import com.teammoeg.frostedheart.bootstrap.reference.FHDamageSources;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData.BodyPart;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;

/** Direct severe-medium exposure; radiant watts affect body energy, never this temperature. */
public final class PlayerThermalInjury {
    private PlayerThermalInjury() {
    }

    static void apply(ServerPlayer player, PlayerTemperatureData data, double elapsedSeconds) {
        if (player.isOnFire() || player.isInLava()) return;
        double heatFrequency = 0.0D, coldFrequency = 0.0D;
        float heatDamage = 0.0F, coldDamage = 0.0F;
        for (BodyPart part : BodyPart.VALUES) {
            if (part == BodyPart.HANDS) continue;
            double temperature = data.getExposureTemperatureC(part);
            double magnitude = Math.abs(temperature);
            float damage = magnitude > 250.0D ? 4.0F : magnitude > 200.0D ? 3.0F
                    : magnitude > 150.0D ? 2.0F : magnitude > 100.0D ? 1.0F : 0.0F;
            double frequency = (damage == 4.0F ? 1.0D : damage == 3.0F ? 0.75D
                    : damage == 2.0F ? 0.5D : damage == 1.0F ? 0.25D : 0.0D)
                    * data.getExposureTransmission(part);
            if (temperature > 0.0D && damage * frequency > heatDamage * heatFrequency) {
                heatDamage = damage;
                heatFrequency = frequency;
            } else if (temperature < 0.0D && damage * frequency > coldDamage * coldFrequency) {
                coldDamage = damage;
                coldFrequency = frequency;
            }
        }
        if (heatDamage > 0.0F) applyExposure(player, FHDamageSources.hyperthermiaInstant(player.level()),
                heatDamage, heatFrequency * elapsedSeconds);
        if (coldDamage > 0.0F) applyExposure(player, FHDamageSources.hypothermiaInstant(player.level()),
                coldDamage, coldFrequency * elapsedSeconds);
    }

    private static void applyExposure(ServerPlayer player, DamageSource source, float damage, double expectedHits) {
        if (!(expectedHits > 0.0D)) return;
        double hits = Math.floor(expectedHits);
        RandomSource random = player.getRandom();
        if (random.nextDouble() < expectedHits - hits) hits++;
        if (hits > 0.0D) player.hurt(source, (float) (damage * hits));
    }
}
