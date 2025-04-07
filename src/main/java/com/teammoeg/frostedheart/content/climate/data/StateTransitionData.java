package com.teammoeg.frostedheart.content.climate.data;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.recipe.CodecRecipeSerializer;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * General specification for physical state transition
 * @param block the block to consider
 * @param state the current state of the block. For example, Water would be "liquid", and Ice would be "solid".
 *              This avoid certain updates when the block is already in certain state to save performance.
 * @param solid the solid state for the block
 * @param liquid the liquid state for the block
 * @param gas the gas state for the block
 * @param freezeTemp the temp below which the block goes to solid
 * @param meltTemp the temp above which the block goes to liquid
 * @param condenseTemp the temp below which the block goes to liquid
 * @param evaporateTemp the temp above which the block goes to gas
 * @param heatCapacity higher this is, less likely the transition happens. transition rate ~ 1 / heatCapacity
 * @param willTransit an overriding switch disallowing any transition, saves performance. in general this is true.
 */
public record StateTransitionData(Block block, String state,
                                  Block solid, Block liquid, Block gas,
                                  float freezeTemp, float meltTemp,
                                  float condenseTemp, float evaporateTemp,
                                  int heatCapacity, boolean willTransit){
    public static final Codec<StateTransitionData> CODEC= RecordCodecBuilder.create(t->t.group(
            ForgeRegistries.BLOCKS.getCodec().fieldOf("block").forGetter(o->o.block),
            Codec.STRING.optionalFieldOf("state","solid").forGetter(o->o.state),
            ForgeRegistries.BLOCKS.getCodec().fieldOf("solid").forGetter(o->o.solid),
            ForgeRegistries.BLOCKS.getCodec().fieldOf("liquid").forGetter(o->o.liquid),
            ForgeRegistries.BLOCKS.getCodec().fieldOf("gas").forGetter(o->o.gas),
            Codec.FLOAT.optionalFieldOf("freeze_temp",0f).forGetter(o->o.freezeTemp),
            Codec.FLOAT.optionalFieldOf("melt_temp",0f).forGetter(o->o.meltTemp),
            Codec.FLOAT.optionalFieldOf("condense_temp",0f).forGetter(o->o.condenseTemp),
            Codec.FLOAT.optionalFieldOf("evaporate_temp",0f).forGetter(o->o.evaporateTemp),
            Codec.INT.optionalFieldOf("heat_capacity",1).forGetter(o->o.heatCapacity),
            Codec.BOOL.optionalFieldOf("will_transit",false).forGetter(o->o.willTransit)).apply(t, StateTransitionData::new));
    public static RegistryObject<CodecRecipeSerializer<StateTransitionData>> TYPE;
    private static Map<Block,StateTransitionData> CACHE = ImmutableMap.of();

    @Nullable
    public static StateTransitionData getData(Block block) {
        return CACHE.get(block);
    }

    public static void updateCache(RecipeManager manager) {
        Collection<Recipe<?>> recipes = manager.getRecipes();
        StateTransitionData.CACHE = StateTransitionData.TYPE.get().filterRecipes(recipes).collect(Collectors.toMap(t->t.getData().block(), t->t.getData()));
    }

    public FinishedRecipe toFinished(ResourceLocation name) {
        return TYPE.get().toFinished(name, this);
    }
}
