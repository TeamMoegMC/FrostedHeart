/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */
package com.teammoeg.frostedheart.content.climate.player;

import com.teammoeg.frostedheart.bootstrap.reference.FHDamageSources;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

/** Applies the existing direct climate injury thresholds and probabilities. */
public final class PlayerThermalInjury {
    private PlayerThermalInjury() {
    }

    static void apply(ServerPlayer player, PlayerTemperatureData data) {
        if (player.isOnFire() || player.isInLava()) return;
        RandomSource random = player.getRandom();
        applyHeat(player, random, data.getHighestFeelTemp());
        applyCold(player, random, data.getLowestFeelTemp());
    }

    private static void applyHeat(ServerPlayer player, RandomSource random, float hottest) {
        if (hottest > 250.0F) {
            player.hurt(FHDamageSources.hyperthermiaInstant(player.level()), 4.0F);
        } else if (hottest > 200.0F && random.nextFloat() < 0.75F) {
            player.hurt(FHDamageSources.hyperthermiaInstant(player.level()), 3.0F);
        } else if (hottest > 150.0F && random.nextFloat() < 0.5F) {
            player.hurt(FHDamageSources.hyperthermiaInstant(player.level()), 2.0F);
        } else if (hottest > 100.0F && random.nextFloat() < 0.25F) {
            player.hurt(FHDamageSources.hyperthermiaInstant(player.level()), 1.0F);
        }
    }

    private static void applyCold(ServerPlayer player, RandomSource random, float coldest) {
        if (coldest < -250.0F) {
            player.hurt(FHDamageSources.hypothermiaInstant(player.level()), 4.0F);
        } else if (coldest < -200.0F && random.nextFloat() < 0.75F) {
            player.hurt(FHDamageSources.hypothermiaInstant(player.level()), 3.0F);
        } else if (coldest < -150.0F && random.nextFloat() < 0.5F) {
            player.hurt(FHDamageSources.hypothermiaInstant(player.level()), 2.0F);
        } else if (coldest < -100.0F && random.nextFloat() < 0.25F) {
            player.hurt(FHDamageSources.hypothermiaInstant(player.level()), 1.0F);
        }
    }
}
