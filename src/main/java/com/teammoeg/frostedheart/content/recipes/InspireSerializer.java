package com.teammoeg.frostedheart.content.recipes;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

public class InspireSerializer extends IERecipeSerializer<InspireRecipe> {

    public InspireSerializer() {
    }

    @Override
    public InspireRecipe read(ResourceLocation recipeId, PacketBuffer buffer) {
        return new InspireRecipe(recipeId, Ingredient.read(buffer), buffer.readVarInt());
    }

    @Override
    public void write(PacketBuffer buffer, InspireRecipe recipe) {
        recipe.item.write(buffer);
        buffer.writeVarInt(recipe.inspire);
    }

    @Override
    public ItemStack getIcon() {
        return new ItemStack(Items.PAPER);
    }

    @Override
    public InspireRecipe readFromJson(ResourceLocation arg0, JsonObject arg1) {
        return new InspireRecipe(arg0, Ingredient.deserialize(arg1.get("item")), arg1.get("amount").getAsInt());
    }

}
