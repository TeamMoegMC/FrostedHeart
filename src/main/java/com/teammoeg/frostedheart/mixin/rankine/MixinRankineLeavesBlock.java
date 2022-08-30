package com.teammoeg.frostedheart.mixin.rankine;



import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.cannolicatfish.rankine.blocks.RankineLeavesBlock;

import net.minecraft.block.LeavesBlock;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.properties.BlockStateProperties;
@Mixin(RankineLeavesBlock.class)
public class MixinRankineLeavesBlock extends LeavesBlock {
	
	public MixinRankineLeavesBlock(Properties properties) {
		super(properties);
	}
	@Inject(at=@At("TAIL"),method="<init>",remap=false)
	public void fh$init(Properties properties,CallbackInfo cbi) {
		this.setDefaultState(this.stateContainer.getBaseState().with(DISTANCE, 7).with(PERSISTENT, Boolean.FALSE).with(BlockStateProperties.AGE_0_5, 4));
	}

}
