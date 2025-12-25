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

public class ToolActionIngredient extends Ingredient {
	public static final ToolActionIngredientSerializer SERIALIZER=new ToolActionIngredientSerializer(new ResourceLocation(Chorda.MODID,"tool"));
	public static record ToolActionIngredientSerializer(ResourceLocation name) implements IIngredientSerializer<ToolActionIngredient>{
		
		@Override
		public ToolActionIngredient parse(FriendlyByteBuf buffer) {
			return new ToolActionIngredient(this,ToolAction.get(buffer.readUtf()));
		}

		@Override
		public ToolActionIngredient parse(JsonObject json) {
			return new ToolActionIngredient(this,ToolAction.get(json.get("tool").getAsString()));
		}

		@Override
		public void write(FriendlyByteBuf buffer, ToolActionIngredient ingredient) {
			buffer.writeUtf(ingredient.tool.name());
			
		}
		
	}
	ToolAction tool;
	ToolActionIngredientSerializer serializer;
	public ToolActionIngredient(ToolActionIngredientSerializer serializer,ToolAction tool) {
		super(ForgeRegistries.ITEMS.getValues().stream().map(ItemStack::new).filter(t->t.canPerformAction( tool)).map(ItemValue::new));
		this.tool=tool;
		this.serializer=serializer;
	}

	@Override
	public boolean test(ItemStack pStack) {
		return pStack.canPerformAction(tool);
	}

	@Override
	public JsonElement toJson() {
		JsonObject json = new JsonObject();
		json.addProperty("type", getSerializer().name.toString());
		json.addProperty("tool", tool.name());
		return json;
	}

	@Override
	public ToolActionIngredientSerializer getSerializer() {
		return serializer;
	}

}
