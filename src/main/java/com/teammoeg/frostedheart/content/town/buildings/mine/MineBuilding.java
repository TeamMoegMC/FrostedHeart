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
import com.teammoeg.chorda.util.CDistHelper;
import lombok.Getter;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.content.town.ITown;
import com.teammoeg.frostedheart.content.town.ITownWithBuildings;
import com.teammoeg.frostedheart.content.town.block.OccupiedVolume;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

public class MineBuilding extends AbstractTownBuilding {

	public static final Codec<MineBuilding> CODEC = RecordCodecBuilder.create(t -> t.group(
                    BlockPos.CODEC.optionalFieldOf("pos",BlockPos.ZERO).forGetter(o -> o.pos),
                    Codec.BOOL.optionalFieldOf("initialized", false).forGetter(o -> o.isInitialized()),
                    Codec.BOOL.optionalFieldOf("occupiedAreaOverlapped", false).forGetter(o -> o.isOccupiedAreaOverlapped()),
                    Codec.BOOL.optionalFieldOf("isStructureValid",false).forGetter(o -> o.isStructureValid()),
                    OccupiedVolume.CODEC.optionalFieldOf("occupiedVolume",OccupiedVolume.EMPTY).forGetter(o -> o.getOccupiedVolume()),
					Codec.DOUBLE.optionalFieldOf("rating",0D).forGetter(o -> o.getRating()),
					Codec.STRING.optionalFieldOf("biomePath","minecraft:plains")
                            .forGetter(o -> o.getBiomePath().toString())
					)
			.apply(t, MineBuilding::new));

	public static final Map<ResourceLocation, Map<Item,  Integer>> BIOME_RESOURCES = new HashMap<>();
    public static final Map<Item, Integer> DEFAULT_RESOURCES = Map.of(
            Items.COBBLESTONE, 1,
            Items.COAL, 1
    );

	private ResourceLocation biomePath = new ResourceLocation("minecraft", "plains");

	@Getter
	private double rating;//might be removed

	public void setBiomePath(ResourceLocation biomePath) { this.biomePath = biomePath; fireChange(); }
	public void setRating(double rating) { this.rating = rating; fireChange(); }

	public MineBuilding(BlockPos pos) {
        super(pos);
    }

    /**
     * Full constructor matching the CODEC definition for serialization/deserialization.
     * 
     * @param pos the block position
     * @param isStructureValid whether the structure is valid
     * @param occupiedVolume the occupied area
     * @param rating the building rating
     * @param biomePathString the biome path as string
     */
    public MineBuilding(BlockPos pos, boolean initialized, boolean occupiedAreaOverlapped,
                        boolean isStructureValid, OccupiedVolume occupiedVolume,
                        double rating, String biomePathString) {
        super(pos);
        this.setInitialized(initialized);
        this.setOccupiedAreaOverlapped(occupiedAreaOverlapped);
        this.setIsStructureValid(isStructureValid);
        this.setOccupiedVolume(occupiedVolume);
        this.setRating(rating);
        ResourceLocation decodedBiome = ResourceLocation.tryParse(biomePathString);
        this.setBiomePath(decodedBiome == null
                ? new ResourceLocation("minecraft", "plains")
                : decodedBiome);
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

    public static boolean hasBiomeRecipe(ResourceLocation biomeID) {
        if (BIOME_RESOURCES.isEmpty()) {
            loadBiomeResources();
        }
        return BIOME_RESOURCES.containsKey(biomeID);
    }

	@Override
	public boolean work(ITownWithBuildings town) {
		return super.work(town);
	}

    public ResourceLocation getBiomePath() {
        return biomePath;
    }

}
