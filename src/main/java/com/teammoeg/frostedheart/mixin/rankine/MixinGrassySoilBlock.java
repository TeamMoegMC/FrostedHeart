package com.teammoeg.frostedheart.mixin.rankine;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.cannolicatfish.rankine.blocks.GrassySoilBlock;
import com.cannolicatfish.rankine.init.RankineLists;
import com.teammoeg.frostedheart.util.FHUtils;

import net.minecraft.block.BlockState;
import net.minecraft.block.GrassBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldReader;
@Mixin(GrassySoilBlock.class)
public class MixinGrassySoilBlock extends GrassBlock {

	public MixinGrassySoilBlock(Properties properties) {
		super(properties);
	}
	
	@Inject(at=@At("HEAD"),method="isSnowyConditions",cancellable=true)
	private static void isSnowyConditions(BlockState state, IWorldReader worldReader, BlockPos pos,CallbackInfoReturnable<Boolean> cbi) {
        if(!FHUtils.canGrassSurvive(worldReader, pos))
        	cbi.setReturnValue(false);
    }
}
