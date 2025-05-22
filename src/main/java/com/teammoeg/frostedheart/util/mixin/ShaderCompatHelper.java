package com.teammoeg.frostedheart.util.mixin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import com.teammoeg.frostedheart.infrastructure.gen.FHBlockBuilder;

import net.irisshaders.iris.shaderpack.materialmap.BlockEntry;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.RegistryObject;

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
	public record SimpleShaderBuilder(BlockState vanilla){
		public SimpleShaderBuilder add(RegistryObject<Block> r){
			shaderLikeState(r, vanilla);
			return this;
		} 
		public <T extends Block> SimpleShaderBuilder add(com.tterrag.registrate.util.entry.BlockEntry<T> r){
			shaderLikeState(r, vanilla);
			return this;
		}
	}
	public static SimpleShaderBuilder use(Block vanilla){
		return use(vanilla.defaultBlockState());
	}
	public static SimpleShaderBuilder use(BlockState vanilla){
		return new SimpleShaderBuilder(vanilla);
	}
	public static void shaderLikeStates(RegistryObject<Block> r,Function<BlockState,BlockState> blockState2Vanilla) {
		for(BlockState bs:r.get().getStateDefinition().getPossibleStates()) {
			ShaderCompatHelper.modBlockState2VanillaBlockMap.put(bs, blockState2Vanilla.apply(bs));
		}
	}
	public static void shaderLike(RegistryObject<Block> r,Block vanilla){
		shaderLikeState(r,vanilla.defaultBlockState());
	}
	public static void shaderLikeState(RegistryObject<Block> r,BlockState vanilla){
		shaderLikeStates(r, b->vanilla);
	}
	public static <T extends Block> void shaderLikeStates(com.tterrag.registrate.util.entry.BlockEntry<T> r,Function<BlockState,BlockState> blockState2Vanilla) {
		for(BlockState bs:r.get().getStateDefinition().getPossibleStates()) {
			ShaderCompatHelper.modBlockState2VanillaBlockMap.put(bs, blockState2Vanilla.apply(bs));
		}
	}
	public static <T extends Block> void shaderLike(com.tterrag.registrate.util.entry.BlockEntry<T> r,Block vanilla){
		shaderLikeState(r,vanilla.defaultBlockState());
	}
	public static <T extends Block> void shaderLikeState(com.tterrag.registrate.util.entry.BlockEntry<T> r,BlockState vanilla){
		shaderLikeStates(r, b->vanilla);
	}
}
