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

package com.teammoeg.frostedheart.content.steamenergy.sauna;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import com.google.gson.JsonObject;
import com.mojang.realmsclient.util.JsonUtils;
import com.teammoeg.frostedheart.FHBlocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;

public class SaunaSerializer extends IERecipeSerializer<SaunaRecipe> {

    public SaunaSerializer() {
    }

    @Override
    public SaunaRecipe read(ResourceLocation recipeId, PacketBuffer buffer) {
        // read effect from buffer
        CompoundNBT effectNBT = buffer.readCompoundTag();
        Effect effect = null;
        int duration = 0;
        int amplifier = 0;
        if (effectNBT.contains("Id")) {
            effect = Effect.get(effectNBT.getInt("Id"));
            duration = effectNBT.getInt("Duration");
            amplifier = effectNBT.getInt("Amplifier");
        }
        // read time from buffer
        int time = buffer.readInt();
        // read ingredient from buffer
        Ingredient input = Ingredient.read(buffer);
        return new SaunaRecipe(recipeId, input, time, effect, duration, amplifier);
    }

    @Override
    public void write(PacketBuffer buffer, SaunaRecipe recipe) {
        // write effect to buffer
        CompoundNBT effectNBT = new CompoundNBT();
        if (recipe.effect != null) {
            effectNBT.putInt("Id", Effect.getId(recipe.effect));
            effectNBT.putInt("Duration", recipe.duration);
            effectNBT.putInt("Amplifier", recipe.amplifier);
        }
        buffer.writeCompoundTag(effectNBT);
        // write time to buffer
        buffer.writeInt(recipe.time);
        // write ingredient to buffer
        recipe.input.write(buffer);
    }

    @Override
    public ItemStack getIcon() {
        return new ItemStack(FHBlocks.sauna.asItem());
    }

    @Override
    public SaunaRecipe readFromJson(ResourceLocation id, JsonObject json) {
        // read effect from json
        Effect effect = null;
        int duration = 0, amplifier = 0;
        if (json.has("effect")) {
            JsonObject effectJson = JSONUtils.getJsonObject(json, "effect");
            ResourceLocation effectID = new ResourceLocation(JSONUtils.getString(effectJson, "id"));
            duration = JSONUtils.getInt(effectJson, "duration");
            amplifier = JSONUtils.getInt(effectJson, "amplifier");
            // Get Effect from effectID from Registry
            if (Registry.EFFECTS.getOrDefault(effectID) != null) {
                effect = Registry.EFFECTS.getOrDefault(effectID);
            }
        }
        return new SaunaRecipe(id, Ingredient.deserialize(json.get("input")), JSONUtils.getInt(json, "time"),
                effect, duration, amplifier);
    }

}
