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

package com.teammoeg.chorda.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * 光影兼容辅助类，提供将模组方块状态映射到原版方块状态的功能，
 * 使光影包能正确渲染模组方块。
 * <p>
 * Shader compatibility helper class providing mapping from mod block states
 * to vanilla block states, enabling shader packs to correctly render mod blocks.
 */
public class ShaderCompatHelper {
	public static final Map<BlockState,BlockState> modBlockState2VanillaBlockMap=new HashMap<>();
	/*public static final List<Runnable> initializers=new ArrayList<>();
	private static boolean isInit=false;
	public static void tryInit() {
		if(!isInit) {

			isInit=true;
			initializers.forEach(Runnable::run);
		}
	}*/
	@FunctionalInterface
	public static interface BlockStateMapper{
		public BlockState applyState(BlockState vanilla,BlockState added);
	}
	public record SimpleShaderBuilder(BlockState vanilla){
		/**
		 * simply specify all state of a block to the specified blockstate shader
		 * 
		 * */
		public <T extends Block> SimpleShaderBuilder add(Supplier<T> r){
			shaderLikeState(r, vanilla);
			return this;
		}
		public <T extends Block> SimpleShaderBuilder addAll(Collection<? extends Supplier<T>> r){
			for(Supplier<T> i:r)
				shaderLikeState(i, vanilla);
			return this;
		}
		/**
		 * simply specify a block and function to set state property to the specified blockstate shader
		 * */
		public <T extends Block> SimpleShaderBuilder add(Supplier<T> r,Function<BlockState,BlockState> stateFunction){
			add(stateFunction.apply(r.get().defaultBlockState()));
			return this;
		}
		/**
		 * simply specify a blockstate to the specified blockstate shader
		 * */
		public SimpleShaderBuilder add(BlockState r){
			ShaderCompatHelper.modBlockState2VanillaBlockMap.put(r, vanilla);
			return this;
		} 
		/**
		 * copy all state from this block to the specific blockstate, and use all potential shaders.
		 * */
		public <T extends Block> SimpleShaderBuilder addSameProperty(Supplier<T> r){
			shaderLikeCopyState(r, vanilla.getBlock());
			return this;
		}
		/**
		 *  For each state, provide a blockstate to use its shader.
		 * */
		public <T extends Block> SimpleShaderBuilder addMapped(Supplier<T> r,BlockStateMapper mapper){
			shaderLikeStates(r, bs->mapper.applyState(vanilla, bs));
			return this;
		}
	}
	/**
	 * Use default state of a block for shader
	 * */
	public static SimpleShaderBuilder use(Block vanilla){
		return use(vanilla.defaultBlockState());
	}
	/**
	 * Use selected state of a block for shader
	 * */
	public static SimpleShaderBuilder use(BlockState vanilla){
		return new SimpleShaderBuilder(vanilla);
	}
	/**
	 * 将方块的所有可能状态通过映射函数映射到原版方块状态。
	 * <p>
	 * Map all possible states of a block to vanilla block states via a mapping function.
	 *
	 * @param <T> 方块类型 / the block type
	 * @param r 方块供应器 / the block supplier
	 * @param blockState2Vanilla 状态映射函数 / the state mapping function
	 */
	public static <T extends Block> void shaderLikeStates(Supplier<T> r,Function<BlockState,BlockState> blockState2Vanilla) {
		for(BlockState bs:r.get().getStateDefinition().getPossibleStates()) {
			ShaderCompatHelper.modBlockState2VanillaBlockMap.put(bs, blockState2Vanilla.apply(bs));
		}
	}
	/**
	 * 将方块的所有状态属性复制到原版方块的默认状态上。
	 * <p>
	 * Copy all state properties of a block onto the default state of a vanilla block.
	 *
	 * @param <T> 方块类型 / the block type
	 * @param r 方块供应器 / the block supplier
	 * @param vanilla 原版方块 / the vanilla block
	 */
	public static <T extends Block> void shaderLikeCopyState(Supplier<T> r,Block vanilla){
		shaderLikeCopyState(r,vanilla.defaultBlockState());
	}
	/**
	 * 将方块的所有状态属性复制到指定的原版方块状态上。
	 * <p>
	 * Copy all state properties of a block onto the specified vanilla block state.
	 *
	 * @param <T> 方块类型 / the block type
	 * @param r 方块供应器 / the block supplier
	 * @param vanilla 原版方块状态 / the vanilla block state
	 */
	public static <T extends Block> void shaderLikeCopyState(Supplier<T> r,BlockState vanilla){
		shaderLikeStates(r, b->{
			BlockState bs=vanilla;
			for(Property property:b.getProperties()) {
				if(bs.hasProperty(property))
					bs=bs.setValue(property, b.getValue(property));
			}
			return bs;
		});
	}
	/**
	 * 将方块的所有状态统一映射到原版方块的默认状态。
	 * <p>
	 * Map all states of a block to the default state of a vanilla block.
	 *
	 * @param <T> 方块类型 / the block type
	 * @param r 方块供应器 / the block supplier
	 * @param vanilla 原版方块 / the vanilla block
	 */
	public static <T extends Block> void shaderLike(Supplier<T> r,Block vanilla){
		shaderLikeState(r,vanilla.defaultBlockState());
	}
	/**
	 * 将方块的所有状态统一映射到指定的原版方块状态。
	 * <p>
	 * Map all states of a block to the specified vanilla block state.
	 *
	 * @param <T> 方块类型 / the block type
	 * @param r 方块供应器 / the block supplier
	 * @param vanilla 原版方块状态 / the vanilla block state
	 */
	public static <T extends Block> void shaderLikeState(Supplier<T> r,BlockState vanilla){
		shaderLikeStates(r, b->vanilla);
	}
}
