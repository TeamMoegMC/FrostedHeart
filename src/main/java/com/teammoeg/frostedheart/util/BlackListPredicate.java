package com.teammoeg.frostedheart.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.item.ItemStack;

public class BlackListPredicate extends ItemPredicate {
	ItemPredicate white;
	public BlackListPredicate(JsonObject jo) {
		JsonElement intern=new JsonParser().parse(jo.toString());
		intern.getAsJsonObject().remove("type");
		white=ItemPredicate.deserialize(intern);
	}
	@Override
	public boolean test(ItemStack item) {
		boolean rs=!white.test(item);
		System.out.println(rs);
		return rs;
	}
	public JsonElement serialize() {
		new Exception().printStackTrace();
		System.out.println("serl");
		JsonElement je=white.serialize();
		je.getAsJsonObject().addProperty("type",FHMain.MODID+":blacklist");
		return je;
	}
}
