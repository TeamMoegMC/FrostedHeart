/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.chorda.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 基础方块类，扩展了原版方块并支持自定义透光度。
 * <p>
 * Base block class extending vanilla Block with custom light opacity support.
 * The default light opacity is 15 (fully opaque). Subclasses can adjust this
 * via {@link #setLightOpacity(int)}.
 */
public class CBlock extends Block {
    /** 方块的透光度值，范围为 0（透明）到 15（不透明）。 / Light opacity value, range 0 (transparent) to 15 (opaque). */
    protected int lightOpacity;

    /**
     * 使用给定的方块属性构造基础方块，默认透光度为15。
     * <p>
     * Constructs a base block with the given properties, defaulting light opacity to 15.
     *
     * @param blockProps 方块属性 / the block properties
     */
    public CBlock(Properties blockProps) {
        super(blockProps);
        lightOpacity = 15;
    }

    /**
     * 获取方块的透光度。对于实心方块返回自定义透光度值，否则根据天光传播情况返回 0 或 1。
     * <p>
     * Gets the light blocking level of this block. Returns the custom light opacity
     * for solid renders, otherwise returns 0 if skylight propagates down, or 1.
     *
     * @param state 方块状态 / the block state
     * @param worldIn 方块所在的世界访问器 / the block getter for world access
     * @param pos 方块位置 / the block position
     * @return 透光度值 / the light blocking value
     */
    @Override
    public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
        if (state.isSolidRender(worldIn, pos))
            return lightOpacity;
        else
            return state.propagatesSkylightDown(worldIn, pos) ? 0 : 1;
    }

    /**
     * 设置此方块在光照引擎中的透光度，仅对实心方块（完整形状且遮挡光线）有效。
     * 取值范围：0（透明）至 15（不透明）。
     * <p>
     * Sets the opacity of this block in the light engine. This only works for solid blocks
     * (full shape and occluding). Value range: 0 (transparent) to 15 (opaque).
     *
     * @param opacity 透光度值 / the light opacity value
     * @return 当前方块实例，用于链式调用 / this block instance for chaining
     */
    public CBlock setLightOpacity(int opacity) {
        lightOpacity = opacity;
        return this;
    }
}
