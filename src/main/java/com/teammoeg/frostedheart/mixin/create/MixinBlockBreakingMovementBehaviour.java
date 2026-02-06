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

package com.teammoeg.frostedheart.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import com.simibubi.create.content.contraptions.behaviour.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.kinetics.base.BlockBreakingMovementBehaviour;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.teammoeg.frostedheart.compat.create.ISpeedContraption;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

@Mixin(BlockBreakingMovementBehaviour.class)
public abstract class MixinBlockBreakingMovementBehaviour implements MovementBehaviour {
    @Shadow(remap = false)
    public abstract boolean canBreak(Level world, BlockPos breakingPos, BlockState state);

    @Shadow(remap = false)
    protected abstract void onBlockBroken(MovementContext context, BlockPos pos, BlockState brokenState);

    @Shadow(remap = false)
    protected abstract boolean shouldDestroyStartBlock(BlockState stateToBreak);

    /**
     * @author khjxiaogu
     * @reason further repair breaking speed
     */
    @Overwrite(remap = false)
    public void tickBreaker(MovementContext context) {
        CompoundTag data = context.data;
        if (context.world.isClientSide)
            return;
        if (!data.contains("BreakingPos"))
            return;
        if (context.relativeMotion.equals(Vec3.ZERO)) {
            context.stall = false;
            return;
        }

        int ticksUntilNextProgress = data.getInt("TicksUntilNextProgress");
        if (ticksUntilNextProgress-- > 0) {
            data.putInt("TicksUntilNextProgress", ticksUntilNextProgress);
            return;
        }

        Level world = context.world;
        BlockPos breakingPos = NbtUtils.readBlockPos(data.getCompound("BreakingPos"));
        int destroyProgress = data.getInt("Progress");
        int id = data.getInt("BreakerId");
        BlockState stateToBreak = world.getBlockState(breakingPos);
        float blockHardness = stateToBreak.getDestroySpeed(world, breakingPos);

        if (!canBreak(world, breakingPos, stateToBreak)) {
            if (destroyProgress != 0) {
                destroyProgress = 0;
                data.remove("Progress");
                data.remove("TicksUntilNextProgress");
                data.remove("BreakingPos");
                world.destroyBlockProgress(id, breakingPos, -1);
            }
            context.stall = false;
            return;
        }
        float breakSpeed;
        if (context.contraption instanceof ISpeedContraption)
            breakSpeed = Mth.clamp(Math.abs(((ISpeedContraption) context.contraption).getSpeed()) * 10, 2, 16000f);
        else
            breakSpeed = Mth.clamp(Math.abs(context.getAnimationSpeed()) / 500f, 1 / 128f, 16f);
        destroyProgress += Mth.clamp((int) (breakSpeed / blockHardness), 1, 10000 - destroyProgress);
        world.playSound(null, breakingPos, stateToBreak.getSoundType().getHitSound(), SoundSource.NEUTRAL, .25f, 1);

        if (destroyProgress >= 10000) {
            world.destroyBlockProgress(id, breakingPos, -1);

            // break falling blocks from top to bottom
            BlockPos ogPos = breakingPos;
            BlockState stateAbove = world.getBlockState(breakingPos.above());
            while (stateAbove.getBlock() instanceof FallingBlock) {
                breakingPos = breakingPos.above();
                stateAbove = world.getBlockState(breakingPos.above());
            }
            stateToBreak = world.getBlockState(breakingPos);

            context.stall = false;
            if (shouldDestroyStartBlock(stateToBreak))
                BlockHelper.destroyBlock(context.world, breakingPos, 1f, stack -> this.dropItem(context, stack));
            onBlockBroken(context, ogPos, stateToBreak);
            ticksUntilNextProgress = -1;
            data.remove("Progress");
            data.remove("TicksUntilNextProgress");
            data.remove("BreakingPos");
            return;
        }

        ticksUntilNextProgress = (int) (blockHardness / breakSpeed);
        world.destroyBlockProgress(id, breakingPos, Mth.clamp(destroyProgress / 1000, 1, 10));
        data.putInt("TicksUntilNextProgress", ticksUntilNextProgress);
        data.putInt("Progress", destroyProgress);
    }
}
