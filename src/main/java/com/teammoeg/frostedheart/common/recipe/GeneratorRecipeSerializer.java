package com.teammoeg.frostedheart.common.recipe;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHContent;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

public class GeneratorRecipeSerializer extends IERecipeSerializer<GeneratorRecipe> {
    @Override
    public ItemStack getIcon() {
        return new ItemStack(FHContent.Multiblocks.generator);
    }

    @Override
    public GeneratorRecipe readFromJson(ResourceLocation recipeId, JsonObject json) {
        ItemStack output = readOutput(json.get("result"));
        IngredientWithSize input = IngredientWithSize.deserialize(json.get("input"));
        int time = JSONUtils.getInt(json, "time");
        return new GeneratorRecipe(recipeId, output, input, time);
    }

    @Nullable
    @Override
    public GeneratorRecipe read(ResourceLocation recipeId, PacketBuffer buffer) {
        ItemStack output = buffer.readItemStack();
        IngredientWithSize input = IngredientWithSize.read(buffer);
        int time = buffer.readInt();
        return new GeneratorRecipe(recipeId, output, input, time);
    }

    @Override
    public void write(PacketBuffer buffer, GeneratorRecipe recipe) {
        buffer.writeItemStack(recipe.output);
        recipe.input.write(buffer);
        buffer.writeInt(recipe.time);
    }
}
