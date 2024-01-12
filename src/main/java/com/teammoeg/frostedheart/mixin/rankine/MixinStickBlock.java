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

package com.teammoeg.frostedheart.mixin.rankine;

import com.cannolicatfish.rankine.blocks.StickBlock;
import net.minecraft.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
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
