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

package com.teammoeg.frostedheart.content.town.buildings.mine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.chorda.util.CDistHelper;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.content.town.Town;
import com.teammoeg.frostedheart.content.town.block.OccupiedArea;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MineBuilding extends AbstractTownBuilding {

	public static final Codec<MineBuilding> CODEC = RecordCodecBuilder.create(t -> t.group(
					BlockPos.CODEC.fieldOf("pos").forGetter(o -> o.pos),
					Codec.BOOL.fieldOf("isStructureValid").forGetter(o -> o.isStructureValid),
					OccupiedArea.CODEC.fieldOf("occupiedArea").forGetter(o -> o.occupiedArea),
					Codec.DOUBLE.fieldOf("rating").forGetter(o -> o.rating),
					Codec.STRING.fieldOf("biomePath").forGetter(o -> o.biomePath.toString())
					)
			.apply(t, MineBuilding::new));

	public static final Map<ResourceLocation, Map<Item,  Integer>> BIOME_RESOURCES = new HashMap<>();
	public static final Map<Item, Integer> DEFAULT_RESOURCES = Map.of(Items.COBBLESTONE, 1);

	public ResourceLocation biomePath;

	public double rating;//might be removed

	public MineBuilding(BlockPos pos) {
        super(pos);
    }

    /**
     * Full constructor matching the CODEC definition for serialization/deserialization.
     * 
     * @param pos the block position
     * @param isStructureValid whether the structure is valid
     * @param occupiedArea the occupied area
     * @param rating the building rating
     * @param biomePathString the biome path as string
     */
    public MineBuilding(BlockPos pos, boolean isStructureValid, OccupiedArea occupiedArea, double rating, String biomePathString) {
        super(pos);
        this.isStructureValid = isStructureValid;
        this.occupiedArea = occupiedArea;
        this.rating = rating;
        this.biomePath = new ResourceLocation(biomePathString);
    }

	private static void loadBiomeResources() {
		for(BiomeMineResourceRecipe recipe : CUtils.filterRecipes(CDistHelper.getRecipeManager(), BiomeMineResourceRecipe.TYPE)){
			ResourceLocation biomeID = recipe.biomeID;
			Map<Item, Integer> weights = recipe.weights;
			BIOME_RESOURCES.put(biomeID, weights);
		}
	}

	public static Map<Item, Integer> getWeights(ResourceLocation biomeID){
		if(BIOME_RESOURCES.isEmpty()){
			loadBiomeResources();
		}
		if(BIOME_RESOURCES.containsKey(biomeID)){
			return BIOME_RESOURCES.get(biomeID);
		}
		return DEFAULT_RESOURCES;
	}

	@Override
	public boolean work(Town town) {
		return super.work(town);
	}

}
