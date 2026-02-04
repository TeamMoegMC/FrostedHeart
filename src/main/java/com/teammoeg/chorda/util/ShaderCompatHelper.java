package com.teammoeg.chorda.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

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
	public static <T extends Block> void shaderLikeStates(Supplier<T> r,Function<BlockState,BlockState> blockState2Vanilla) {
		for(BlockState bs:r.get().getStateDefinition().getPossibleStates()) {
			ShaderCompatHelper.modBlockState2VanillaBlockMap.put(bs, blockState2Vanilla.apply(bs));
		}
	}
	public static <T extends Block> void shaderLikeCopyState(Supplier<T> r,Block vanilla){
		shaderLikeCopyState(r,vanilla.defaultBlockState());
	}
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
	public static <T extends Block> void shaderLike(Supplier<T> r,Block vanilla){
		shaderLikeState(r,vanilla.defaultBlockState());
	}
	public static <T extends Block> void shaderLikeState(Supplier<T> r,BlockState vanilla){
		shaderLikeStates(r, b->vanilla);
	}
}
