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

package com.teammoeg.frostedheart.mixin.client;

import com.teammoeg.frostedheart.content.climate.render.sky.LightPillarWorldChanges;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@OnlyIn(Dist.CLIENT)
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin_LightPillars {
    /**
     * Level.markAndNotifyBlock calls this for every block state change, from server packets and the player's own.
     */
    @Inject(method = "setBlocksDirty", at = @At("HEAD"), require = 0)
    private void frostedheart$noteBlockChange(BlockPos pos, BlockState oldState, BlockState newState,
            CallbackInfo callbackInfo) {
        LightPillarWorldChanges.onBlockChanged((ClientLevel) (Object) this, pos, oldState, newState);
    }
}
