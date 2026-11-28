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

import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockBE;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockState;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import com.teammoeg.frostedheart.content.agriculture.tree.CultivatedOakBlock;
import com.teammoeg.frostedheart.content.climate.data.PlantTempData;
import com.teammoeg.frostedheart.content.utility.SoilThermometer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Shared target and equipment snapshot for information providers during one frame. */
public record GoggleOverlayContext(Minecraft minecraft, ClientLevel level, BlockPos position,
        BlockState state, BlockEntity blockEntity, IMultiblockState multiblock,
        boolean wearingGoggles, boolean wearingSoilThermometer, boolean cultivatedSapling,
        boolean hasPlantInformation) {
    /** Resolves the proxy target once and snapshots the equipment and multiblock state. */
    public static GoggleOverlayContext at(Minecraft mc, BlockPos target) {
        BlockPos position = TemperatureGoogleRenderer.proxiedOverlayPosition(mc.level, target);
        BlockState state = mc.level.getBlockState(position);
        BlockEntity entity = mc.level.getBlockEntity(position);
        IMultiblockState multiblock = entity instanceof IMultiblockBE be ? be.getHelper().getState() : null;
        boolean sapling = state.getBlock() instanceof CultivatedOakBlock;
        return new GoggleOverlayContext(mc, mc.level, position, state, entity, multiblock,
                GogglesItem.isWearingGoggles(mc.player), SoilThermometer.isWearingSoilThermometer(mc.player),
                sapling, PlantTempData.cacheList.get(state.getBlock()) != null || sapling);
    }
}
