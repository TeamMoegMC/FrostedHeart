package com.teammoeg.frostedheart.research.clues;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.research.TeamResearchData;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

public class AdvancementClue extends TickListenerClue {
	ResourceLocation advancement = new ResourceLocation("minecraft:story/root");
	String criterion = "";

	public AdvancementClue(String name, String desc, String hint, float contribution) {
		super(name, desc, hint, contribution);
	}

	public AdvancementClue(String name, float contribution) {
		super(name, contribution);
	}

	public AdvancementClue(JsonObject jo) {
		super(jo);
		advancement = new ResourceLocation(jo.get("advancement").getAsString());
		if (jo.has("criterion"))
			criterion = jo.get("criterion").getAsString();
	}

	public AdvancementClue(PacketBuffer pb) {
		super(pb);
		advancement = pb.readResourceLocation();
		criterion = pb.readString();
	}

	public AdvancementClue() {
		super();
	}

	@Override
	public boolean isCompleted(TeamResearchData t, ServerPlayerEntity player) {
		Advancement a = player.server.getAdvancementManager().getAdvancement(advancement);
		if (a == null) {
			return false;
		}

		AdvancementProgress progress = player.getAdvancements().getProgress(a);

		if (criterion.isEmpty()) {
			return progress.isDone();
		}
		CriterionProgress criterionProgress = progress.getCriterionProgress(criterion);
		return criterionProgress != null && criterionProgress.isObtained();
	}

	@Override
	public String getId() {
		return "advancement";
	}

	@Override
	public JsonObject serialize() {
		JsonObject jo = super.serialize();
		jo.addProperty("advancement", advancement.toString());
		if (!criterion.isEmpty())
			jo.addProperty("criterion", criterion);
		return jo;
	}

	@Override
	public void write(PacketBuffer buffer) {
		super.write(buffer);
		buffer.writeResourceLocation(advancement);
		buffer.writeString(criterion);
	}

	@Override
	public int getIntType() {
		return 1;
	}

}
