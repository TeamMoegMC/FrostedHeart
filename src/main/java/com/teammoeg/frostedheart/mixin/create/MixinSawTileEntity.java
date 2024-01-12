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

import com.simibubi.create.content.contraptions.components.actors.BlockBreakingKineticTileEntity;
import com.simibubi.create.content.contraptions.components.saw.SawTileEntity;
import com.simibubi.create.foundation.utility.TreeCutter;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SawTileEntity.class)
public abstract class MixinSawTileEntity extends BlockBreakingKineticTileEntity {
    public MixinSawTileEntity(TileEntityType<?> typeIn) {
        super(typeIn);
    }

    @Inject(at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/foundation/utility/TreeCutter;findTree(Lnet/minecraft/world/IBlockReader;Lnet/minecraft/util/math/BlockPos;)Lcom/simibubi/create/foundation/utility/TreeCutter$Tree;",
            ordinal = 0, remap = false),
            method = "onBlockBroken", cancellable = true, remap = false)
    private void FH$onBroken(BlockState state, CallbackInfo cbi) {
        if (world == null)
            return;
        BlockState up = world.getBlockState(pos.up());
        if (TreeCutter.isVerticalPlant(state) && !TreeCutter.isVerticalPlant(up))
            cbi.cancel();
        if (TreeCutter.isChorus(state) && !TreeCutter.isChorus(up))
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

    @Shadow(remap = false)
    public abstract void dropItemFromCutTree(BlockPos pos, ItemStack stack);
}
