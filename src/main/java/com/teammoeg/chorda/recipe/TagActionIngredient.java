/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.chorda.recipe;

import java.util.stream.Stream;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teammoeg.chorda.Chorda;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import net.minecraftforge.registries.ForgeRegistries;

public class TagActionIngredient extends Ingredient {
	public static final TagActionIngredientSerializer SERIALIZER=new TagActionIngredientSerializer(new ResourceLocation(Chorda.MODID,"tag"));
	public static record TagActionIngredientSerializer(ResourceLocation name) implements IIngredientSerializer<TagActionIngredient>{
		
		@Override
		public TagActionIngredient parse(FriendlyByteBuf buffer) {
			return new TagActionIngredient(this,ItemTags.create(buffer.readResourceLocation()));
		}

		@Override
		public TagActionIngredient parse(JsonObject json) {
			return new TagActionIngredient(this,ItemTags.create(new ResourceLocation(json.get("tag").getAsString())));
		}

		@Override
		public void write(FriendlyByteBuf buffer, TagActionIngredient ingredient) {
			buffer.writeResourceLocation(ingredient.tool.location());
			
		}
		
	}
	TagKey<Item> tool;
	TagActionIngredientSerializer serializer;
	public TagActionIngredient(TagActionIngredientSerializer serializer,TagKey<Item> tool) {
		super(Stream.of(new TagValue(tool)));
		this.tool=tool;
		this.serializer=serializer;
	}

	@Override
	public boolean test(ItemStack pStack) {
		return pStack.is(tool);
	}

	@Override
	public JsonElement toJson() {
		JsonObject json = new JsonObject();
		json.addProperty("type", getSerializer().name.toString());
		json.addProperty("tag", tool.location().toString());
		return json;
	}

	@Override
	public TagActionIngredientSerializer getSerializer() {
		return serializer;
	}

}
