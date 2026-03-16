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
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * 支持水平朝向和附着面（墙面、地面、天花板）的可旋转方向方块。
 * 放置时根据玩家点击的面自动确定附着方式，并根据玩家的水平朝向确定方向。
 * <p>
 * A block with both horizontal facing and attach face (wall, floor, ceiling) support.
 * When placed, the attach face is determined by the clicked surface, and the horizontal
 * facing is set opposite to the player's horizontal direction.
 */
public class CDirectionalRotatableBlock extends Block {
	/** 水平朝向属性。 / Horizontal facing property. */
	public static final EnumProperty<Direction> FACING=BlockStateProperties.HORIZONTAL_FACING;
	/** 附着面属性（地面、天花板或墙面）。 / Attach face property (floor, ceiling, or wall). */
	public static final EnumProperty<AttachFace> ATTACH_FACE=BlockStateProperties.ATTACH_FACE;

	/**
	 * 使用给定的方块属性构造可旋转方向方块。
	 * <p>
	 * Constructs a directional rotatable block with the given properties.
	 *
	 * @param p_52591_ 方块属性 / the block properties
	 */
	public CDirectionalRotatableBlock(Properties p_52591_) {
		super(p_52591_);
	}

	/**
	 * 根据放置上下文确定方块状态。根据点击面设置附着方式（地面、天花板或墙面），
	 * 并设置水平朝向为玩家朝向的反方向。
	 * <p>
	 * Determines the block state for placement. Sets the attach face based on the clicked
	 * surface (floor, ceiling, or wall) and the horizontal facing opposite to the player's direction.
	 *
	 * @param pContext 方块放置上下文 / the block placement context
	 * @return 放置时的方块状态 / the block state for placement
	 */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext pContext) {
		Direction clickedFace=pContext.getClickedFace();
		return this.defaultBlockState().setValue(ATTACH_FACE, (clickedFace.getAxis()==Axis.Y)?(clickedFace==Direction.UP?AttachFace.FLOOR:AttachFace.CEILING):AttachFace.WALL)
				.setValue(FACING, pContext.getHorizontalDirection().getOpposite());
	}

	/**
	 * 创建方块状态定义，注册 FACING 和 ATTACH_FACE 属性。
	 * <p>
	 * Creates the block state definition, registering FACING and ATTACH_FACE properties.
	 *
	 * @param pBuilder 方块状态定义构建器 / the block state definition builder
	 */
	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
		super.createBlockStateDefinition(pBuilder);
		pBuilder.add(FACING).add(ATTACH_FACE);
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
	@Override
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
	@Override
	public BlockState mirror(BlockState pState, Mirror pMirror) {
		return pState.setValue(FACING, pMirror.mirror(pState.getValue(FACING)));
	}

}
