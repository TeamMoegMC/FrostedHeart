/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.util;

import java.util.Random;
import java.util.function.ToIntFunction;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.teammoeg.frostedheart.climate.chunkdata.ChunkData;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ILivingEntityData;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.capabilities.CapabilityManager;

public class FHUtils {
    public static <T> T notNull() {
        return null;
    }

    public static void registerSimpleCapability(Class<?> clazz) {
        CapabilityManager.INSTANCE.register(clazz, new NoopStorage<>(), () -> {
            throw new UnsupportedOperationException("Creating default instances is not supported. Why would you ever do this");
        });
    }

    public static ToIntFunction<BlockState> getLightValueLit(int lightValue) {
        return (state) -> {
            return state.get(BlockStateProperties.LIT) ? lightValue : 0;
        };
    }

    public static boolean isRainingAt(BlockPos pos, World world) {
        if (!world.isRaining()) {
            return false;
        } else if (!world.canSeeSky(pos)) {
            return false;
        } else if (world.getHeight(Heightmap.Type.MOTION_BLOCKING, pos).getY() > pos.getY()) {
            return false;
        } else {
            return true;
        }
    }
    public static void canBigTreeGenerate(World w,BlockPos p,Random r,CallbackInfoReturnable<Boolean> cr) {
		if(!canBigTreeGenerate(w,p,r))
			cr.setReturnValue(false);
    }
    public static boolean canBigTreeGenerate(World w,BlockPos p,Random r) {
		int i=35;
		i-=ChunkData.getTemperature(w, p)/2;
		return i<=0||r.nextInt(i)==0;
			
    }
    public static void spawnMob(ServerWorld world, BlockPos blockpos, CompoundNBT nbt, ResourceLocation type) {
        if (World.isInvalidPosition(blockpos)) {
            CompoundNBT compoundnbt = nbt.copy();
            compoundnbt.putString("id", type.toString());
            Entity entity = EntityType.loadEntityAndExecute(compoundnbt, world, (p_218914_1_) -> {
                p_218914_1_.setLocationAndAngles(blockpos.getX(), blockpos.getY(), blockpos.getZ(), p_218914_1_.rotationYaw, p_218914_1_.rotationPitch);
                return p_218914_1_;
            });
            if (entity != null) {
                if (entity instanceof MobEntity) {
                    ((MobEntity)entity).onInitialSpawn(world, world.getDifficultyForLocation(entity.getPosition()), SpawnReason.NATURAL, (ILivingEntityData)null, (CompoundNBT)null);
                }
                if (!world.addEntityAndUniquePassengers(entity)) {
                    return;
                }
            }
        }
    }
}
