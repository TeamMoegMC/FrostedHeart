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

package com.teammoeg.chorda.multiblock;

import java.util.Optional;
import java.util.function.Supplier;

import com.teammoeg.chorda.util.CUtils;

import blusunrize.immersiveengineering.api.multiblocks.blocks.MultiblockRegistration;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockBEHelper;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockLevel;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockBE;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockLogic;
import blusunrize.immersiveengineering.common.blocks.multiblocks.blockimpl.MultiblockContext;
import blusunrize.immersiveengineering.common.blocks.multiblocks.blockimpl.WrappingMultiblockContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 多方块结构的工具类，提供从世界或多方块上下文中获取多方块相关信息的静态方法。
 * 包括获取方块实体辅助器、多方块注册信息、逻辑组件、尺寸、主方块位置等功能。
 * 支持通过世界坐标或 {@link IMultiblockContext} 两种方式访问多方块数据。
 * <p>
 * Utility class for multiblock structures, providing static methods to retrieve multiblock-related
 * information from the world or multiblock context. Includes functionality for obtaining block entity
 * helpers, multiblock registrations, logic components, dimensions, master block positions, and more.
 * Supports accessing multiblock data via both world coordinates and {@link IMultiblockContext}.
 *
 * @see CMultiblock
 */
public class CMultiblockHelper {

	/** 当多方块不存在时抛出的异常提供器 / Exception supplier thrown when no multiblock exists */
	private static final Supplier<RuntimeException> noMultiblockExists=()->new IllegalStateException("Multiblock is not valid");

	/**
	 * 私有构造函数，防止实例化此工具类。
	 * <p>
	 * Private constructor to prevent instantiation of this utility class.
	 */
	private CMultiblockHelper() {
	}

	/**
	 * 尝试从世界中指定位置获取多方块方块实体辅助器。
	 * <p>
	 * Attempts to retrieve the multiblock block entity helper from the specified position in the world.
	 *
	 * @param level 当前世界 / The current level
	 * @param pos 方块位置 / The block position
	 * @return 包含方块实体辅助器的 Optional，如果该位置不是多方块则为空 / An Optional containing the block entity helper, or empty if the position is not a multiblock
	 */
	public static Optional<IMultiblockBEHelper<?>> getBEHelperOptional(Level level,BlockPos pos){
		if(CUtils.getExistingTileEntity(level, pos) instanceof IMultiblockBE te) {
			return Optional.of(te.getHelper());
		}
		return Optional.empty();
	}

	/**
	 * 从世界中指定位置获取多方块方块实体辅助器，如果不存在则抛出异常。
	 * <p>
	 * Retrieves the multiblock block entity helper from the specified position in the world,
	 * throwing an exception if it does not exist.
	 *
	 * @param level 当前世界 / The current level
	 * @param pos 方块位置 / The block position
	 * @return 方块实体辅助器 / The block entity helper
	 * @throws IllegalStateException 如果指定位置不是有效的多方块 / If the specified position is not a valid multiblock
	 */
	public static IMultiblockBEHelper<?> getBEHelper(Level level,BlockPos pos){
		return getBEHelperOptional(level,pos).orElseThrow(noMultiblockExists);
	}

	/**
	 * 尝试从世界中指定位置获取多方块逻辑组件。
	 * <p>
	 * Attempts to retrieve the multiblock logic component from the specified position in the world.
	 *
	 * @param level 当前世界 / The current level
	 * @param pos 方块位置 / The block position
	 * @return 包含多方块逻辑的 Optional，如果该位置不是多方块则为空 / An Optional containing the multiblock logic, or empty if the position is not a multiblock
	 */
	public static Optional<IMultiblockLogic<?>> getMultiblockLogic(Level level,BlockPos pos){
		return getBEHelperOptional(level,pos).map(t->t.getMultiblock().logic());
	}

	/**
	 * 从多方块层级的原点位置获取方块实体辅助器。
	 * <p>
	 * Retrieves the block entity helper from the origin position of a multiblock level.
	 *
	 * @param level 多方块层级 / The multiblock level
	 * @return 包含方块实体辅助器的 Optional，如果原点不是多方块则为空 / An Optional containing the block entity helper, or empty if the origin is not a multiblock
	 */
	public static Optional<IMultiblockBEHelper<?>> getBEHelper(IMultiblockLevel level) {
		if(level.getBlockEntity(BlockPos.ZERO) instanceof IMultiblockBE te)
			return Optional.of(te.getHelper());
		return Optional.empty();
	}

	/**
	 * 尝试从多方块上下文中获取多方块注册信息。
	 * 先尝试解包上下文，如果是 {@link MultiblockContext} 则直接获取；
	 * 否则通过方块实体辅助器间接获取。
	 * <p>
	 * Attempts to retrieve the multiblock registration from the multiblock context.
	 * First tries to unwrap the context; if it's a {@link MultiblockContext}, retrieves directly;
	 * otherwise retrieves indirectly via the block entity helper.
	 *
	 * @param ctx 多方块上下文 / The multiblock context
	 * @return 包含多方块注册信息的 Optional / An Optional containing the multiblock registration
	 */
	public static Optional<MultiblockRegistration<?>> getMultiblockOptional(IMultiblockContext<?> ctx){
		unwrap(ctx);
		if(ctx instanceof MultiblockContext mbctx) {
			return Optional.of(mbctx.multiblock());
		}
		return getBEHelper(ctx.getLevel()).map(t->t.getMultiblock());
	}

	/**
	 * 从多方块上下文中获取多方块注册信息，如果不存在则抛出异常。
	 * <p>
	 * Retrieves the multiblock registration from the multiblock context,
	 * throwing an exception if it does not exist.
	 *
	 * @param ctx 多方块上下文 / The multiblock context
	 * @return 多方块注册信息 / The multiblock registration
	 * @throws IllegalStateException 如果多方块无效 / If the multiblock is not valid
	 */
	public static MultiblockRegistration<?> getMultiblock(IMultiblockContext<?> ctx){
		return getMultiblockOptional(ctx).orElseThrow(noMultiblockExists);
	}

	/**
	 * 尝试从多方块上下文中获取多方块逻辑组件。
	 * <p>
	 * Attempts to retrieve the multiblock logic component from the multiblock context.
	 *
	 * @param ctx 多方块上下文 / The multiblock context
	 * @return 包含多方块逻辑的 Optional / An Optional containing the multiblock logic
	 */
	public static Optional<IMultiblockLogic<?>> getMultiblockLogic(IMultiblockContext<?> ctx){
		return getMultiblockOptional(ctx).map(t->t.logic());
	}

	/**
	 * 获取多方块结构的尺寸。
	 * <p>
	 * Gets the size of the multiblock structure.
	 *
	 * @param ctx 多方块上下文 / The multiblock context
	 * @return 结构尺寸向量 / The structure size vector
	 */
	public static Vec3i getSize(IMultiblockContext<?> ctx) {
		return getMultiblock(ctx).size(ctx.getLevel().getRawLevel());
	}

	/**
	 * 获取主方块在多方块结构中的相对位置。
	 * <p>
	 * Gets the master block's relative position within the multiblock structure.
	 *
	 * @param ctx 多方块上下文 / The multiblock context
	 * @return 主方块在结构中的位置 / The master block position within the structure
	 */
	public static BlockPos getMasterPos(IMultiblockContext<?> ctx) {
		return getMultiblock(ctx).masterPosInMB();
	}

	/**
	 * 获取主方块在世界坐标中的绝对位置。
	 * <p>
	 * Gets the master block's absolute position in world coordinates.
	 *
	 * @param ctx 多方块上下文 / The multiblock context
	 * @return 主方块的世界坐标绝对位置 / The master block's absolute position in world coordinates
	 */
	public static BlockPos getAbsoluteMaster(IMultiblockContext<?> ctx) {
		return ctx.getLevel().toAbsolute(getMasterPos(ctx));
	}

	/**
	 * 获取主方块位置处的方块实体。
	 * <p>
	 * Gets the block entity at the master block position.
	 *
	 * @param ctx 多方块上下文 / The multiblock context
	 * @return 主方块位置处的方块实体 / The block entity at the master block position
	 */
	public static BlockEntity getMasterBlock(IMultiblockContext<?> ctx) {
		return ctx.getLevel().getBlockEntity(getMasterPos(ctx));
	}

	/**
	 * 递归解包多方块上下文，直到获取到非包装的底层上下文。
	 * <p>
	 * Recursively unwraps the multiblock context until the underlying non-wrapping context is obtained.
	 *
	 * @param ctx 可能被包装的多方块上下文 / The potentially wrapped multiblock context
	 * @return 解包后的底层上下文 / The unwrapped underlying context
	 */
	private static IMultiblockContext<?> unwrap(IMultiblockContext<?> ctx){
		while(ctx instanceof WrappingMultiblockContext wctx) {
			ctx=wctx.inner();
		}
		return ctx;
	}
}
