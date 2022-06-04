package com.teammoeg.frostedheart.research.clues;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.network.FHClueProgressSyncPacket;
import com.teammoeg.frostedheart.network.PacketHandler;
import com.teammoeg.frostedheart.research.AutoIDItem;
import com.teammoeg.frostedheart.research.ResearchDataManager;
import com.teammoeg.frostedheart.research.TeamResearchData;
import com.teammoeg.frostedheart.research.gui.FHTextUtil;
import com.teammoeg.frostedheart.util.Writeable;

import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.PacketDistributor;

/**
 * "Clue" for researches, contributes completion percentage for some
 * researches.6
 * Clue can be trigger if any research is researchable(finished item commit)
 */
public abstract class Clue extends AutoIDItem implements Writeable {
	float contribution;// percentage, range (0,1]
	String name = "";
	String desc = "";
	String hint = "";

	public float getResearchContribution() {
		return contribution;
	}

	public Clue(String name, String desc, String hint, float contribution) {
		super();
		this.contribution = contribution;
		this.name = name;
		this.desc = desc;
		this.hint = hint;
	}

	public Clue(String name, float contribution) {
		this(name, "", "", contribution);
	}

	public Clue(JsonObject jo) {
		super();
		if (jo.has("name"))
			this.name = jo.get("name").getAsString();
		if (jo.has("desc"))
			this.desc = jo.get("desc").getAsString();
		if (jo.has("hint"))
			this.hint = jo.get("hint").getAsString();
		this.contribution = jo.get("value").getAsFloat();

	}

	public Clue(PacketBuffer pb) {
		super();
		this.name = pb.readString();
		this.desc = pb.readString();
		this.hint = pb.readString();
		this.contribution = pb.readFloat();

	}

	public void setCompleted(Team team, boolean trig) {
		ResearchDataManager.INSTANCE.getData(team.getId()).setClueTriggered(this, trig);
		if (trig)
			end(team);
		else
			start(team);
		this.sendProgressPacket(team);
	}

	@OnlyIn(Dist.CLIENT)
	public void setCompleted(boolean trig) {
		TeamResearchData.getClientInstance().setClueTriggered(this, trig);
		;
	}

	public boolean isCompleted(TeamResearchData data) {
		return data.isClueTriggered(this);
	}

	public boolean isCompleted(Team team) {
		return ResearchDataManager.INSTANCE.getData(team.getId()).isClueTriggered(this);
	}

	@OnlyIn(Dist.CLIENT)
	public boolean isCompleted() {
		return TeamResearchData.getClientInstance().isClueTriggered(this);
	}

	/**
	 * send progress packet to client
	 * should not called manually
	 */
	public void sendProgressPacket(Team team) {
		FHClueProgressSyncPacket packet = new FHClueProgressSyncPacket(team.getId(), this);
		for (ServerPlayerEntity spe : team.getOnlineMembers())
			PacketHandler.send(PacketDistributor.PLAYER.with(() -> spe), packet);
	}

	/**
	 * called when researches load finish
	 */
	public abstract void init();

	/**
	 * called when this clue's research has started
	 */
	public abstract void start(Team team);

	/**
	 * Stop detection when clue is completed
	 */
	public abstract void end(Team team);

	public ITextComponent getName() {
		return FHTextUtil.get(name, "clue", () -> this.getLId() + ".name");
	}

	public ITextComponent getDescription() {
		return FHTextUtil.getOptional(desc, "clue", () -> this.getLId() + ".desc");
	}

	public ITextComponent getHint() {
		return FHTextUtil.getOptional(hint, "clue", () -> this.getLId() + ".hint");
	}

	@Override
	public JsonObject serialize() {
		JsonObject jo = new JsonObject();
		if (!name.isEmpty())
			jo.addProperty("name", name);
		if (!desc.isEmpty())
			jo.addProperty("desc", desc);
		if (!hint.isEmpty())
			jo.addProperty("hint", hint);
		jo.addProperty("value", contribution);
		return jo;
	}

	@Override
	public void write(PacketBuffer buffer) {
		buffer.writeString(name);
		buffer.writeString(desc);
		buffer.writeString(hint);
		buffer.writeFloat(contribution);
	}

}
