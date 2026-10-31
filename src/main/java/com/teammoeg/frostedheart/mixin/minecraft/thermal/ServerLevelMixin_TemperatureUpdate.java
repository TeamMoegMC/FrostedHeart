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

import com.teammoeg.frostedheart.content.climate.ClimateBlockUpdates;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The custom climate pass retains the existing post-thunder sampling contract. */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin_TemperatureUpdate {
    @Inject(method = "tickChunk", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V",
            ordinal = 0, shift = At.Shift.BEFORE), cancellable = true)
    private void frostedheart$tickClimate(LevelChunk chunk, int randomTickSpeed, CallbackInfo callback) {
        ClimateBlockUpdates.tickAfterThunder((ServerLevel) (Object) this, chunk, randomTickSpeed);
        callback.cancel();
    }
}
