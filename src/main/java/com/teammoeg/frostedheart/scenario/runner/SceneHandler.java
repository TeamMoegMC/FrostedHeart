package com.teammoeg.frostedheart.scenario.runner;

import java.util.HashMap;
import java.util.Map;

import com.teammoeg.frostedheart.FHPacketHandler;
import com.teammoeg.frostedheart.scenario.network.ServerSenarioTextPacket;
import com.teammoeg.frostedheart.scenario.runner.ScenarioConductor.ExecuteTarget;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.fml.network.PacketDistributor;

/**
 * A paragraph/scene is, a kinda block of code.
 * That means, forces to invalidate all allocated resource created during paragraph, such as text, links etc.
 * 
 * */
public class SceneHandler {
    final Map<String,ExecuteTarget> links=new HashMap<>();
    int paragraphNum;
    boolean lastIsReline=true;
    StringBuilder currentLiteral;
    CompoundNBT executionData=new CompoundNBT();
    final ScenarioConductor parent;
    public SceneHandler(ScenarioConductor parent) {
		super();
		this.parent = parent;
	}
	public void clear(int paragraphNum) {
		sendNormal();
		parent.waitClient();
    	lastIsReline=true;
    	this.paragraphNum=paragraphNum;
    	if(!executionData.isEmpty())
    		executionData=new CompoundNBT();
    	links.clear();
    	currentLiteral=null;
    }
    public boolean shouldWaitClient() {
    	return currentLiteral!=null&&currentLiteral.length()!=0;
    }
    public void appendLiteral(String text) {
    	if(!text.isEmpty()) {
    		if(currentLiteral==null)
        		currentLiteral=new StringBuilder();
    		currentLiteral.append(text);
    	}
    }
    
    public void sendNormal() {
    	if(currentLiteral!=null) {
    		//System.out.println("Reline "+currentLiteral.toString());
    		FHPacketHandler.send(PacketDistributor.PLAYER.with(()->(ServerPlayerEntity)parent.player), new ServerSenarioTextPacket(currentLiteral.toString(),true,parent.isNowait));
    	}else if(lastIsReline) {
    		//System.out.println("Reline");
    		FHPacketHandler.send(PacketDistributor.PLAYER.with(()->(ServerPlayerEntity)parent.player), new ServerSenarioTextPacket("",true,parent.isNowait));
    		lastIsReline=false;
    	}
    	currentLiteral=null;
    }
    public void sendNewline() {
    	if(currentLiteral!=null) {
    		//System.out.println("Reline "+currentLiteral.toString());
    		FHPacketHandler.send(PacketDistributor.PLAYER.with(()->(ServerPlayerEntity)parent.player), new ServerSenarioTextPacket(currentLiteral.toString(),true,parent.isNowait));
    	}else if(lastIsReline) {
    		//System.out.println("Reline");
    		FHPacketHandler.send(PacketDistributor.PLAYER.with(()->(ServerPlayerEntity)parent.player), new ServerSenarioTextPacket("",true,parent.isNowait));
    		lastIsReline=false;
    	}else {
    		FHPacketHandler.send(PacketDistributor.PLAYER.with(()->(ServerPlayerEntity)parent.player), new ServerSenarioTextPacket(" ",true,parent.isNowait));
    	}
    	currentLiteral=null;
    }
    public void sendNoreline() {
    	if(currentLiteral!=null) {
    		//System.out.println("NoReline "+currentLiteral.toString());
    		lastIsReline=true;
    		FHPacketHandler.send(PacketDistributor.PLAYER.with(()->(ServerPlayerEntity)parent.player), new ServerSenarioTextPacket(currentLiteral.toString(),false,parent.isNowait));
    	}
    	currentLiteral=null;
    }
}
