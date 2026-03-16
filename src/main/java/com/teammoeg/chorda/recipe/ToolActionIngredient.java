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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 基于 Forge {@link ToolAction} 的自定义合成原料。
 * 允许在配方中使用工具动作（如挖掘、切割等）作为原料匹配条件，
 * 即匹配所有能执行指定工具动作的物品。构造时会自动从注册表中收集所有匹配物品。
 * 支持 JSON 和网络字节缓冲区两种序列化方式。
 * <p>
 * A custom ingredient based on Forge {@link ToolAction}.
 * Allows using tool actions (such as digging, cutting, etc.) as ingredient matching criteria
 * in recipes, matching all items that can perform the specified tool action. Automatically
 * collects all matching items from the registry during construction.
 * Supports both JSON and network byte buffer serialization.
 *
 * @see ToolActionIngredientSerializer
 */
public class ToolActionIngredient extends Ingredient {

	/** 默认的工具动作原料序列化器实例 / Default tool action ingredient serializer instance */
	public static final ToolActionIngredientSerializer SERIALIZER=new ToolActionIngredientSerializer(new ResourceLocation(Chorda.MODID,"tool"));

	/**
	 * 工具动作原料的序列化器，负责 JSON 和网络字节缓冲区的读写。
	 * <p>
	 * Serializer for tool action ingredients, responsible for JSON and network byte buffer read/write.
	 *
	 * @param name 序列化器的资源位置标识符 / The resource location identifier of this serializer
	 */
	public static record ToolActionIngredientSerializer(ResourceLocation name) implements IIngredientSerializer<ToolActionIngredient>{

		/**
		 * 从网络字节缓冲区解析工具动作原料。
		 * <p>
		 * Parses a tool action ingredient from a network byte buffer.
		 *
		 * @param buffer 网络字节缓冲区 / The network byte buffer
		 * @return 解析后的工具动作原料实例 / The parsed tool action ingredient instance
		 */
		@Override
		public ToolActionIngredient parse(FriendlyByteBuf buffer) {
			return new ToolActionIngredient(this,ToolAction.get(buffer.readUtf()));
		}

		/**
		 * 从 JSON 对象解析工具动作原料。
		 * <p>
		 * Parses a tool action ingredient from a JSON object.
		 *
		 * @param json 包含工具动作信息的 JSON 对象 / The JSON object containing tool action information
		 * @return 解析后的工具动作原料实例 / The parsed tool action ingredient instance
		 */
		@Override
		public ToolActionIngredient parse(JsonObject json) {
			return new ToolActionIngredient(this,ToolAction.get(json.get("tool").getAsString()));
		}

		/**
		 * 将工具动作原料写入网络字节缓冲区。
		 * <p>
		 * Writes a tool action ingredient to a network byte buffer.
		 *
		 * @param buffer 网络字节缓冲区 / The network byte buffer
		 * @param ingredient 要写入的工具动作原料 / The tool action ingredient to write
		 */
		@Override
		public void write(FriendlyByteBuf buffer, ToolActionIngredient ingredient) {
			buffer.writeUtf(ingredient.tool.name());

		}

	}

	/** 用于匹配的工具动作 / The tool action used for matching */
	ToolAction tool;

	/** 此原料使用的序列化器 / The serializer used by this ingredient */
	ToolActionIngredientSerializer serializer;

	/**
	 * 使用指定的序列化器和工具动作构造工具动作原料。
	 * 构造时会自动扫描所有已注册物品，收集能执行指定工具动作的物品作为匹配候选。
	 * <p>
	 * Constructs a tool action ingredient with the specified serializer and tool action.
	 * During construction, automatically scans all registered items and collects those
	 * that can perform the specified tool action as matching candidates.
	 *
	 * @param serializer 序列化器实例 / The serializer instance
	 * @param tool 用于匹配的工具动作 / The tool action used for matching
	 */
	public ToolActionIngredient(ToolActionIngredientSerializer serializer,ToolAction tool) {
		super(ForgeRegistries.ITEMS.getValues().stream().map(ItemStack::new).filter(t->t.canPerformAction( tool)).map(ItemValue::new));
		this.tool=tool;
		this.serializer=serializer;
	}

	/**
	 * 测试给定的物品堆是否能执行此工具动作。
	 * <p>
	 * Tests whether the given item stack can perform this tool action.
	 *
	 * @param pStack 要测试的物品堆 / The item stack to test
	 * @return 如果物品能执行此工具动作则返回 true / Returns true if the item can perform this tool action
	 */
	@Override
	public boolean test(ItemStack pStack) {
		return pStack.canPerformAction(tool);
	}

	/**
	 * 将此工具动作原料序列化为 JSON 元素。
	 * <p>
	 * Serializes this tool action ingredient to a JSON element.
	 *
	 * @return 包含类型和工具动作信息的 JSON 对象 / A JSON object containing type and tool action information
	 */
	@Override
	public JsonElement toJson() {
		JsonObject json = new JsonObject();
		json.addProperty("type", getSerializer().name.toString());
		json.addProperty("tool", tool.name());
		return json;
	}

	/**
	 * 获取此原料的序列化器。
	 * <p>
	 * Gets the serializer for this ingredient.
	 *
	 * @return 工具动作原料序列化器 / The tool action ingredient serializer
	 */
	@Override
	public ToolActionIngredientSerializer getSerializer() {
		return serializer;
	}

}
