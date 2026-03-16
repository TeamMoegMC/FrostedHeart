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

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * 具有方向朝向属性的方块，支持六个方向（上、下、东、西、南、北）。
 * 放置时朝向与玩家视线方向相反，默认朝向为北。
 * <p>
 * A block with directional facing properties supporting all six directions
 * (up, down, east, west, south, north). When placed, the block faces opposite
 * to the player's nearest looking direction. Default facing is north.
 */
public class CDirectionalFacingBlock extends DirectionalBlock {

    /**
     * 使用给定的方块属性构造方向朝向方块，默认朝向为北。
     * <p>
     * Constructs a directional facing block with the given properties, defaulting to north facing.
     *
     * @param p_52591_ 方块属性 / the block properties
     */
    public CDirectionalFacingBlock(Properties p_52591_) {
        super(p_52591_);
        super.registerDefaultState(super.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /**
     * 根据放置上下文确定方块状态，朝向与玩家最近的视线方向相反。
     * <p>
     * Determines the block state for placement, facing opposite to the player's
     * nearest looking direction.
     *
     * @param pContext 方块放置上下文 / the block placement context
     * @return 放置时的方块状态 / the block state for placement
     */
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(FACING, pContext.getNearestLookingDirection().getOpposite());
    }

    /**
     * 创建方块状态定义，注册 FACING 属性。
     * <p>
     * Creates the block state definition, registering the FACING property.
     *
     * @param pBuilder 方块状态定义构建器 / the block state definition builder
     */
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING);
    }

    /**
     * 对方块状态应用旋转变换。
     * <p>
     * Applies a rotation transformation to the block state.
     *
     * @param pState 原始方块状态 / the original block state
     * @param pRot 旋转变换 / the rotation to apply
     * @return 旋转后的方块状态 / the rotated block state
     */
    public BlockState rotate(BlockState pState, Rotation pRot) {
        return pState.setValue(FACING, pRot.rotate(pState.getValue(FACING)));
    }

    /**
     * 对方块状态应用镜像变换。
     * <p>
     * Applies a mirror transformation to the block state.
     *
     * @param pState 原始方块状态 / the original block state
     * @param pMirror 镜像变换 / the mirror to apply
     * @return 镜像后的方块状态 / the mirrored block state
     */
    public BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
    }
}
