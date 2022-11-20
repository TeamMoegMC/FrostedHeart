package com.teammoeg.frostedheart.mixin.rankine;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.cannolicatfish.rankine.blocks.StickBlock;

import net.minecraft.block.Block;
@Mixin(StickBlock.class)
public class MixinStickBlock extends Block {

	public MixinStickBlock(Properties properties) {
		super(properties);
	}
	/**
	 * @author khjxiaogu
	 * @reason fix too fast
	 * 
	 * */
	@Override
	@Overwrite
    public float getSpeedFactor() {return 1.0f;}
}
