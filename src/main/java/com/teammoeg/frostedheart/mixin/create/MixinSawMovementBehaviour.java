package com.teammoeg.frostedheart.mixin.create;

import com.simibubi.create.content.contraptions.components.actors.BlockBreakingMovementBehaviour;
import com.simibubi.create.content.contraptions.components.actors.SawMovementBehaviour;
import com.simibubi.create.content.contraptions.components.structureMovement.MovementContext;
import com.simibubi.create.foundation.utility.TreeCutter;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(SawMovementBehaviour.class)
public abstract class MixinSawMovementBehaviour  extends BlockBreakingMovementBehaviour{
	@Inject(at=@At(value="INVOKE",
			target="Lcom/simibubi/create/foundation/utility/TreeCutter;findTree(Lnet/minecraft/world/IBlockReader;Lnet/minecraft/util/math/BlockPos;)Lcom/simibubi/create/foundation/utility/TreeCutter$Tree;",
			ordinal=0,remap=false),
			method="onBlockBroken",cancellable=true,remap=false)
	private void FH$onBroken(MovementContext context, BlockPos pos, BlockState brokenState,CallbackInfo cbi) {
		if (context.world == null)
			return;
		BlockState up=context.world.getBlockState(pos.up());
		if(TreeCutter.isVerticalPlant(brokenState)&&!TreeCutter.isVerticalPlant(up))
			cbi.cancel();
		if(TreeCutter.isChorus(brokenState)&&!TreeCutter.isChorus(up))
			cbi.cancel();
	}
}
