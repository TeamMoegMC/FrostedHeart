/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Caupona.
 *
 * Caupona is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Caupona is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * Specially, we allow this software to be used alongside with closed source software Minecraft(R) and Forge or other modloader.
 * Any mods or plugins can also use apis provided by forge or com.teammoeg.caupona.api without using GPL or open source.
 *
 * You should have received a copy of the GNU General Public License
 * along with Caupona. If not, see <https://www.gnu.org/licenses/>.
 */

package com.teammoeg.frostedheart.client.model;

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

public record DynamicBlockModelReference(ResourceLocation name) implements Supplier<BakedModel>,Function<ModelData,List<BakedQuad>>
{

	private static final RandomSource RANDOM_SOURCE=RandomSource.create();
	static {
		RANDOM_SOURCE.setSeed(42L);
	}
	public static final Function<ResourceLocation,DynamicBlockModelReference> cache=Util.memoize(DynamicBlockModelReference::new);
	public static DynamicBlockModelReference getModelCached(ResourceLocation rl)
	{
		if(rl==null)
			return null;
		return cache.apply(rl);
	}
	public static final Set<ResourceLocation> registeredModels=Collections.newSetFromMap(new ConcurrentHashMap<>());
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