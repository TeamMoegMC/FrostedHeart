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
 * A reference key for dynamic model registry, this should be constructed during constructor and before assets reloaded
 * Use of {@link #getModelCached getModelCached} is suggested to prevent creating multiple instances
 * */
public record DynamicBlockModelReference(ResourceLocation name) implements Supplier<BakedModel>,Function<ModelData,List<BakedQuad>>
{

	private static final RandomSource RANDOM_SOURCE=RandomSource.create();
	static {
		RANDOM_SOURCE.setSeed(42L);
	}
	private static final Function<ResourceLocation,DynamicBlockModelReference> cache=Util.memoize(DynamicBlockModelReference::new);
	public static DynamicBlockModelReference getModelCached(ResourceLocation rl)
	{
		if(rl==null)
			return null;
		return cache.apply(rl);
	}
	public static final Set<ResourceLocation> registeredModels=Collections.newSetFromMap(new ConcurrentHashMap<>());
	/**
	 * Register model to add when resource loaded, this must be called during mod construct
	 * */
	public DynamicBlockModelReference register() {
		registeredModels.add(name);
		return this;
	}
	public static DynamicBlockModelReference getModelCached(String modid,String path)
	{

		return cache.apply(new ResourceLocation(modid,path));
	}
	@Override
	public BakedModel get()
	{
		return Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getModelManager().getModel(name);
	}

	public List<BakedQuad> getAllQuads()
	{
		return apply(ModelData.EMPTY);
	}
	@Override
	public List<BakedQuad> apply(ModelData data)
	{
		return get().getQuads(null, null,RANDOM_SOURCE, data, null);
	}
	public static RandomSource getRandomSource() {
		return RANDOM_SOURCE;
	}

}