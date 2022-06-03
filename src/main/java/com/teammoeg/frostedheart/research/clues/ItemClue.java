package com.teammoeg.frostedheart.research.clues;

import java.util.List;

import com.google.gson.JsonObject;

import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;

public class ItemClue extends ListenerClue {
	boolean consume;
	ItemStack stack;
	public ItemClue(String name, List<String> desc, float contribution) {
		super(name, desc, contribution);
	}

	public ItemClue(String name, float contribution) {
		super(name, contribution);
	}

	public ItemClue(JsonObject jo) {
		super(jo);
	}

	public ItemClue(PacketBuffer pb) {
		super(pb);
	}

	@Override
	public JsonObject serialize() {
		return super.serialize();
	}

	@Override
	public void write(PacketBuffer buffer) {
		super.write(buffer);
	}

	@Override
	public void initListener(Team t) {
	}

	@Override
	public void removeListener(Team t) {
	}

	@Override
	public String getType() {
		return "item";
	}

}
