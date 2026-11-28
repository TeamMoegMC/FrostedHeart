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

import java.util.ArrayList;
import java.util.List;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.IDisplayAssemblyExceptions;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock;
import com.simibubi.create.content.contraptions.piston.PistonExtensionPoleBlock;
import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.equipment.goggles.IHaveHoveringInformation;
import com.simibubi.create.content.trains.entity.TrainRelocator;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Lang;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

/** Create and Immersive Engineering information adapters; presentation lives in OverlayStyle. */
final class CreateOverlayInformation {
    private CreateOverlayInformation() {}

    /** Per-frame readout and provider flags used by the legacy visibility rule. */
    static final class Readout {
        final List<Component> tooltip = new ArrayList<>();
        final boolean hasGoggleInformation;
        boolean hasHoveringInformation;
        boolean goggleAddedInformation;
        boolean hoverAddedInformation;

        Readout(GoggleOverlayContext context) {
            hasGoggleInformation = context.blockEntity() instanceof IHaveGoggleInformation;
            hasHoveringInformation = context.blockEntity() instanceof IHaveHoveringInformation;
        }

        boolean suppresses(boolean hasPlantInformation, boolean plantAdded) {
            return hasGoggleInformation && !goggleAddedInformation
                    && hasHoveringInformation && !hoverAddedInformation
                    && hasPlantInformation && !plantAdded;
        }
    }

    /** Collects block/multiblock goggles information followed by block hover information. */
    static void appendGogglesAndHover(GoggleOverlayContext context, Readout readout) {
        boolean shifting = context.minecraft().player.isShiftKeyDown();
        if (context.wearingGoggles()) {
            if (context.blockEntity() instanceof IHaveGoggleInformation info) {
                readout.goggleAddedInformation = info.addToGoggleTooltip(readout.tooltip, shifting);
                info.getIcon(shifting);
            }
            if (context.multiblock() instanceof IHaveGoggleInformation info) {
                readout.goggleAddedInformation = info.addToGoggleTooltip(readout.tooltip, shifting);
                info.getIcon(shifting);
            }
        }
        if (context.blockEntity() instanceof IHaveHoveringInformation info) {
            if (!readout.tooltip.isEmpty()) readout.tooltip.add(Components.immutableEmpty());
            readout.hoverAddedInformation = info.addToTooltip(readout.tooltip, shifting);
            if (readout.goggleAddedInformation && !readout.hoverAddedInformation)
                readout.tooltip.remove(readout.tooltip.size() - 1);
        }
    }

    /** Appends assembly errors and falls back to the train relocation readout. */
    static boolean appendAssemblyAndTrain(GoggleOverlayContext context, Readout readout) {
        if (context.blockEntity() instanceof IDisplayAssemblyExceptions info
                && info.addExceptionToTooltip(readout.tooltip)) {
            readout.hasHoveringInformation = true;
            readout.hoverAddedInformation = true;
        }
        if (readout.hasHoveringInformation) return false;
        boolean trainAdded = TrainRelocator.addToTooltip(readout.tooltip,
                context.minecraft().player.isShiftKeyDown());
        readout.hasHoveringInformation = trainAdded;
        readout.hoverAddedInformation = trainAdded;
        return trainAdded;
    }

    /** Appends piston length; false suppresses orphan extension poles as before. */
    static boolean appendPiston(GoggleOverlayContext context, List<Component> tooltip) {
        var state = context.state();
        if (!context.wearingGoggles() || !AllBlocks.PISTON_EXTENSION_POLE.has(state)) return true;
        var world = context.level();
        var pos = context.position();
        Direction[] directions = Iterate.directionsInAxis(state.getValue(PistonExtensionPoleBlock.FACING).getAxis());
        int poles = 1;
        boolean pistonFound = false;
        for (Direction direction : directions) {
            int attached = PistonExtensionPoleBlock.PlacementHelper.get().attachedPoles(world, pos, direction);
            poles += attached;
            pistonFound |= world.getBlockState(pos.relative(direction, attached + 1))
                    .getBlock() instanceof MechanicalPistonBlock;
        }
        if (!pistonFound) return false;
        if (!tooltip.isEmpty()) tooltip.add(Components.immutableEmpty());
        Lang.translate("gui.goggles.pole_length").text(" " + poles).forGoggles(tooltip);
        return true;
    }
}
