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

package com.teammoeg.frostedheart.content.climate.data;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.chorda.recipe.CodecRecipeSerializer;
import com.teammoeg.frostedheart.content.climate.PhysicalState;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * General specification for physical state transition
 *
 * Note: In some sense, some entries can be omitted bse on "state".
 *
 * @param block the block to consider
 * @param checked the current state of the block. For example, Water would be "liquid", and Ice would be "solid".
 *              This avoid certain updates when the block is already in certain state to save performance.
 * @param solid the solid state for the block
 * @param liquid the liquid state for the block
 * @param gas the gas state for the block
 * @param freezeTemp the temp below which the block goes to solid.
 *                   may be omitted if state is solid.
 * @param meltTemp the temp above which the block goes to liquid.
 *                 may be omitted if state is liquid or gas.
 * @param condenseTemp the temp below which the block goes to liquid.
 *                     may be omitted if state is liquid or solid.
 * @param evaporateTemp the temp above which the block goes to gas.
 *                      may be omitted if state is gas.
 * @param heatCapacity higher this is, less likely the transition happens. transition rate ~ 1 / heatCapacity
 * @param willTransit an overriding switch disallowing any transition, saves performance. in general this is true.
 */
public record StateTransitionData(BlockState block,boolean ignoreState, PhysicalState state,
		BlockState solid, BlockState liquid, BlockState gas,
                                  float freezeTemp, float meltTemp,
                                  float condenseTemp, float evaporateTemp,
                                  int heatCapacity, boolean willTransit){

    /** The first configured transition stage reached while heating this state. */
    public record HeatingTransition(
            float temperatureC,
            PhysicalState targetState,
            BlockState targetBlock
    ) {
        public HeatingTransition {
            if (!Float.isFinite(temperatureC) || targetState == null || targetBlock == null) {
                throw new IllegalArgumentException("heating transition fields are invalid");
            }
        }
    }
	
    public static final Codec<StateTransitionData> CODEC= RecordCodecBuilder.create(t->t.group(
    		BlockState.CODEC.optionalFieldOf("block").forGetter(o->Optional.ofNullable(o.block)),
    		Codec.BOOL.optionalFieldOf("ignoreState",true).forGetter(o->o.ignoreState),
            CodecUtil.enumCodec(PhysicalState.class).fieldOf("state").forGetter(o->o.state),
            BlockState.CODEC.optionalFieldOf("solid").forGetter(o->Optional.ofNullable(o.solid)),
            BlockState.CODEC.optionalFieldOf("liquid").forGetter(o->Optional.ofNullable(o.liquid)),
            BlockState.CODEC.optionalFieldOf("gas").forGetter(o->Optional.ofNullable(o.gas)),
            Codec.FLOAT.optionalFieldOf("freeze_temp",0f).forGetter(o->o.freezeTemp),
            Codec.FLOAT.optionalFieldOf("melt_temp",0f).forGetter(o->o.meltTemp),
            Codec.FLOAT.optionalFieldOf("condense_temp",0f).forGetter(o->o.condenseTemp),
            Codec.FLOAT.optionalFieldOf("evaporate_temp",0f).forGetter(o->o.evaporateTemp),
            Codec.INT.optionalFieldOf("heat_capacity",1).forGetter(o->o.heatCapacity),
            Codec.BOOL.optionalFieldOf("will_transit",false).forGetter(o->o.willTransit)).apply(t, StateTransitionData::new));
    
    public static RegistryObject<CodecRecipeSerializer<StateTransitionData>> TYPE;
    private static Map<BlockState,StateTransitionData> CACHE = ImmutableMap.of();
    StateTransitionData(Optional<BlockState> block,boolean ignoreState, PhysicalState state,
    		Optional<BlockState> solid, Optional<BlockState> liquid, Optional<BlockState> gas,
                                      float freezeTemp, float meltTemp,
                                      float condenseTemp, float evaporateTemp,
                                      int heatCapacity, boolean willTransit){
    	this(block.orElse(null),ignoreState,state,solid.orElse(null),liquid.orElse(null),gas.orElse(null),freezeTemp,meltTemp,condenseTemp,evaporateTemp,heatCapacity,willTransit);
    	
    }
    @Nullable
    public static StateTransitionData getData(BlockState block) {
        return CACHE.get(block);
    }

    /**
     * Returns the first hot-side stage at its onset temperature. A later stage
     * is compiled from the replacement BlockState, so each stage accounts for
     * its own latent energy. Equal solid thresholds keep the legacy gas-first
     * priority.
     */
    @Nullable
    public HeatingTransition heatingTransition(BlockState currentState) {
        if (currentState == null) {
            return null;
        }
        return switch (state) {
            case SOLID -> earlier(
                    candidate(evaporateTemp, PhysicalState.GAS, gas, currentState),
                    candidate(meltTemp, PhysicalState.LIQUID, liquid, currentState));
            case LIQUID -> candidate(
                    evaporateTemp, PhysicalState.GAS, gas, currentState);
            case GAS -> null;
        };
    }

    @Nullable
    private static HeatingTransition candidate(
            float temperatureC,
            PhysicalState targetState,
            BlockState targetBlock,
            BlockState currentState
    ) {
        if (!Float.isFinite(temperatureC)
                || targetBlock == null
                || targetBlock == currentState) {
            return null;
        }
        return new HeatingTransition(temperatureC, targetState, targetBlock);
    }

    @Nullable
    private static HeatingTransition earlier(
            HeatingTransition priority,
            HeatingTransition fallback
    ) {
        if (priority == null) {
            return fallback;
        }
        if (fallback == null || priority.temperatureC() <= fallback.temperatureC()) {
            return priority;
        }
        return fallback;
    }
    public Stream<Pair<BlockState,StateTransitionData>> getStates(){
    	if(!ignoreState)
    		return Stream.of(Pair.of(block, this));
    	Stream.Builder<Pair<BlockState,StateTransitionData>> builder=Stream.builder();
    	for(BlockState bs:block.getBlock().getStateDefinition().getPossibleStates()) {
    		builder.add(Pair.of(bs, this));
    	}
    	return builder.build();
    }
    public static void updateCache(RecipeManager manager) {
        Collection<Recipe<?>> recipes = manager.getRecipes();
        StateTransitionData.CACHE = StateTransitionData.TYPE.get().filterRecipes(recipes).flatMap(t->t.getData().getStates()).collect(Collectors.toMap(t->t.getFirst(), t->t.getSecond()));
    }

    public FinishedRecipe toFinished(ResourceLocation name) {
        return TYPE.get().toFinished(name, this);
    }
}
