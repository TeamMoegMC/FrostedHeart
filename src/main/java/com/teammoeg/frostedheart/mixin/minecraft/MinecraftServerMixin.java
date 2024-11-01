/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.mixin.minecraft;

import com.teammoeg.frostedheart.world.FHFeatures;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.MinecraftServer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ServerLevelData;
/**
 * Generates aircraft on start
 * <p>
 * */
@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Inject(at = @At("TAIL"), method = "setInitialSpawn")
    private static void spacecraftGenerate(ServerLevel serverWorld, ServerLevelData info, boolean pGenerateBonusChest, boolean pDebug, CallbackInfo ci) {
        int y = 256, h;
        // store these as temporary variables to reduce procedural calls in loop
        int seaLevel = serverWorld.getSeaLevel();
        int xStart = info.getXSpawn() - 8, xEnd = xStart + 16;
        int zStart = info.getZSpawn() - 8, zEnd = zStart + 16;
        // scan the 16x16 area around the spawn point
        // find the minimum surface point that is above sea level.
        for (int x = xStart; x <= xEnd; x++) {
            for (int z = zStart; z <= zEnd; z++) {
                h = serverWorld.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                if (h >= seaLevel)
                    y = Math.min(h, y);
            }
        }
        // in extreme case, that is a 16x16 valley below sea level around the spawn point,
        // we just generate the spacecraft at sea level.
        // this case should not happen because there is no known features that does so.
        if (y == 256)
            y = seaLevel;
        info.setYSpawn(y - 3);
        FHFeatures.SPACECRAFT.get().place(NoneFeatureConfiguration.INSTANCE, serverWorld, serverWorld.getChunkSource().getGenerator(), serverWorld.random,
                new BlockPos(info.getXSpawn(), info.getYSpawn(), info.getZSpawn()));
        serverWorld.setDefaultSpawnPos(new BlockPos(info.getXSpawn(), info.getYSpawn(), info.getZSpawn()), info.getSpawnAngle());

    }
}
