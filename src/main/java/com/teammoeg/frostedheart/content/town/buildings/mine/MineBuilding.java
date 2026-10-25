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

package com.teammoeg.frostedheart.content.town.buildings.mine;

import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import static com.teammoeg.frostedheart.content.town.building.BuildingRecordsCodec.strict;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.util.CDistHelper;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.content.town.ITownWithBuildings;
import com.teammoeg.frostedheart.content.town.block.OccupiedZoneInfo;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class MineBuilding extends AbstractTownBuilding {

	public static final MapCodec<MineBuilding> MAP_CODEC = RecordCodecBuilder.mapCodec(t -> t.group(
                    strict(BlockPos.CODEC).optionalFieldOf("pos",BlockPos.ZERO).forGetter(o -> o.pos),
                    strict(Codec.BOOL).optionalFieldOf("initialized", false).forGetter(o -> o.isInitialized()),
                    strict(Codec.BOOL).optionalFieldOf("isStructureValid",false).forGetter(o -> o.isStructureValid()),
                    strict(OccupiedZoneInfo.CODEC).optionalFieldOf("occupiedVolume",OccupiedZoneInfo.EMPTY).forGetter(o -> o.getOccupiedVolume()),
					strict(Codec.DOUBLE).optionalFieldOf("rating",0D).forGetter(o -> o.getRating()),
					strict(Codec.STRING).optionalFieldOf("biomePath","minecraft:plains")
                            .forGetter(o -> o.getBiomePath().toString())
					)
			.apply(t, MineBuilding::new));
        public static final Codec<MineBuilding> CODEC = MAP_CODEC.codec();


	private static final Map<ResourceLocation, Map<Item,  Integer>> BIOME_RESOURCES = new HashMap<>();
    private static Object loadedRecipeManager;
    public static final Map<Item, Integer> DEFAULT_RESOURCES = Map.of(
            Items.COBBLESTONE, 1,
            Items.COAL, 1
    );

	private ResourceLocation biomePath = new ResourceLocation("minecraft", "plains");

	@Getter
	private double rating;//might be removed

	public void setBiomePath(ResourceLocation biomePath) {
	    if (java.util.Objects.equals(this.biomePath, biomePath)) {
	        return;
	    }
	    this.biomePath = biomePath;
	    fireChange();
	}
	public void setRating(double rating) {
	    if (Double.compare(this.rating, rating) == 0) {
	        return;
	    }
	    this.rating = rating;
	    fireChange();
	}

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
    public MineBuilding(BlockPos pos, boolean initialized, 
                        boolean isStructureValid, OccupiedZoneInfo occupiedVolume,
                        double rating, String biomePathString) {
        super(pos);
        this.setInitialized(initialized);
        this.setIsStructureValid(isStructureValid);
        this.internalSetOccupiedVolume(occupiedVolume);
        this.setRating(rating);
        ResourceLocation decodedBiome = ResourceLocation.tryParse(biomePathString);
        this.setBiomePath(decodedBiome == null
                ? new ResourceLocation("minecraft", "plains")
                : decodedBiome);
    }

	private static void loadBiomeResources() {
        var currentManager = CDistHelper.getRecipeManager();
        if (currentManager == loadedRecipeManager) {
            return;
        }
        BIOME_RESOURCES.clear();
		for(BiomeMineResourceRecipe recipe : CUtils.filterRecipes(currentManager, BiomeMineResourceRecipe.TYPE)){
			ResourceLocation biomeID = recipe.biomeID;
			Map<Item, Integer> weights = recipe.weights;
			BIOME_RESOURCES.put(biomeID, Map.copyOf(weights));
		}
        loadedRecipeManager = currentManager;
	}

    // The integrated server and its client share this cache, including reload invalidation.
	public static synchronized Map<Item, Integer> getWeights(ResourceLocation biomeID){
		if(loadedRecipeManager != CDistHelper.getRecipeManager()){
			loadBiomeResources();
		}
		if(BIOME_RESOURCES.containsKey(biomeID)){
			return BIOME_RESOURCES.get(biomeID);
		}
		return DEFAULT_RESOURCES;
	}

    public static synchronized void invalidateRecipeCache() {
        loadedRecipeManager = null;
        BIOME_RESOURCES.clear();
    }

    public static synchronized boolean hasBiomeRecipe(ResourceLocation biomeID) {
        if (loadedRecipeManager != CDistHelper.getRecipeManager()) {
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
