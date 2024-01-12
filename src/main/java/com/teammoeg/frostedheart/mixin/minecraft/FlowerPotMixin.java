/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.mixin.minecraft;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.FlowerPotBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(FlowerPotBlock.class)
public abstract class FlowerPotMixin extends Block {
	public FlowerPotMixin(Properties properties) {
		super(properties);
	}

	@Shadow(remap = false)
	private java.util.function.Supplier<FlowerPotBlock> emptyPot;

	/**
	 * @author khjxiaogu
	 * @reason TODO
	 */
	@Overwrite(remap = false)
	public FlowerPotBlock getEmptyPot() {
		
		FlowerPotBlock emp= emptyPot == null ? (FlowerPotBlock)(Object)this : emptyPot.get();
		if(emp==(Object)this)
			return (FlowerPotBlock) Blocks.FLOWER_POT;
		return emp;
	}
}
