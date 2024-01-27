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

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.Heightmap.Type;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.IServerWorldInfo;

import java.util.PriorityQueue;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Inject(at = @At("TAIL"), method = "func_240786_a_")
    private static void spacecraftGenerate(ServerWorld serverWorld, IServerWorldInfo info, boolean hasBonusChest, boolean p_240786_3_, boolean p_240786_4_, CallbackInfo ci) {
        int y = 256, h;
        final int yOffset=-1;
        // store these as temporary variables to reduce procedural calls in loop
        int seaLevel = serverWorld.getSeaLevel();
        int xStart = info.getSpawnX() - 8, xEnd = xStart + 16;
        int zStart = info.getSpawnZ() - 8, zEnd = zStart + 16;
        // scan the 16x16 area around the spawn point
        // find the minimum surface point that is above sea level.
        PriorityQueue<Integer> pq=new PriorityQueue<>();
        BlockPos.Mutable bm=new BlockPos.Mutable();
        for (int x = xStart; x <= xEnd; x++) {
        	bm.setX(x);
            for (int z = zStart; z <= zEnd; z++) {
            	bm.setZ(z);
                h = serverWorld.getHeight(Type.MOTION_BLOCKING_NO_LEAVES, x, z);
                
                while(h >= seaLevel) {
                	bm.setY(h);
                	BlockState ss=serverWorld.getBlockState(bm);
                	if(ss.isIn(BlockTags.VALID_SPAWN)&&!ss.matchesBlock(Blocks.SNOW)) {
	                    pq.add(h);
	                    break;
                	}
                	h--;
                    
                }
            }
        }
        Integer[] il=pq.toArray(new Integer[0]);
        if(il.length>0)
        	y=il[il.length/2];
        else
        	y=seaLevel;
        // in extreme case, that is a 16x16 valley below sea level around the spawn point,
        // we just generate the spacecraft at sea level.
        // this case should not happen because there is no known features that does so.
        if (y == 256)
            y = seaLevel;
        info.setSpawnY(y +yOffset);
        FHFeatures.spacecraft_feature.generate(serverWorld, serverWorld.getChunkProvider().getChunkGenerator(), serverWorld.rand,
                new BlockPos(info.getSpawnX(), y+yOffset, info.getSpawnZ()));
        serverWorld.setSpawnLocation(new BlockPos(info.getSpawnX(), y +yOffset, info.getSpawnZ()), info.getSpawnAngle());

    }
}
