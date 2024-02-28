/*
 * Copyright (c) 2022-2024 TeamMoeg
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

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHBlocks;
import com.teammoeg.frostedheart.util.RegistryUtils;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IESerializableRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.potion.Effect;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.RegistryObject;

public class SaunaRecipe extends IESerializableRecipe {
    public static class Serializer extends IERecipeSerializer<SaunaRecipe> {


        @Override
        public ItemStack getIcon() {
            return new ItemStack(FHBlocks.sauna.get().asItem());
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
                effect = RegistryUtils.getEffect(effectID);
            }
            return new SaunaRecipe(id, Ingredient.deserialize(json.get("input")), JSONUtils.getInt(json, "time"),
                    effect, duration, amplifier);
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

    }
    public static IRecipeType<SaunaRecipe> TYPE;
    public static RegistryObject<IERecipeSerializer<SaunaRecipe>> SERIALIZER;
    public final Ingredient input;
    public final int time;
    public final Effect effect;
    public final int duration;

    public final int amplifier;


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
}
