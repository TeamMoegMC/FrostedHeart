package com.teammoeg.frostedheart.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.contraptions.components.actors.BlockBreakingKineticTileEntity;
import com.simibubi.create.content.contraptions.components.saw.SawTileEntity;
import com.simibubi.create.foundation.utility.TreeCutter;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.BlockPos;
@Mixin(SawTileEntity.class)
public abstract class MixinSawTileEntity extends BlockBreakingKineticTileEntity{
	public MixinSawTileEntity(TileEntityType<?> typeIn) {
		super(typeIn);
	}

	@Inject(at=@At(value="INVOKE",
			target="Lcom/simibubi/create/foundation/utility/TreeCutter;findTree(Lnet/minecraft/world/IBlockReader;Lnet/minecraft/util/math/BlockPos;)Lcom/simibubi/create/foundation/utility/TreeCutter$Tree;",
			ordinal=0,remap=false),
			method="onBlockBroken",cancellable=true,remap=false)
	private void FH$onBroken(BlockState state,CallbackInfo cbi) {
		if (world == null)
			return;
		BlockState up=world.getBlockState(pos.up());
		if(TreeCutter.isVerticalPlant(state)&&!TreeCutter.isVerticalPlant(up))
			cbi.cancel();
		if(TreeCutter.isChorus(state)&&!TreeCutter.isChorus(up))
			cbi.cancel();
	}/*
	@Overwrite(remap=false)
	public void onBlockBroken(BlockState stateToBreak) {
		Optional<AbstractBlockBreakQueue> dynamicTree = TreeCutter.findDynamicTree(stateToBreak.getBlock(), breakingPos);
		if (dynamicTree.isPresent()) {
			dynamicTree.get().destroyBlocks(world, null, this::dropItemFromCutTree);
			return;
		}
		if (world == null)
			return;
		boolean flag=true;
		BlockState up=world.getBlockState(pos.up());
		if(TreeCutter.isVerticalPlant(stateToBreak)&&!TreeCutter.isVerticalPlant(up))
			flag=false;
		else if(TreeCutter.isChorus(stateToBreak)&&!TreeCutter.isChorus(up))
			flag=false;
		super.onBlockBroken(stateToBreak);
		if(flag)
		TreeCutter.findTree(world, breakingPos)
			.destroyBlocks(world, null, this::dropItemFromCutTree);
	}*/
	@Shadow(remap=false)
	public abstract void dropItemFromCutTree(BlockPos pos, ItemStack stack);
}
