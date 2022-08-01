package com.teammoeg.frostedheart.content.recipes;

import net.minecraft.network.PacketBuffer;

public class CampfireDefrostRecipeSerializer extends DefrostRecipeSerializer<CampfireDefrostRecipe> {

    public CampfireDefrostRecipeSerializer() {
        super(CampfireDefrostRecipe::new);
    }

    @Override
    public void write(PacketBuffer buffer, CampfireDefrostRecipe recipe) {
        super.write(buffer, recipe);
        buffer.writeFloat(recipe.getExperience());
        buffer.writeVarInt(recipe.getCookTime());
    }

}
