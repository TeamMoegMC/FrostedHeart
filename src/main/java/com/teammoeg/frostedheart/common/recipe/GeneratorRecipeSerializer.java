package com.teammoeg.frostedheart.common.recipe;

import blusunrize.immersiveengineering.api.IEApi;
import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHContent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ShapedRecipe;
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
        ItemStack input = readInput(json.get("input"));
        int time = JSONUtils.getInt(json, "time");
        return new GeneratorRecipe(recipeId, output, input, time);
    }

    protected ItemStack readInput(JsonElement inputObject) {
        if (inputObject.isJsonObject() && inputObject.getAsJsonObject().has("item"))
            return ShapedRecipe.deserializeItem(inputObject.getAsJsonObject());
        IngredientWithSize outgredient = IngredientWithSize.deserialize(inputObject);
        return IEApi.getPreferredStackbyMod(outgredient.getMatchingStacks());
    }

    @Nullable
    @Override
    public GeneratorRecipe read(ResourceLocation recipeId, PacketBuffer buffer) {
        ItemStack output = buffer.readItemStack();
        ItemStack input = ItemStack.read(buffer.readCompoundTag());
        int time = buffer.readInt();
        return new GeneratorRecipe(recipeId, output, input, time);
    }

    @Override
    public void write(PacketBuffer buffer, GeneratorRecipe recipe) {
        buffer.writeItemStack(recipe.output);
        recipe.input.write(buffer.readCompoundTag());
        buffer.writeInt(recipe.time);
    }
}
