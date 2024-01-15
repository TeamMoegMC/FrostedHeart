package com.teammoeg.frostedheart.scenario.runner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.teammoeg.frostedheart.FHPacketHandler;
import com.teammoeg.frostedheart.scenario.network.ServerSenarioTextPacket;
import com.teammoeg.frostedheart.scenario.runner.target.ExecuteTarget;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.network.PacketDistributor;

/**
 * A scene is a place to present content to client
 *
 * */
public class Scene {
	transient final Map<String,ExecuteTarget> links=new HashMap<>();
    transient boolean lastNowrap=false;
    transient StringBuilder currentLiteral;
    CompoundNBT executionData=new CompoundNBT();
    transient final Act parent;
    public transient boolean isNowait;
    private boolean isSaveNowait;
    private transient boolean isSlient;
    transient int waiting;
    LinkedList<StringBuilder> log=new LinkedList<>();
    private transient List<IScenarioTrigger> triggers=new ArrayList<>();
    List<String> savedLog=new ArrayList<>();
    private transient boolean doResetScene;
    public CompoundNBT save() {
    	CompoundNBT nbt=new CompoundNBT();
    	nbt.putBoolean("nowait", isSaveNowait);
    	ListNBT logs=new ListNBT();
    	for(String s:savedLog) {
    		logs.add(StringNBT.valueOf(s));
    	}
    	nbt.put("logs", logs);
    	return nbt;
    }
    public void load(CompoundNBT nbt) {
    	isSaveNowait=nbt.getBoolean("nowait");
    	ListNBT logs=nbt.getList("logs", Constants.NBT.TAG_STRING);
    	savedLog.clear();
    	log.clear();
    	for(INBT s:logs) {
    		savedLog.add(s.getString());
    		log.add(new StringBuilder(s.getString()));
    	}
    }
    public void addLog(String text) {
    	if(lastNowrap) {
    		log.peekLast().append(text);
    	}else {
    		log.add(new StringBuilder(text));
    	}
    }
    public Scene(Act parent) {
		super();
		this.parent = parent;
	}
	public void clear() {
		doResetScene=true;
		sendNormal();
		//parent.waitClient();
    	lastNowrap=true;
    	isSaveNowait=isNowait;
    	savedLog.clear();
    	triggers.clear();
    	for(StringBuilder sb:log)
    		savedLog.add(sb.toString());
    	log.clear();
    	if(!executionData.isEmpty())
    		executionData=new CompoundNBT();
    	links.clear();
    	currentLiteral=null;
    }
    public boolean shouldWaitClient() {
    	return (currentLiteral!=null&&currentLiteral.length()!=0)&&!isNowait&&!isSlient;
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
    		addLog(currentLiteral.toString());
    		//System.out.println("Reline "+currentLiteral.toString());
    		if(!isSlient())
    			FHPacketHandler.send(PacketDistributor.PLAYER.with(()->parent.getPlayer()), new ServerSenarioTextPacket(currentLiteral.toString(),true,isNowait,doResetScene));
    		doResetScene=false;
    	}else if(lastNowrap) {
    		//System.out.println("Reline");
    		if(!isSlient())
    			FHPacketHandler.send(PacketDistributor.PLAYER.with(()->parent.getPlayer()), new ServerSenarioTextPacket("",true,isNowait,doResetScene));
    		lastNowrap=false;
    		doResetScene=false;
    	}else if(doResetScene) {
    		FHPacketHandler.send(PacketDistributor.PLAYER.with(()->parent.getPlayer()), new ServerSenarioTextPacket("",false,isNowait,doResetScene));
    		doResetScene=false;
    	}
    	currentLiteral=null;
    }
    public void sendNewline() {
    	if(currentLiteral!=null) {
    		//System.out.println("Reline "+currentLiteral.toString());
    		addLog(currentLiteral.toString());
    		if(!isSlient())
    			FHPacketHandler.send(PacketDistributor.PLAYER.with(()->parent.getPlayer()), new ServerSenarioTextPacket(currentLiteral.toString(),true,isNowait,doResetScene));
    	}else if(lastNowrap) {
    		//System.out.println("Reline");
    		if(!isSlient())
    			FHPacketHandler.send(PacketDistributor.PLAYER.with(()->parent.getPlayer()), new ServerSenarioTextPacket("",true,isNowait,doResetScene));
    		lastNowrap=false;
    	}else {
    		if(!isSlient())
    			FHPacketHandler.send(PacketDistributor.PLAYER.with(()->parent.getPlayer()), new ServerSenarioTextPacket(" ",true,isNowait,doResetScene));
    	}
    	currentLiteral=null;
    }
    public void sendNoreline() {
    	if(currentLiteral!=null) {
    		//System.out.println("NoReline "+currentLiteral.toString());
    		lastNowrap=true;
    		addLog(currentLiteral.toString());
    		if(!isSlient())
    			FHPacketHandler.send(PacketDistributor.PLAYER.with(()->parent.getPlayer()), new ServerSenarioTextPacket(currentLiteral.toString(),false,isNowait,doResetScene));
    	}
    	currentLiteral=null;
    }
	public boolean isSlient() {
		return isSlient;
	}
	public void setSlient(boolean isSlient) {
		this.isSlient = isSlient;
	}
	public void waitClient() {
    	if(shouldWaitClient()&&!isSlient)
    		parent.setStatus(RunStatus.WAITCLIENT);
    }
	public void addWait(int time) {
		waiting+=time;
		parent.setStatus(RunStatus.WAITTIMER);
    }
	public boolean tickWait() {
    	if(waiting>0) {
    		waiting--;
    		if(waiting<=0)
    			return true;
    	}
    	return false;
	}
	public void tickTriggers(ScenarioConductor runner,boolean isCurrentAct) {
    	for(IScenarioTrigger t:triggers) {
    		if(t.test(runner)) {
    			if(t.use()) {
	    			if(isCurrentAct) {
	    				parent.jump(t);
	    			}else {
	    				parent.queue(t);
	    			}
    			}
    		}
    	}
    	triggers.removeIf(t->!t.canUse());
	}
    public void clearLink() {
    	links.clear();
	}
	public void addTrigger(IScenarioTrigger trig) {
		triggers.add(trig);
	}
}
