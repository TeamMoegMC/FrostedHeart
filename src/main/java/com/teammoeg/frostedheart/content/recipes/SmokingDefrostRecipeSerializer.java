/*
 * Copyright (c) 2022 TeamMoeg
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

package com.teammoeg.frostedheart.content.recipes;

import net.minecraft.network.PacketBuffer;

public class SmokingDefrostRecipeSerializer extends DefrostRecipeSerializer<SmokingDefrostRecipe> {

    public SmokingDefrostRecipeSerializer() {
        super(SmokingDefrostRecipe::new);
    }

    @Override
    public void write(PacketBuffer buffer, SmokingDefrostRecipe recipe) {
        super.write(buffer, recipe);
        buffer.writeFloat(recipe.getExperience());
        buffer.writeVarInt(recipe.getCookTime());
    }

}
