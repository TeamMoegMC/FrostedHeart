package com.teammoeg.frostedheart.content.robotics.logistics;

import java.util.function.Supplier;

import com.teammoeg.chorda.block.FHGuiBlock;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class LogisticChestBlock<T extends BlockEntity> extends FHGuiBlock<T>{
	Supplier<BlockEntityType<T>> blockEntity;


	public LogisticChestBlock(Properties blockProps, Supplier<BlockEntityType<T>> blockEntity) {
		super(blockProps);
		this.blockEntity = blockEntity;
	}


	@Override
	public Supplier<BlockEntityType<T>> getBlock() {
		return blockEntity;
	}


}
