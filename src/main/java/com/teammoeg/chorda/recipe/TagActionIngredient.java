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

/**
 * 基于物品标签（Tag）的自定义合成原料。
 * 允许在配方中使用物品标签作为原料匹配条件，即匹配属于指定标签的所有物品。
 * 支持 JSON 和网络字节缓冲区两种序列化方式。
 * <p>
 * A custom ingredient based on item tags.
 * Allows using item tags as ingredient matching criteria in recipes, matching all items
 * belonging to the specified tag. Supports both JSON and network byte buffer serialization.
 *
 * @see TagActionIngredientSerializer
 */
public class TagActionIngredient extends Ingredient {

	/** 默认的标签原料序列化器实例 / Default tag ingredient serializer instance */
	public static final TagActionIngredientSerializer SERIALIZER=new TagActionIngredientSerializer(new ResourceLocation(Chorda.MODID,"tag"));

	/**
	 * 标签动作原料的序列化器，负责 JSON 和网络字节缓冲区的读写。
	 * <p>
	 * Serializer for tag action ingredients, responsible for JSON and network byte buffer read/write.
	 *
	 * @param name 序列化器的资源位置标识符 / The resource location identifier of this serializer
	 */
	public static record TagActionIngredientSerializer(ResourceLocation name) implements IIngredientSerializer<TagActionIngredient>{

		/**
		 * 从网络字节缓冲区解析标签原料。
		 * <p>
		 * Parses a tag ingredient from a network byte buffer.
		 *
		 * @param buffer 网络字节缓冲区 / The network byte buffer
		 * @return 解析后的标签原料实例 / The parsed tag ingredient instance
		 */
		@Override
		public TagActionIngredient parse(FriendlyByteBuf buffer) {
			return new TagActionIngredient(this,ItemTags.create(buffer.readResourceLocation()));
		}

		/**
		 * 从 JSON 对象解析标签原料。
		 * <p>
		 * Parses a tag ingredient from a JSON object.
		 *
		 * @param json 包含标签信息的 JSON 对象 / The JSON object containing tag information
		 * @return 解析后的标签原料实例 / The parsed tag ingredient instance
		 */
		@Override
		public TagActionIngredient parse(JsonObject json) {
			return new TagActionIngredient(this,ItemTags.create(new ResourceLocation(json.get("tag").getAsString())));
		}

		/**
		 * 将标签原料写入网络字节缓冲区。
		 * <p>
		 * Writes a tag ingredient to a network byte buffer.
		 *
		 * @param buffer 网络字节缓冲区 / The network byte buffer
		 * @param ingredient 要写入的标签原料 / The tag ingredient to write
		 */
		@Override
		public void write(FriendlyByteBuf buffer, TagActionIngredient ingredient) {
			buffer.writeResourceLocation(ingredient.tool.location());

		}

	}

	/** 用于匹配的物品标签键 / The item tag key used for matching */
	TagKey<Item> tool;

	/** 此原料使用的序列化器 / The serializer used by this ingredient */
	TagActionIngredientSerializer serializer;

	/**
	 * 使用指定的序列化器和物品标签构造标签原料。
	 * <p>
	 * Constructs a tag ingredient with the specified serializer and item tag.
	 *
	 * @param serializer 序列化器实例 / The serializer instance
	 * @param tool 用于匹配的物品标签键 / The item tag key used for matching
	 */
	public TagActionIngredient(TagActionIngredientSerializer serializer,TagKey<Item> tool) {
		super(Stream.of(new TagValue(tool)));
		this.tool=tool;
		this.serializer=serializer;
	}

	/**
	 * 测试给定的物品堆是否属于此标签。
	 * <p>
	 * Tests whether the given item stack belongs to this tag.
	 *
	 * @param pStack 要测试的物品堆 / The item stack to test
	 * @return 如果物品属于此标签则返回 true / Returns true if the item belongs to this tag
	 */
	@Override
	public boolean test(ItemStack pStack) {
		return pStack.is(tool);
	}

	/**
	 * 将此标签原料序列化为 JSON 元素。
	 * <p>
	 * Serializes this tag ingredient to a JSON element.
	 *
	 * @return 包含类型和标签信息的 JSON 对象 / A JSON object containing type and tag information
	 */
	@Override
	public JsonElement toJson() {
		JsonObject json = new JsonObject();
		json.addProperty("type", getSerializer().name.toString());
		json.addProperty("tag", tool.location().toString());
		return json;
	}

	/**
	 * 获取此原料的序列化器。
	 * <p>
	 * Gets the serializer for this ingredient.
	 *
	 * @return 标签原料序列化器 / The tag ingredient serializer
	 */
	@Override
	public TagActionIngredientSerializer getSerializer() {
		return serializer;
	}

}
