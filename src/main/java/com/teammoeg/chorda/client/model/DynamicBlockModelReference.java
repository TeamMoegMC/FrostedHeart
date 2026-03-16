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

package com.teammoeg.chorda.client.model;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraftforge.client.model.data.ModelData;
/**
 * 动态方块模型注册表的引用键。应在构造阶段且资源重载前创建。建议使用 {@link #getModelCached} 以避免创建多个实例。
 * <p>
 * A reference key for the dynamic model registry. Should be constructed during initialization and before assets are reloaded.
 * Use of {@link #getModelCached getModelCached} is suggested to prevent creating multiple instances.
 */
public record DynamicBlockModelReference(ResourceLocation name) implements Supplier<BakedModel>,Function<ModelData,List<BakedQuad>>
{

	private static final RandomSource RANDOM_SOURCE=RandomSource.create();
	static {
		RANDOM_SOURCE.setSeed(42L);
	}
	private static final Function<ResourceLocation,DynamicBlockModelReference> cache=Util.memoize(DynamicBlockModelReference::new);
	/**
	 * 获取缓存的模型引用。如果传入null则返回null。
	 * <p>
	 * Gets a cached model reference. Returns null if the input is null.
	 *
	 * @param rl 模型的资源位置 / the resource location of the model
	 * @return 缓存的模型引用，或null / the cached model reference, or null
	 */
	public static DynamicBlockModelReference getModelCached(ResourceLocation rl)
	{
		if(rl==null)
			return null;
		return cache.apply(rl);
	}
	public static final Set<ResourceLocation> registeredModels=Collections.newSetFromMap(new ConcurrentHashMap<>());
	/**
	 * 注册模型以在资源加载时添加，必须在模组构造阶段调用。
	 * <p>
	 * Registers the model to be added when resources are loaded. Must be called during mod construction.
	 *
	 * @return 当前实例（链式调用） / this instance for chaining
	 */
	public DynamicBlockModelReference register() {
		registeredModels.add(name);
		return this;
	}
	/**
	 * 通过模组ID和路径获取缓存的模型引用。
	 * <p>
	 * Gets a cached model reference by mod ID and path.
	 *
	 * @param modid 模组ID / the mod ID
	 * @param path 模型路径 / the model path
	 * @return 缓存的模型引用 / the cached model reference
	 */
	public static DynamicBlockModelReference getModelCached(String modid,String path)
	{

		return cache.apply(new ResourceLocation(modid,path));
	}
	/**
	 * 从Minecraft的模型管理器获取已烘焙的模型。
	 * <p>
	 * Gets the baked model from Minecraft's model manager.
	 *
	 * @return 已烘焙的模型 / the baked model
	 */
	@Override
	public BakedModel get()
	{
		return Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getModelManager().getModel(name);
	}

	/**
	 * 获取模型的所有面片（使用空的模型数据）。
	 * <p>
	 * Gets all quads of the model using empty model data.
	 *
	 * @return 所有已烘焙的面片列表 / the list of all baked quads
	 */
	public List<BakedQuad> getAllQuads()
	{
		return apply(ModelData.EMPTY);
	}
	/**
	 * 使用指定的模型数据获取模型面片。
	 * <p>
	 * Gets model quads with the specified model data.
	 *
	 * @param data 模型数据 / the model data
	 * @return 已烘焙的面片列表 / the list of baked quads
	 */
	@Override
	public List<BakedQuad> apply(ModelData data)
	{
		return get().getQuads(null, null,RANDOM_SOURCE, data, null);
	}
	/**
	 * 获取用于模型面片查询的固定种子随机源。
	 * <p>
	 * Gets the fixed-seed random source used for model quad queries.
	 *
	 * @return 随机源实例 / the random source instance
	 */
	public static RandomSource getRandomSource() {
		return RANDOM_SOURCE;
	}

}