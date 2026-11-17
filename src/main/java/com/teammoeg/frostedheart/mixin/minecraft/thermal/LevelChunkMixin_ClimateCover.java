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

package com.teammoeg.frostedheart.mixin.minecraft.thermal;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.teammoeg.frostedheart.content.climate.ClimateCover;
import com.teammoeg.frostedheart.content.climate.ClimateCoverChunk;
import com.teammoeg.frostedheart.content.climate.ClimateCoverColumns;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/** Keeps the lazily built climate cover of a chunk in step with opacity changes in its columns. */
@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin_ClimateCover implements ClimateCoverChunk {
    @Unique
    private ClimateCoverColumns frostedheart$climateCover;

    @Override
    public ClimateCoverColumns frostedheart$getClimateCover() {
        ClimateCoverColumns cover = frostedheart$climateCover;
        if (cover == null) frostedheart$climateCover = cover = new ClimateCoverColumns();
        return cover;
    }

    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void frostedheart$updateClimateCover(
            BlockPos position, BlockState state, boolean moving,
            CallbackInfoReturnable<BlockState> callback) {
        ClimateCoverColumns cover = frostedheart$climateCover;
        BlockState previous = callback.getReturnValue();
        if (cover == null || previous == null || previous == state) return;
        LevelChunk chunk = (LevelChunk) (Object) this;
        boolean coverChanged = previous.isSolidRender(chunk, position) != state.isSolidRender(chunk, position)
                && cover.changed(position.getX(), position.getY(), position.getZ());
        if (chunk.getLevel() instanceof ServerLevel server) {
            if (coverChanged) MinecraftThermalInput.onClimateCoverChanged(server, position.getX(), position.getZ());
        } else {
            // Client values derived from this chunk's cover, such as infrared natural temperatures, follow any change.
            ClimateCover.ClientBlockChanges listener = ClimateCover.clientBlockChanges;
            if (listener != null) listener.changed(position.getX(), position.getY(), position.getZ(), coverChanged);
        }
    }
}
