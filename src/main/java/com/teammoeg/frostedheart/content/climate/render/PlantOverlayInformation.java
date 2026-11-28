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

package com.teammoeg.frostedheart.content.climate.render;

import java.util.List;
import com.simibubi.create.foundation.utility.Components;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.climate.network.SoilThermometerRequestPacket;
import com.teammoeg.frostedheart.content.climate.tooltips.PlantTempStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** Owns plant readout requests, target lifetime and server-authoritative cached data. */
final class PlantOverlayInformation {
    private BlockPos cropTarget;
    private java.lang.ref.WeakReference<ClientLevel> cropLevel = new java.lang.ref.WeakReference<>(null);
    private long lastCropRequest;

    private float cachedTemperature = Float.NaN;
    private boolean cropExposedToBadWeather;
    private Component cachedPlantInfo;
    private com.teammoeg.frostedheart.content.climate.network.SoilThermometerUpdatePacket.TreeInfo cachedTreeInfo;
    /** Updates the growth readout for the currently inspected plant. */
    public void receivePlantReadout(BlockPos position, float temperature, boolean exposedToBadWeather,
            @org.jetbrains.annotations.Nullable Component plantInfo,
            @org.jetbrains.annotations.Nullable com.teammoeg.frostedheart.content.climate.network.SoilThermometerUpdatePacket.TreeInfo treeInfo) {
        if (position.equals(cropTarget) && Minecraft.getInstance().level == cropLevel.get()) {
            cachedTemperature = temperature;
            cropExposedToBadWeather = exposedToBadWeather;
            cachedPlantInfo = plantInfo;
            cachedTreeInfo = treeInfo;
        }
    }

    /** Stops tracking plants when the crosshair no longer hits a block. */
    void clearTarget() { cropTarget = null; }

    /** Refreshes plant data and appends the available readout, preserving section spacing. */
    boolean append(GoggleOverlayContext context, List<Component> tooltip) {
        updateTarget(context);
        List<Component> stats = readout(context);
        if (stats.isEmpty()) return false;
        if (!tooltip.isEmpty()) tooltip.add(Components.immutableEmpty());
        tooltip.addAll(stats);
        return true;
    }

    private void updateTarget(GoggleOverlayContext context) {
        if (!context.wearingSoilThermometer() || !context.hasPlantInformation()) {
            clearTarget();
            return;
        }
        var world = context.level();
        var pos = context.position();
        long tick = world.getGameTime();
        boolean changed = world != cropLevel.get() || !pos.equals(cropTarget);
        if (!changed && tick >= lastCropRequest && tick - lastCropRequest < 20) return;
        if (changed) {
            cachedTemperature = Float.NaN;
            cachedPlantInfo = null;
            cachedTreeInfo = null;
        }
        cropTarget = pos.immutable();
        if (world != cropLevel.get()) cropLevel = new java.lang.ref.WeakReference<>(world);
        lastCropRequest = tick;
        FHNetwork.INSTANCE.sendToServer(new SoilThermometerRequestPacket(cropTarget));
    }

    private List<Component> readout(GoggleOverlayContext context) {
        if (cropTarget == null) return List.of();
        if (context.cultivatedSapling() && cachedPlantInfo != null) {
            return com.teammoeg.frostedheart.content.climate.tooltips.CultivatedTreeTempStats.getStats(
                    cachedTreeInfo, cachedPlantInfo, context.level().getBlockState(context.position().below()), cachedTemperature);
        }
        if (!context.cultivatedSapling() && cachedPlantInfo == null && Float.isFinite(cachedTemperature)) {
            return PlantTempStats.getStats(context.state().getBlock(), context.minecraft().player,
                    context.position(), cachedTemperature, cropExposedToBadWeather);
        }
        return List.of();
    }
}
