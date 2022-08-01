package com.teammoeg.frostedheart.mixin.create;

import com.simibubi.create.content.contraptions.components.structureMovement.AssemblyException;
import com.simibubi.create.content.contraptions.components.structureMovement.BlockMovementChecks;
import com.simibubi.create.content.contraptions.components.structureMovement.Contraption;
import com.simibubi.create.content.contraptions.components.structureMovement.glue.SuperGlueEntity;
import com.simibubi.create.foundation.config.AllConfigs;
import com.simibubi.create.foundation.utility.Iterate;
import com.teammoeg.frostedheart.util.ISpeedContraption;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.PushReaction;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.template.Template.BlockInfo;
import net.minecraftforge.common.util.Constants.BlockFlags;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

@Mixin(Contraption.class)
public abstract class MixinContraption implements ISpeedContraption {
    float speed;
    float sc = 0;

    @Override
    public float getSpeed() {
        return speed;
    }

    @Override
    public void contributeSpeed(float s) {
        if (sc < 20480)
            sc += Math.abs(s);
    }

    @Override
    public void setSpeed(float spd) {
        speed = spd;
    }

    /**
     * @author khjxiaogu
     * @reason no more instabreak
     */
    @Overwrite(remap = false)
    protected boolean customBlockPlacement(IWorld world, BlockPos targetPos, BlockState state) {
        BlockState blockState = world.getBlockState(targetPos);

        if (sc < 20480)
            if (!blockState.getCollisionShape(world, targetPos).isEmpty() && blockState.getBlockHardness(world, targetPos) != -1) {
                if (targetPos.getY() == 0)
                    targetPos = targetPos.up();
                if (!state.getCollisionShape(world, targetPos).isEmpty()) {
                    world.playEvent(2001, targetPos, Block.getStateId(blockState));
                    world.setBlockState(targetPos, state, 3 | BlockFlags.IS_MOVING);
                    if (!blockState.getRequiresTool())
                        Block.spawnDrops(blockState, world, targetPos, null);
                } else {
                    world.playEvent(2001, targetPos, Block.getStateId(state));
                    Block.spawnDrops(state, world, targetPos, null);
                }
                return true;
            }
        return false;
    }

    @Inject(at = @At("RETURN"), method = "writeNBT", remap = false, locals = LocalCapture.CAPTURE_FAILHARD)
    public void fh$writeNBT(boolean spawnPacket, CallbackInfoReturnable<CompoundNBT> cbi, CompoundNBT cnbt) {
        cnbt.putFloat("speedCollected", sc);
    }

    @Inject(at = @At("HEAD"), method = "readNBT", remap = false)
    public void fh$readNBT(World world, CompoundNBT nbt, boolean spawnData, CallbackInfo cbi) {
        sc = nbt.getFloat("speedCollected");
    }

    @Shadow(remap = false)
    protected abstract boolean isAnchoringBlockAt(BlockPos pos);

    @Shadow(remap = false)
    protected abstract boolean movementAllowed(BlockState state, World world, BlockPos pos);

    @Shadow(remap = false)
    protected abstract void addGlue(SuperGlueEntity entity);

    @Shadow(remap = false)
    protected abstract Pair<BlockInfo, TileEntity> capture(World world, BlockPos pos);

    @Shadow(remap = false)
    protected abstract void addBlock(BlockPos pos, Pair<BlockInfo, TileEntity> pair);

    @Shadow(remap = false)
    protected Map<BlockPos, BlockInfo> blocks;

    /**
     * @author khjxiaogu
     * @reason now check collider
     */
    @Inject(at = @At(value = "INVOKE_ASSIGN", target = "Lcom/simibubi/create/content/contraptions/components/structureMovement/glue/SuperGlueHandler;gatherGlue(Lnet/minecraft/world/IWorld;Lnet/minecraft/util/math/BlockPos;)Ljava/util/Map;", remap = false, ordinal = 0)
            , method = "moveBlock", remap = false, locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    protected void fh$moveBlock(World world, @Nullable Direction forcedDirection, Queue<BlockPos> frontier,
                                Set<BlockPos> visited, CallbackInfoReturnable<Boolean> r, BlockPos pos, BlockState state, BlockPos posDown, BlockState stateBelow, Map<Direction, SuperGlueEntity> superglue) throws AssemblyException {
        // Slime blocks and super glue drag adjacent blocks if possible
        for (Direction offset : Iterate.directions) {
            BlockPos offsetPos = pos.offset(offset);
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
                if (state.getPushReaction() == PushReaction.PUSH_ONLY
                        || blockState.getPushReaction() == PushReaction.PUSH_ONLY) {
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

    private boolean isSupportive(BlockState s1, BlockState s2, World w, BlockPos p1, BlockPos p2, Direction offset) {

        return VoxelShapes.compare(s1.getShape(w, p1).project(offset), s2.getShape(w, p2).project(offset.getOpposite()), IBooleanFunction.AND);
    }
}
