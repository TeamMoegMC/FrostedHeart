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

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IERecipeTypes.TypeWithClass;
import blusunrize.immersiveengineering.api.crafting.IESerializableRecipe;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.util.RegistryUtils;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.crafting.conditions.ICondition.IContext;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.registries.RegistryObject;

public class SaunaRecipe extends IESerializableRecipe {
    public static RegistryObject<RecipeType<SaunaRecipe>> TYPE;
    public static Lazy<TypeWithClass<SaunaRecipe>> IEType = Lazy.of(() -> new TypeWithClass<>(TYPE, SaunaRecipe.class));
    public static RegistryObject<IERecipeSerializer<SaunaRecipe>> SERIALIZER;
    public final Ingredient input;
    public final int time;
    public final MobEffect effect;
    public final int duration;
    public final int amplifier;

    public SaunaRecipe(ResourceLocation id, Ingredient input, int time, MobEffect effect, int duration, int amplifier) {
        super(Lazy.of(() -> ItemStack.EMPTY), IEType.get(), id);
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
    public ItemStack getResultItem(RegistryAccess ra) {
        return ItemStack.EMPTY;
    }

    public static class Serializer extends IERecipeSerializer<SaunaRecipe> {


        @Override
        public ItemStack getIcon() {
            return new ItemStack(FHBlocks.SAUNA_VENT.get().asItem());
        }

        @Override
        public SaunaRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            // read effect from buffer
            CompoundTag effectNBT = buffer.readNbt();
            MobEffect effect = null;
            int duration = 0;
            int amplifier = 0;
            if (effectNBT.contains("Id")) {
                effect = MobEffect.byId(effectNBT.getInt("Id"));
                duration = effectNBT.getInt("Duration");
                amplifier = effectNBT.getInt("Amplifier");
            }
            // read time from buffer
            int time = buffer.readInt();
            // read ingredient from buffer
            Ingredient input = Ingredient.fromNetwork(buffer);
            return new SaunaRecipe(recipeId, input, time, effect, duration, amplifier);
        }

        @Override
        public SaunaRecipe readFromJson(ResourceLocation id, JsonObject json, IContext ctx) {
            // read effect from json
            MobEffect effect = null;
            int duration = 0, amplifier = 0;
            if (json.has("effect")) {
                JsonObject effectJson = GsonHelper.getAsJsonObject(json, "effect");
                ResourceLocation effectID = new ResourceLocation(GsonHelper.getAsString(effectJson, "id"));
                duration = GsonHelper.getAsInt(effectJson, "duration");
                amplifier = GsonHelper.getAsInt(effectJson, "amplifier");
                // Get Effect from effectID from Registry
                effect = RegistryUtils.getEffect(effectID);
            }
            return new SaunaRecipe(id, Ingredient.fromJson(json.get("input")), GsonHelper.getAsInt(json, "time"),
                    effect, duration, amplifier);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, SaunaRecipe recipe) {
            // write effect to buffer
            CompoundTag effectNBT = new CompoundTag();
            if (recipe.effect != null) {
                effectNBT.putInt("Id", MobEffect.getId(recipe.effect));
                effectNBT.putInt("Duration", recipe.duration);
                effectNBT.putInt("Amplifier", recipe.amplifier);
            }
            buffer.writeNbt(effectNBT);
            // write time to buffer
            buffer.writeInt(recipe.time);
            // write ingredient to buffer
            recipe.input.toNetwork(buffer);
        }

    }
}
