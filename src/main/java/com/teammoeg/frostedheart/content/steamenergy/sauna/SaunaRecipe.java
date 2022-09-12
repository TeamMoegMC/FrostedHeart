package com.teammoeg.frostedheart.content.steamenergy.sauna;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IESerializableRecipe;
import blusunrize.immersiveengineering.api.utils.ItemUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.RegistryObject;

import java.util.Collections;
import java.util.Map;

public class SaunaRecipe extends IESerializableRecipe {
    public static IRecipeType<SaunaRecipe> TYPE;
    public static RegistryObject<IERecipeSerializer<SaunaRecipe>> SERIALIZER;
    public final Ingredient input;
    public final int time;
    public final Effect effect;
    public final int duration;
    public final int amplifier;
    public static Map<ResourceLocation, SaunaRecipe> recipeList = Collections.emptyMap();

    public SaunaRecipe(ResourceLocation id, Ingredient input, int time, Effect effect, int duration, int amplifier) {
        super(ItemStack.EMPTY, TYPE, id);
        this.input = input;
        this.time = time;
        this.effect = effect;
        this.duration = duration;
        this.amplifier = amplifier;
    }

    @Override
    protected IERecipeSerializer getIESerializer() {
        return SERIALIZER.get();
    }

    @Override
    public ItemStack getRecipeOutput() {
        return ItemStack.EMPTY;
    }

    public static SaunaRecipe findRecipe(ItemStack input) {
        for (SaunaRecipe recipe : recipeList.values())
            if (ItemUtils.stackMatchesObject(input, recipe.input))
                return recipe;
        return null;
    }
}
