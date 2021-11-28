package com.teammoeg.frostedheart.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import com.simibubi.create.content.contraptions.components.actors.BlockBreakingMovementBehaviour;
import com.simibubi.create.content.contraptions.components.structureMovement.MovementBehaviour;
import com.simibubi.create.content.contraptions.components.structureMovement.MovementContext;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.teammoeg.frostedheart.util.ISpeedContraption;

import net.minecraft.block.BlockState;
import net.minecraft.block.FallingBlock;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
@Mixin(BlockBreakingMovementBehaviour.class)
public abstract class MixinBlockBreakingMovementBehaviour extends MovementBehaviour {
	/**
	 * @author khjxiaogu
	 * @reason further repair breaking speed
	 */
	@Overwrite(remap=false)
	public void tickBreaker(MovementContext context) {
		CompoundNBT data = context.data;
		if (context.world.isRemote)
			return;
		if (!data.contains("BreakingPos"))
			return;
		if (context.relativeMotion.equals(Vector3d.ZERO)) {
			context.stall = false;
			return;
		}

		int ticksUntilNextProgress = data.getInt("TicksUntilNextProgress");
		if (ticksUntilNextProgress-- > 0) {
			data.putInt("TicksUntilNextProgress", ticksUntilNextProgress);
			return;
		}

		World world = context.world;
		BlockPos breakingPos = NBTUtil.readBlockPos(data.getCompound("BreakingPos"));
		int destroyProgress = data.getInt("Progress");
		int id = data.getInt("BreakerId");
		BlockState stateToBreak = world.getBlockState(breakingPos);
		float blockHardness = stateToBreak.getBlockHardness(world, breakingPos);

		if (!canBreak(world, breakingPos, stateToBreak)) {
			if (destroyProgress != 0) {
				destroyProgress = 0;
				data.remove("Progress");
				data.remove("TicksUntilNextProgress");
				data.remove("BreakingPos");
				world.sendBlockBreakProgress(id, breakingPos, -1);
			}
			context.stall = false;
			return;
		}
		float breakSpeed;
		if(context.contraption instanceof ISpeedContraption)
			breakSpeed = MathHelper.clamp(Math.abs(((ISpeedContraption) context.contraption).getSpeed()) / 100f, 1 / 128f, 16f);
		else
			breakSpeed = MathHelper.clamp(Math.abs(context.getAnimationSpeed()) / 500f, 1 / 128f, 16f);
		destroyProgress += MathHelper.clamp((int) (breakSpeed / blockHardness), 1, 10 - destroyProgress);
		world.playSound(null, breakingPos, stateToBreak.getSoundType().getHitSound(), SoundCategory.NEUTRAL, .25f, 1);

		if (destroyProgress >= 10) {
			world.sendBlockBreakProgress(id, breakingPos, -1);

			// break falling blocks from top to bottom
			BlockPos ogPos = breakingPos;
			BlockState stateAbove = world.getBlockState(breakingPos.up());
			while (stateAbove.getBlock() instanceof FallingBlock) {
				breakingPos = breakingPos.up();
				stateAbove = world.getBlockState(breakingPos.up());
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
		world.sendBlockBreakProgress(id, breakingPos, (int) destroyProgress);
		data.putInt("TicksUntilNextProgress", ticksUntilNextProgress);
		data.putInt("Progress", destroyProgress);
	}
	@Shadow(remap=false)
	protected abstract boolean shouldDestroyStartBlock(BlockState stateToBreak);
	@Shadow(remap=false)
	public abstract boolean canBreak(World world, BlockPos breakingPos, BlockState state);
	@Shadow(remap=false)
	protected abstract void onBlockBroken(MovementContext context, BlockPos pos, BlockState brokenState);
}
