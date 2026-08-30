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

import com.teammoeg.frostedheart.bootstrap.common.FHAttributes;
import com.teammoeg.frostedheart.bootstrap.common.FHMobEffects;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WorldClimate;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.ThermalEnvironmentSample;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.level.material.Fluid;

/** Reads and normalizes physical and entity-local temperature inputs. */
public final class PlayerThermalEnvironment {
    private PlayerThermalEnvironment() {
    }

    static double sampleAirTemperatureC(ServerPlayer player, BlockPos samplePosition, ThermalEnvironmentSample sample) {
        return MinecraftThermalInput.gameplayPlayerEnvironment(
                player, WorldTemperature.naturalAir(player.level(), samplePosition), sample);
    }

    static double localAirTemperatureC(ServerPlayer player, double sampledAirTemperatureC) {
        AttributeInstance attribute = player.getAttribute(FHAttributes.ENV_TEMPERATURE.get());
        double result = sampledAirTemperatureC;
        if (attribute != null) {
            attribute.removeModifier(PlayerTemperatureComputation.ENV_TEMP_ATTRIBUTE_UUID);
            attribute.addTransientModifier(new AttributeModifier(
                    PlayerTemperatureComputation.ENV_TEMP_ATTRIBUTE_UUID, "player environment temperature",
                    sampledAirTemperatureC, AttributeModifier.Operation.ADDITION));
            result = player.getAttributeValue(FHAttributes.ENV_TEMPERATURE.get());
        }
        return player.hasEffect(FHMobEffects.SAUNA.get())
                ? Math.max(result, 80.0D) : result;
    }

    static double outdoorWindMPerS(ServerPlayer player) {
        return PlayerThermalModel.outdoorWindMPerS(WorldTemperature.wind(player.level()));
    }

    static double relativeHumidity(ServerPlayer player) {
        return PlayerThermalModel.clamp(WorldClimate.getHumidity(player.level()) / 50.0D, 0.0D, 1.0D);
    }

    static double fluidHeightRatio(ServerPlayer player, TagKey<Fluid> fluid, double bodyHeight) {
        return PlayerThermalModel.clamp(player.getFluidHeight(fluid) / bodyHeight, 0.0D, 1.0D);
    }

    static boolean isBodyFrozen(ServerPlayer player) {
        return player.hasEffect(FHMobEffects.INSULATION.get())
                || player.isCreative()
                || player.isSpectator()
                || player.getAbilities().invulnerable;
    }
}
