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

package com.teammoeg.frostedheart.recipes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.util.RegistryUtils;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IESerializableRecipe;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.RegistryObject;
import top.theillusivec4.diet.api.IDietGroup;

public class DietValueRecipe extends IESerializableRecipe {
	/*public static final Codec<DietValueRecipe> CODEC=RecordCodecBuilder.create(
		t->t.group(Registry.ITEM
			.fieldOf("item")
			.forGetter(o->o.item),
			SerializeUtil.mapCodec(Codec.STRING,Codec.FLOAT)
			.fieldOf("groups")
			.forGetter(o->o.groups)
		).apply(t, DietValueRecipe::new));*/
    public static class Serializer extends IERecipeSerializer<DietValueRecipe> {
        @Override
        public ItemStack getIcon() {
            return ItemStack.EMPTY;
        }
        @Override
        public DietValueRecipe read(ResourceLocation recipeId, PacketBuffer buffer) {
            return new DietValueRecipe(recipeId, DietGroupCodec.read(buffer), buffer.readRegistryId());
        }
        @Override
        public DietValueRecipe readFromJson(ResourceLocation id, JsonObject json) {
            Map<String, Float> m = json.get("groups").getAsJsonObject().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getAsFloat()));
            Item i = RegistryUtils.getItem(new ResourceLocation(json.get("item").getAsString()));
            if (i == null || i == Items.AIR)
                return null;
            return new DietValueRecipe(id, m, i);
        }
        @Override
        public void write(PacketBuffer buffer, DietValueRecipe recipe) {
            DietGroupCodec.write(buffer, recipe.groups);
            buffer.writeRegistryId(recipe.item);
        }
    }
    public static IRecipeType<DietValueRecipe> TYPE;
    public static RegistryObject<IERecipeSerializer<DietValueRecipe>> SERIALIZER;
    public static Map<Item, DietValueRecipe> recipeList = Collections.emptyMap();
    
    final Map<String, Float> groups;
    Map<IDietGroup, Float> cache;
    public final Item item;

    public DietValueRecipe(ResourceLocation id, Item it) {
        this(id, new HashMap<>(), it);
    }
    public DietValueRecipe(ResourceLocation id, Map<String, Float> groups, Item it) {
        super(ItemStack.EMPTY, TYPE, id);
        this.groups = groups;
        this.item = it;
    }
   /* public DietValueRecipe(Item it, Map<String, Float> groups) {
        super(ItemStack.EMPTY, TYPE, null);
        this.groups = groups;
        this.item = it;
    }
*/
    @Override
    protected IERecipeSerializer<DietValueRecipe> getIESerializer() {
        return SERIALIZER.get();
    }

    @Override
    public ItemStack getRecipeOutput() {
        return ItemStack.EMPTY;
    }

    public Map<IDietGroup, Float> getValues() {
        if (cache == null)
            cache = groups.entrySet().stream().collect(Collectors.toMap(e -> DietGroupCodec.getGroup(e.getKey()), Map.Entry::getValue));
        return cache;
    }

    @Override
    public String toString() {
        return "DietValueRecipe [groups=" + groups + ", item=" + item + "]";
    }

}
