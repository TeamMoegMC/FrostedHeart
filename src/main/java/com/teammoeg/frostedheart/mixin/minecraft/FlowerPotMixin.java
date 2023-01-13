package com.teammoeg.frostedheart.mixin.minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.FlowerPotBlock;

@Mixin(FlowerPotBlock.class)
public abstract class FlowerPotMixin extends Block {
	public FlowerPotMixin(Properties properties) {
		super(properties);
	}

	@Shadow(remap = false)
	private java.util.function.Supplier<FlowerPotBlock> emptyPot;

	@Overwrite(remap = false)
	public FlowerPotBlock getEmptyPot() {
		
		FlowerPotBlock emp= emptyPot == null ? (FlowerPotBlock)(Object)this : emptyPot.get();
		if(emp==(Object)this)
			return (FlowerPotBlock) Blocks.FLOWER_POT;
		return emp;
	}
}
