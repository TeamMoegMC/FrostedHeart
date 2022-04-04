package com.teammoeg.frostedheart.research.clues;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.network.FHClueProgressSyncPacket;
import com.teammoeg.frostedheart.network.PacketHandler;

import com.teammoeg.frostedheart.research.FHRegisteredItem;
import com.teammoeg.frostedheart.research.ResearchDataManager;
import com.teammoeg.frostedheart.research.TeamResearchData;
import com.teammoeg.frostedheart.util.Writeable;

import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.PacketDistributor;
/**
 * "Clue" for researches, contributes completion percentage for some researches.
 * */
public abstract class AbstractClue extends FHRegisteredItem implements Writeable{
	float contribution;//percentage, range (0,1]
	String ID;
	public float getResearchContribution() {
		return contribution;
	}

	public AbstractClue(String ID,float contribution) {
		this.contribution = contribution;
		this.ID = ID;
	}
	public void setCompleted(Team team,boolean trig) {
		ResearchDataManager.INSTANCE.getData(team.getId()).setClueTriggered(this,trig);
		this.sendProgressPacket(team);
	}
	@OnlyIn(Dist.CLIENT)
	public void setCompleted(boolean trig) {
		TeamResearchData.INSTANCE.setClueTriggered(this, trig);;
	}
	public boolean isCompleted(TeamResearchData data) {
		return data.isClueTriggered(this);
	}
	public boolean isCompleted(Team team) {
		return ResearchDataManager.INSTANCE.getData(team.getId()).isClueTriggered(this);
	}
	@OnlyIn(Dist.CLIENT)
	public boolean isCompleted() {
		return TeamResearchData.INSTANCE.isClueTriggered(this);
	}
	/**
	 * send progress packet to client
	 * should not called manually
	 * */
	public void sendProgressPacket(Team team) {
    	FHClueProgressSyncPacket packet=new FHClueProgressSyncPacket(team.getId(),this);
    	for(ServerPlayerEntity spe:team.getOnlineMembers())
    		PacketHandler.send(PacketDistributor.PLAYER.with(()->spe),packet);
    }

	public String getID() {
		return ID;
	}

	public IFormattableTextComponent getName() {
		return new TranslationTextComponent("clue."+FHMain.MODID+"."+ID+".name");
	}

	public IFormattableTextComponent getDescription() {
		return new TranslationTextComponent("clue."+FHMain.MODID+"."+ID+".desc");
	}

	public IFormattableTextComponent getHint() {
		return new TranslationTextComponent("clue."+FHMain.MODID+"."+ID+".hint");
	}
	@Override
	public String getLId() {
		return ID;
	}

	@Override
	public JsonObject serialize() {
		JsonObject jo=new JsonObject();
		jo.addProperty("percent",contribution);
		return jo;
	}

	@Override
	public void write(PacketBuffer buffer) {
		buffer.writeFloat(contribution);
	}
	
}
