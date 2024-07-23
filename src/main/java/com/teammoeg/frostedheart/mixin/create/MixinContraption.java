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

package com.teammoeg.frostedheart.mixin.create;

import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.simibubi.create.content.contraptions.components.structureMovement.AssemblyException;
import com.simibubi.create.content.contraptions.components.structureMovement.BlockMovementChecks;
import com.simibubi.create.content.contraptions.components.structureMovement.Contraption;
import com.simibubi.create.content.contraptions.components.structureMovement.glue.SuperGlueEntity;
import com.simibubi.create.foundation.config.AllConfigs;
import com.simibubi.create.foundation.utility.Iterate;
import com.teammoeg.frostedheart.util.mixin.ISpeedContraption;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

@Mixin(Contraption.class)
public abstract class MixinContraption implements ISpeedContraption {
    float speed;
    float sc = 0;

    @Shadow(remap = false)
    protected Map<BlockPos, StructureBlockInfo> blocks;

    @Shadow(remap = false)
    protected abstract void addBlock(BlockPos pos, Pair<StructureBlockInfo, BlockEntity> pair);

    @Shadow(remap = false)
    protected abstract void addGlue(SuperGlueEntity entity);

    @Shadow(remap = false)
    protected abstract Pair<StructureBlockInfo, BlockEntity> capture(Level world, BlockPos pos);

    @Override
    public void contributeSpeed(float s) {
        if (sc < 20480)
            sc += Math.abs(s);
    }

    /**
     * @author khjxiaogu
     * @reason no more instabreak
     */
    @Overwrite(remap = false)
    protected boolean customBlockPlacement(LevelAccessor world, BlockPos targetPos, BlockState state) {
        BlockState blockState = world.getBlockState(targetPos);

        if (sc < 20480)
            if (!blockState.getBlockSupportShape(world, targetPos).isEmpty() && blockState.getDestroySpeed(world, targetPos) != -1) {
                if (targetPos.getY() == 0)
                    targetPos = targetPos.above();
                if (!state.getBlockSupportShape(world, targetPos).isEmpty()) {
                    world.levelEvent(2001, targetPos, Block.getId(blockState));
                    world.setBlock(targetPos, state, 3 | Block.UPDATE_MOVE_BY_PISTON);
                    if (!blockState.requiresCorrectToolForDrops())
                        Block.dropResources(blockState, world, targetPos, null);
                } else {
                    world.levelEvent(2001, targetPos, Block.getId(state));
                    Block.dropResources(state, world, targetPos, null);
                }
                return true;
            }
        return false;
    }

    /**
     * @author khjxiaogu
     * @reason now check collider
     */
    @Inject(at = @At(value = "INVOKE_ASSIGN", target = "Lcom/simibubi/create/content/contraptions/components/structureMovement/glue/SuperGlueHandler;gatherGlue(Lnet/minecraft/world/IWorld;Lnet/minecraft/util/math/BlockPos;)Ljava/util/Map;", remap = false, ordinal = 0)
            , method = "moveBlock", remap = false, locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    protected void fh$moveBlock(Level world, @Nullable Direction forcedDirection, Queue<BlockPos> frontier,
                                Set<BlockPos> visited, CallbackInfoReturnable<Boolean> r, BlockPos pos, BlockState state, BlockPos posDown, BlockState stateBelow, Map<Direction, SuperGlueEntity> superglue) throws AssemblyException {
        // Slime blocks and super glue drag adjacent blocks if possible
        for (Direction offset : Iterate.directions) {
            BlockPos offsetPos = pos.relative(offset);
            BlockState blockState = world.getBlockState(offsetPos);
            if (isAnchoringBlockAt(offsetPos))
                continue;
            if (!movementAllowed(blockState, world, offsetPos)) {
                if (offset == forcedDirection)
                    throw AssemblyException.unmovableBlock(pos, state);
                continue;
            }

            boolean wasVisited = visited.contains(offsetPos);
            boolean faceHasGlue = superglue.containsKey(offset);
            boolean blockAttachedTowardsFace =
                    BlockMovementChecks.isBlockAttachedTowards(blockState, world, offsetPos, offset.getOpposite());
            boolean brittle = BlockMovementChecks.isBrittle(blockState);
            boolean canStick = !brittle && state.canStickTo(blockState) && blockState.canStickTo(state);
            if (canStick) {
                if (state.getPistonPushReaction() == PushReaction.PUSH_ONLY
                        || blockState.getPistonPushReaction() == PushReaction.PUSH_ONLY) {
                    canStick = false;
                }
                if (BlockMovementChecks.isNotSupportive(state, offset)) {
                    canStick = false;
                }
                if (BlockMovementChecks.isNotSupportive(blockState, offset.getOpposite())) {
                    canStick = false;
                }
            }

            state.getBlock();
            if (!wasVisited && (
                    canStick ||
                            blockAttachedTowardsFace ||
                            faceHasGlue ||
                            (offset == forcedDirection && !BlockMovementChecks.isNotSupportive(state, forcedDirection))
            ) && isSupportive(state, blockState, world, pos, offsetPos, offset))
                frontier.add(offsetPos);
            if (faceHasGlue)
                addGlue(superglue.get(offset));
        }

        addBlock(pos, capture(world, pos));
        if (blocks.size() <= AllConfigs.SERVER.kinetics.maxBlocksMoved.get())
            r.setReturnValue(true);
        else
            throw AssemblyException.structureTooLarge();
    }

    @Inject(at = @At("HEAD"), method = "readNBT", remap = false)
    public void fh$readNBT(Level world, CompoundTag nbt, boolean spawnData, CallbackInfo cbi) {
        sc = nbt.getFloat("speedCollected");
    }

    @Inject(at = @At("RETURN"), method = "writeNBT", remap = false, locals = LocalCapture.CAPTURE_FAILHARD)
    public void fh$writeNBT(boolean spawnPacket, CallbackInfoReturnable<CompoundTag> cbi, CompoundTag cnbt) {
        cnbt.putFloat("speedCollected", sc);
    }

    @Override
    public float getSpeed() {
        return speed;
    }

    @Shadow(remap = false)
    protected abstract boolean isAnchoringBlockAt(BlockPos pos);

    private boolean isSupportive(BlockState s1, BlockState s2, Level w, BlockPos p1, BlockPos p2, Direction offset) {

        return Shapes.joinIsNotEmpty(s1.getShape(w, p1).getFaceShape(offset), s2.getShape(w, p2).getFaceShape(offset.getOpposite()), BooleanOp.AND);
    }

    @Shadow(remap = false)
    protected abstract boolean movementAllowed(BlockState state, Level world, BlockPos pos);

    @Override
    public void setSpeed(float spd) {
        speed = spd;
    }
}
