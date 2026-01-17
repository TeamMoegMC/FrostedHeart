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
