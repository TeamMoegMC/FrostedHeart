package com.teammoeg.frostedheart.research.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.util.SerializeUtil;
import com.teammoeg.frostedheart.util.Writeable;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ImageIcon;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

public class FHIcons {
	public static abstract class FHIcon extends Icon implements Writeable{
		public FHIcon() {
			super();
		}
	}
	private static class FHNopIcon extends FHIcon{
		public static final FHNopIcon INSTANCE=new FHNopIcon();
		/**
		 * @param e  
		 */
		public static FHNopIcon get(JsonElement e) {
			return INSTANCE;
		}
		/**
		 * @param e  
		 */
		public static FHNopIcon get(PacketBuffer e) {
			return INSTANCE;
		}
		private FHNopIcon() {
		}
		@Override
		public JsonElement serialize() {
			return JsonNull.INSTANCE;
		}

		@Override
		public void write(PacketBuffer buffer) {
			buffer.writeVarInt(0);
		}

		@Override
		public void draw(MatrixStack ms, int x, int y, int w, int h) {
		}
		
	}
	private static class FHItemIcon extends FHIcon{
		Icon nested;
		String item;
		ItemStack stack;
		public FHItemIcon(JsonElement elm) {
			if(elm.isJsonPrimitive()) {
				item=elm.getAsString();
				
			}else if(elm.isJsonObject()) {
				item=elm.getAsJsonObject().get("item").getAsString();
			}
			init();
		}
		public FHItemIcon(PacketBuffer buffer) {
			item=SerializeUtil.readOptional(buffer,PacketBuffer::readString).orElse(null);
			stack=SerializeUtil.readOptional(buffer,PacketBuffer::readItemStack).orElse(null);
			init();
		}
		public FHItemIcon(ItemStack stack) {
			this.stack=stack;
			init();
		}
		public FHItemIcon(String item) {
			this.item=item;
			init();
		}
		private void init() {
			if(item!=null)
				nested=ItemIcon.getItemIcon(item);
			else if(stack!=null)
				nested=ItemIcon.getItemIcon(stack);
		}
		@Override
		public void draw(MatrixStack ms, int x, int y, int w, int h) {
			nested.draw(ms, x, y, w, h);
		}

		@Override
		public JsonElement serialize() {
			if(item!=null)
				return new JsonPrimitive(item);
			StringBuilder sb=new StringBuilder();
			sb.append(stack.getItem().getRegistryName());
			sb.append(" ");
			sb.append(stack.getCount());
			sb.append(" ");
			sb.append(stack.getDamage());
			return new JsonPrimitive(sb.toString());
		}

		@Override
		public void write(PacketBuffer buffer) {
			buffer.writeVarInt(1);
			SerializeUtil.writeOptional2(buffer, item,PacketBuffer::writeString);
			SerializeUtil.writeOptional2(buffer, stack,PacketBuffer::writeItemStack);
		}
	}
	private static class FHCombinedIcon extends FHIcon{
		FHIcon large;
		FHIcon small;
		public FHCombinedIcon(JsonElement elm) {
			if(elm.isJsonObject()) {
				large=FHIcons.getIcon(elm.getAsJsonObject().get("base"));
				small=FHIcons.getIcon(elm.getAsJsonObject().get("small"));
			}
		}
		public FHCombinedIcon(PacketBuffer buffer) {
			large=FHIcons.readIcon(buffer);
			small=FHIcons.readIcon(buffer);
		}
		public FHCombinedIcon(FHIcon base, FHIcon small) {
			this.large=base;
			this.small=small;
		}
		@Override
		public void draw(MatrixStack ms, int x, int y, int w, int h) {
			large.draw(ms, x, y, w, h);
			small.draw(ms, x+w/2, y+h/2, w/2, h/2);
		}

		@Override
		public JsonElement serialize() {
			JsonObject jo=new JsonObject();
			jo.addProperty("type","combined");
			jo.add("base",large.serialize());
			jo.add("small",small.serialize());
			return jo;
		}

		@Override
		public void write(PacketBuffer buffer) {
			buffer.writeVarInt(2);
			large.write(buffer);
			small.write(buffer);
		}
	}
	private static class FHAnimatedIcon extends FHIcon{
		List<FHIcon> icons;
		public FHAnimatedIcon(JsonElement elm) {
			if(elm.isJsonObject()) {
				JsonObject jo=elm.getAsJsonObject();
				icons=SerializeUtil.parseJsonElmList(jo.get("icons"),FHIcons::getIcon);
			}else if(elm.isJsonArray()){
				icons=SerializeUtil.parseJsonElmList(elm,FHIcons::getIcon);
			}
		}
		public FHAnimatedIcon(PacketBuffer buffer) {
			icons=SerializeUtil.readList(buffer,FHIcons::readIcon);
		}
		public FHAnimatedIcon() {
			icons=new ArrayList<>();
		}
		public FHAnimatedIcon(FHIcon[] icons2) {
			for(FHIcon i:icons2)
				icons.add(i);
		}
		@Override
		public void draw(MatrixStack ms, int x, int y, int w, int h) {
			if(icons.size()>0)
			icons.get((int) ((System.currentTimeMillis()/1000)%icons.size())).draw(ms, x, y, w, h);
		}

		@Override
		public JsonElement serialize() {
			
			return SerializeUtil.toJsonList(icons,FHIcon::serialize);
		}

		@Override
		public void write(PacketBuffer buffer) {
			buffer.writeVarInt(3);
			SerializeUtil.writeList(buffer,icons,FHIcon::write);
		}
	}
	private static class FHIngredientIcon extends FHAnimatedIcon{
		Ingredient igd;
		public FHIngredientIcon(JsonElement elm) {
			this(Ingredient.deserialize(elm.getAsJsonObject().get("ingredient")));
		}
		public FHIngredientIcon(PacketBuffer buffer) {
			this(Ingredient.read(buffer));
			
		}
		public FHIngredientIcon(Ingredient i) {
			igd=i;
			for(ItemStack stack:igd.getMatchingStacks())
				icons.add(new FHItemIcon(stack));
		}
		@Override
		public JsonElement serialize() {
			JsonObject jo=new JsonObject();
			jo.addProperty("type","ingredient");
			jo.add("ingredient", igd.serialize());
			return jo;
		}

		@Override
		public void write(PacketBuffer buffer) {
			buffer.writeVarInt(4);
			igd.write(buffer);
		}
	}
	private static class FHTextureIcon extends FHIcon{
		Icon nested;
		ResourceLocation rl;
		public FHTextureIcon(JsonElement elm) {
			this(new ResourceLocation(elm.getAsJsonObject().get("location").getAsString()));
		}
		public FHTextureIcon(PacketBuffer buffer) {
			this(buffer.readResourceLocation());
		}
		public FHTextureIcon(ResourceLocation rl) {
			this.rl=rl;
			nested=ImageIcon.getIcon(rl);
		}
		@Override
		public void draw(MatrixStack ms, int x, int y, int w, int h) {
			nested.draw(ms, x, y, w, h);
		}

		@Override
		public JsonElement serialize() {
			JsonObject jo=new JsonObject();
			jo.addProperty("type","texture");
			jo.addProperty("location",rl.toString());
			return jo;
		}

		@Override
		public void write(PacketBuffer buffer) {
			buffer.writeVarInt(5);
			buffer.writeResourceLocation(rl);
		}
	}
	public static final Map<String,Function<JsonElement,FHIcon>> JsonIcon=new HashMap<>();
	public static final List<Function<PacketBuffer,FHIcon>> BufferIcon=new ArrayList<>();
	static {
		JsonIcon.put("nop",FHNopIcon::get);
		JsonIcon.put("item",FHItemIcon::new);
		JsonIcon.put("combined",FHCombinedIcon::new);
		JsonIcon.put("animated",FHAnimatedIcon::new);
		JsonIcon.put("ingredient",FHIngredientIcon::new);
		JsonIcon.put("texture",FHTextureIcon::new);
		BufferIcon.add(FHNopIcon::get);
		BufferIcon.add(FHItemIcon::new);
		BufferIcon.add(FHCombinedIcon::new);
		BufferIcon.add(FHAnimatedIcon::new);
		BufferIcon.add(FHIngredientIcon::new);
		BufferIcon.add(FHTextureIcon::new);
		
	}
	/**
	 * get Icon from ItemStack
	 * Would discard item NBT when serialize to json
	 * To preserve NBT, use {@link #getIcon(String)}
	 * */
	public static FHIcon getIcon(ItemStack item) {
		return new FHItemIcon(item);
	}
	/**
	 * get Item Icon from ItemStack String, serialize safe
	 * */
	public static FHIcon getIcon(String item) {
		return new FHItemIcon(item);
	}
	/**
	 * get Item Icon of combined base and small at the bottom right corner
	 * */
	public static FHIcon getIcon(FHIcon base,FHIcon small) {
		return new FHCombinedIcon(base,small);
	}
	public static FHIcon getIcon(Ingredient i) {
		return new FHIngredientIcon(i);
	}
	public static FHIcon getIcon(ResourceLocation texture) {
		return new FHTextureIcon(texture);
	}
	public static FHIcon getAnimatedIcon(FHIcon... icons) {
		return new FHAnimatedIcon(icons);
	}
	public static FHIcon getIcon(List<ItemStack> rewards) {
		return new FHIngredientIcon(Ingredient.fromStacks(rewards.stream()));
	}
	public static FHIcon getIcon(JsonElement elm) {
		if(elm==null||elm.isJsonNull()) {
			return FHNopIcon.INSTANCE;
		}if(elm.isJsonPrimitive()) {
			return new FHItemIcon(elm);
		}else if(elm.isJsonArray()) {
			return new FHAnimatedIcon(elm);
		}else if(elm.isJsonObject()){
			Function<JsonElement, FHIcon> func=JsonIcon.get("type");
			if(func!=null)
				return func.apply(elm);
		}
		return FHNopIcon.INSTANCE;
	}
	public static FHIcon readIcon(PacketBuffer elm) {
		int type=elm.readVarInt();
		Function<PacketBuffer, FHIcon> func=BufferIcon.get(type);
		if(func!=null)
			return func.apply(elm);
		return FHNopIcon.INSTANCE;
	}
	public static FHIcon nop() {
		return FHNopIcon.INSTANCE;
	}
	

}
