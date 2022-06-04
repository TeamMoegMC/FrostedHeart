package com.teammoeg.frostedheart.research.clues;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.research.ResearchListeners;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;

public class ItemClue extends ListenerClue {
	boolean consume;
	IngredientWithSize stack;

	public ItemClue(String name, String desc, String hint, float contribution, IngredientWithSize stack) {
		super(name, desc, hint, contribution);
		this.stack = stack;
	}

	public ItemClue(JsonObject jo) {
		super(jo);
		stack = IngredientWithSize.deserialize(jo.get("item"));
		if (jo.has("consume"))
			consume = jo.get("consume").getAsBoolean();
	}

	public ItemClue(PacketBuffer pb) {
		super(pb);
		stack = IngredientWithSize.read(pb);
		consume = pb.readBoolean();
	}

	@Override
	public JsonObject serialize() {
		JsonObject jo = super.serialize();
		jo.add("item", stack.serialize());
		if (consume)
			jo.addProperty("consume", consume);
		return jo;
	}

	@Override
	public void write(PacketBuffer buffer) {
		super.write(buffer);
		stack.write(buffer);
		buffer.writeBoolean(consume);
	}

	@Override
	public void initListener(Team t) {
		ResearchListeners.itemClues.add(this, t);
	}

	@Override
	public void removeListener(Team t) {
		ResearchListeners.itemClues.remove(this, t);
	}

	public int test(Team t, ItemStack stack) {
		if (this.stack.test(stack)) {
			this.setCompleted(t, true);
			if (consume)
				return this.stack.getCount();
		}
		return 0;
	}

	@Override
	public String getType() {
		return "item";
	}

}
