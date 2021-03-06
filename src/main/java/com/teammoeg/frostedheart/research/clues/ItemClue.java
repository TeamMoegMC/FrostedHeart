package com.teammoeg.frostedheart.research.clues;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.research.TeamResearchData;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public class ItemClue extends Clue {
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

	ItemClue() {
		super();
	}

	@Override
	public ITextComponent getName() {
		if (name != null && !name.isEmpty())
			return super.getName();
		return GuiUtils.translate("clue." + FHMain.MODID + ".item");
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

	public int test(TeamResearchData t, ItemStack stack) {
		if (this.stack.test(stack)) {
			this.setCompleted(t, true);
			if (consume)
				return this.stack.getCount();
		}
		return 0;
	}

	@Override
	public String getId() {
		return "item";
	}

	@Override
	public void init() {
	}

	@Override
	public void start(Team team) {
	}

	@Override
	public void end(Team team) {
	}

	@Override
	public int getIntType() {
		return 2;

	}

	@Override
	public ITextComponent getDescription() {
		ITextComponent itc = super.getDescription();
		if (itc != null || stack == null)
			return itc;
		if (stack.hasNoMatchingItems())
			return null;
		return stack.getMatchingStacks()[0].getDisplayName().copyRaw()
				.appendSibling(new StringTextComponent(" x" + stack.getCount()));
	}

}
