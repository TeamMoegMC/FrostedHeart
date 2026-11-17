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
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.BitSet;
import java.util.Iterator;

@OnlyIn(Dist.CLIENT)
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin_LightPillars {
    @Shadow
    private ClientLevel level;

    /**
     * Light from the server for one layer of a chunk column, from chunk and light update packets alike.
     * It runs from ClientLevel.pollLightUpdates, and the light engine run right after it in the same frame makes the
     * new values readable.
     */
    @Inject(method = "readSectionList", at = @At("HEAD"), require = 0)
    private void frostedheart$noteLightData(int chunkX, int chunkZ, LevelLightEngine lightEngine, LightLayer layer,
            BitSet dataMask, BitSet emptyMask, Iterator<byte[]> data, CallbackInfo callbackInfo) {
        LightPillarWorldChanges.onLightReceived(level, chunkX, chunkZ, layer, dataMask, emptyMask,
                lightEngine.getMinLightSection(), lightEngine.getLightSectionCount());
    }
}
