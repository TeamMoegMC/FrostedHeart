package com.teammoeg.frostedheart.content.recipes;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IESerializableRecipe;
import com.google.common.collect.ImmutableList;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.RegistryObject;

import java.util.List;

public class InspireRecipe extends IESerializableRecipe {
    public static IRecipeType<InspireRecipe> TYPE;
    public static RegistryObject<IERecipeSerializer<InspireRecipe>> SERIALIZER;
    public Ingredient item;
    public int inspire;
    public static List<InspireRecipe> recipes = ImmutableList.of();

    public InspireRecipe(ResourceLocation id, Ingredient item, int inspire) {
        super(ItemStack.EMPTY, TYPE, id);
        this.item = item;
        this.inspire = inspire;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return ItemStack.EMPTY;
    }

    @Override
    protected IERecipeSerializer getIESerializer() {
        return SERIALIZER.get();
    }

}
