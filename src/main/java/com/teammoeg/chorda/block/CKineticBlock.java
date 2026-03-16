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

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 动力学方块基类，集成 Create 模组的水平动力学方块，并支持自定义透光度。
 * 使用动态形状（dynamicShape）以兼容 Create 模组的渲染系统。
 * <p>
 * Base class for kinetic blocks, integrating with the Create mod's {@link HorizontalKineticBlock}.
 * Supports custom light opacity and uses dynamic shape for compatibility with Create's rendering system.
 */
public abstract class CKineticBlock extends HorizontalKineticBlock {
    /** 方块的透光度值，范围为 0（透明）到 15（不透明）。 / Light opacity value, range 0 (transparent) to 15 (opaque). */
    protected int lightOpacity;

    /**
     * 使用给定的方块属性构造动力学方块，默认透光度为 15，并启用动态形状。
     * <p>
     * Constructs a kinetic block with the given properties, defaulting light opacity to 15
     * and enabling dynamic shape.
     *
     * @param blockProps 方块属性 / the block properties
     */
    public CKineticBlock(Properties blockProps) {
        super(blockProps.dynamicShape());
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
     * 设置此方块在光照引擎中的透光度，仅对实心方块有效。
     * 取值范围：0（透明）至 15（不透明）。
     * <p>
     * Sets the opacity of this block in the light engine. This only works for solid blocks.
     * Value range: 0 (transparent) to 15 (opaque).
     *
     * @param opacity 透光度值 / the light opacity value
     * @return 当前方块实例，用于链式调用 / this block instance for chaining
     */
    public CKineticBlock setLightOpacity(int opacity) {
        lightOpacity = opacity;
        return this;
    }
}
