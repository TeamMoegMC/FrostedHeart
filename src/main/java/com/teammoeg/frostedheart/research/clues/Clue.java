package com.teammoeg.frostedheart.research.clues;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.network.FHClueProgressSyncPacket;
import com.teammoeg.frostedheart.network.PacketHandler;
import com.teammoeg.frostedheart.research.AutoIDItem;
import com.teammoeg.frostedheart.research.ResearchDataManager;
import com.teammoeg.frostedheart.research.TeamResearchData;
import com.teammoeg.frostedheart.research.gui.FHTextUtil;
import com.teammoeg.frostedheart.util.SerializeUtil;
import com.teammoeg.frostedheart.util.Writeable;

import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.PacketDistributor;
/**
 * "Clue" for researches, contributes completion percentage for some researches.6
 * Clue can be trigger if any research is researchable(finished item commit)
 * */
public abstract class Clue extends AutoIDItem implements Writeable{
	float contribution;//percentage, range (0,1]
	String name="";
	List<String> desc;
	public float getResearchContribution() {
		return contribution;
	}

	public Clue(String name, List<String> desc, float contribution) {
		super();
		this.name = name;
		this.desc = desc;
		this.contribution = contribution;
	}
	public Clue(String name, float contribution) {
		super();
		this.name = name;
		this.desc =new ArrayList<>();
		this.contribution = contribution;
	}
	public Clue(JsonObject jo) {
		super();
		this.name = jo.get("name").getAsString();
		this.desc = SerializeUtil.parseJsonElmList(jo.get("desc"),JsonElement::getAsString);
		this.contribution = jo.get("value").getAsFloat();
		
	}
	public Clue(PacketBuffer pb) {
		super();
		this.name = pb.readString();
		this.desc = SerializeUtil.readList(pb,PacketBuffer::readString);
		this.contribution = pb.readFloat();
		
	}
	public void setCompleted(Team team,boolean trig) {
		ResearchDataManager.INSTANCE.getData(team.getId()).setClueTriggered(this,trig);
		if(trig)
			end(team);
		else
			start(team);
		this.sendProgressPacket(team);
	}
	@OnlyIn(Dist.CLIENT)
	public void setCompleted(boolean trig) {
		TeamResearchData.getClientInstance().setClueTriggered(this, trig);;
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
	 * */
	public void sendProgressPacket(Team team) {
    	FHClueProgressSyncPacket packet=new FHClueProgressSyncPacket(team.getId(),this);
    	for(ServerPlayerEntity spe:team.getOnlineMembers())
    		PacketHandler.send(PacketDistributor.PLAYER.with(()->spe),packet);
    }
	/**
	 * called when researches load finish
	 * */
	public abstract void init();
	/**
	 * called when this clue's research has started
	 * */
	public abstract void start(Team team);
	/**
	 * Stop detection when clue is completed
	 * */
	public abstract void end(Team team);
	public IFormattableTextComponent getName() {
		return (IFormattableTextComponent) FHTextUtil.get(name,"clue",()->this.getLId()+".name");
	}

	public List<ITextComponent> getDescription() {
		return FHTextUtil.get(desc,"clue",()->this.getLId()+".desc");
	}
	@Override
	public JsonObject serialize() {
		JsonObject jo=new JsonObject();
		jo.addProperty("name", name);
		jo.add("desc", SerializeUtil.toJsonStringList(desc,e->e));
		jo.addProperty("valeu",contribution);
		return jo;
	}

	@Override
	public void write(PacketBuffer buffer) {
		buffer.writeString(name);
		SerializeUtil.writeList2(buffer,desc,PacketBuffer::writeString);
		buffer.writeFloat(contribution);
	}
	
}
