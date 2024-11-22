/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.scenario.runner;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.FHCapabilities;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.base.team.FHTeamDataManager;
import com.teammoeg.frostedheart.content.scenario.parser.Scenario;
import com.teammoeg.frostedheart.content.scenario.runner.target.ExecuteStackElement;
import com.teammoeg.frostedheart.content.scenario.runner.target.ExecuteTarget;
import com.teammoeg.frostedheart.content.scenario.runner.target.ScenarioTarget;
import com.teammoeg.frostedheart.content.scenario.runner.target.TriggerTarget;
import com.teammoeg.frostedheart.util.TranslateUtils;
import com.teammoeg.frostedheart.util.io.CodecUtil;
import com.teammoeg.frostedheart.util.io.NBTSerializable;
import com.teammoeg.frostedheart.util.io.registry.IdRegistry;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraftforge.common.util.LazyOptional;

/**
 * ScenarioConductor
 * Main class for conducting and directing scenarios.
 * You shouldn't opearte this class from any other code except from scenario trigger and commands.
 * You should define triggers in script file and activate triggers to make it execute.
 * */
public class ScenarioConductor implements NBTSerializable{
    //Sence control
    private transient Act currentAct;
    public Map<ActNamespace,Act> acts=new HashMap<>();
    public IdRegistry<ActNamespace> actids=new IdRegistry<>();
    private transient boolean isActsEnabled;
    //private transient ActNamespace lastQuest;
    private ActScenarioContext context=new ActScenarioContext(this);
    private static ActNamespace lastCurrent;
	private static final ActNamespace init=new ActNamespace(null,null);
	
    public void copy() {}
    public void enableActs() {
    	if(!isActsEnabled) {
    		isActsEnabled=true;
       		if(lastCurrent!=null) {
       			currentAct=acts.get(lastCurrent);
    		}
    		acts.values().forEach(t->{
    			if(t.getStatus()==RunStatus.WAITTRIGGER) {
    				t.scene().setSlient(true);
    				//t.getScene().setSlient(true);
    				t.setStatus(RunStatus.RUNNING);
    				//if(t.name.equals(lastQuest))lastQuest=null;
    				
    				t.restoreLocation(getContext());
    				t.runCodeExecutionLoop(getContext());
    				t.scene().setSlient(false);
    			}else if(t.getStatus()==RunStatus.RUNNING) {
    				t.prepareForRun(context);
    			}
    		});
 
    	}
    }
    public void initContext(ServerPlayer player) {
    	context.setPlayer(player);
    }
    public ScenarioConductor() {
		super();
		currentAct=new Act(init);
		acts.put(init,currentAct);
		actids.register(init);
	}

    public void init(ServerPlayer player) {
    	initContext(player);
	}

    public ScenarioConductor(ServerPlayer player) {
		this();
		this.initContext(player);
	}
	
	public void notifyClientResponse(boolean isSkip,int status) {
		for(Act act:acts.values())
		act.notifyClientResponse(context, isSkip, status);
		
    }

	public void onLinkClicked(String link) {
		ExecuteTarget target=getCurrentAct().scene().getLinks().get(link);
		if(target!=null)
			getCurrentAct().jump(getContext(), target);
	}


	public void addTrigger(IScenarioTrigger trig,ScenarioTarget targ) {
		//if(getCurrentAct().name.isAct()) {
			getCurrentAct().addTrigger(trig,targ);
		//}else super.addTrigger(trig,targ);
	}
    public void run(String scenario) {
		if (scenario == null) {
			FHMain.LOGGER.error("[Scenario Conductor] Scenario to run is null");
		} else {
			FHMain.LOGGER.info("[Scenario Conductor] Running scenario "+scenario);
		}
		getContext().takeSnapshot();
		acts.get(init).jump(getContext(), scenario, null);
	}
	public void tick(ServerPlayer player) {
		init(player);
    	//detect triggers
		//System.out.println("start tick==============");
		
    	for(Act act:acts.values()) {
    		//System.out.println(act.name+": "+act);
    		act.tickTrigger(getContext());
    	}
    	//System.out.println("current act: "+getCurrentAct());
    	if(currentAct!=null) {
	    	currentAct.tickMain(context);
	    	if(currentAct.getStatus().shouldPause) {
	    		currentAct=null;
	    	}
    	}
    	if(currentAct==null)
	    	for(Act act:acts.values()) {
	    		
	    		act.runScheduled(context);
	    		if(!act.getStatus().shouldPause) {
	    			currentAct=act;
	    		}
	    	}
    	
    }


	public void pauseAct() {
		if(getCurrentAct().name.isAct()) {
			ActNamespace old=getCurrentAct().name;
			Act olddata=getCurrentAct();
			getContext().restoreSnapshot();//Protective against modify varibles without save
			if(!olddata.getStatus().shouldPause) {//Back to last savepoint unless it is waiting trigger
				olddata.restoreLocation(getContext());
			}else {
				olddata.setStatus(RunStatus.PAUSED);//pause current act
			}
			olddata.scene().clear(getContext(),RunStatus.STOPPED);
			
			
			acts.put(old, olddata);
			globalScope();
		}else {
			getContext().takeSnapshot();
		}	
	}
	public void continueAct(ActNamespace quest) {
		if(quest.equals(getCurrentAct().name))return;
		Act data=acts.get(quest);
		if(data!=null) {
			pauseAct();
			currentAct=data;
			data.prepareForRun(getContext());
			
			data.sendTitles(getContext(),true, true);
			if(data.getStatus().shouldRun) {
				data.scene().forcedClear(getContext(),RunStatus.RUNNING);
				data.run();
			}
		}
	}
	private void globalScope() {
		currentAct=(acts.get(init));
	}
	public void enterAct(ActNamespace quest) {
		if(quest.equals(getCurrentAct().name))return;
		Act old=getCurrentAct();
		Act data=acts.get(quest);
		if(data!=null) {
		}else {
			data=new Act(quest);
			acts.put(quest, data);
			actids.register(quest);
		}
		if(old.savedLocation!=null)
			data.savedLocation=new ParagraphData(old.savedLocation);
		else
			data.savedLocation=new ParagraphData(old.getScenario().name(),-1);
		currentAct=data;
		copyExecuteInfo(currentAct,old);
		old.stop();
		
	}
	public void copyExecuteInfo(Act later,Act old) {
		later.setExecutePos(old.getExecutePos());
		later.setScenario(old.getScenario());
		later.setStatus(old.getStatus());
		later.setCallStack(old.getCallStack());
	}
	public void queueAct(ActNamespace quest,String scene,String label) {
		Act data=getCurrentAct();
		if(!quest.equals(getCurrentAct().name)) {
			data=acts.get(quest);
			if(data==null){
				data=new Act(quest);
				acts.put(quest, data);
				actids.register(quest);
			}
		}
		if(scene==null)
			scene=getCurrentAct().getScenario().name();
		ExecuteTarget target;
		if(label!=null) {
			data.label=label;
			target=new ExecuteTarget(scene,label);
		}else {
			target=new ExecuteTarget(scene,null);
		}
		data.jump(getContext(), target);
		data.savedLocation=new ParagraphData(scene,-1);
	}

	public void endAct() {
		Act old=getCurrentAct();
		if(old.name.isAct()) {
			
			globalScope();
			copyExecuteInfo(currentAct,old);
			old.stop();
		}
	}


	public Act getCurrentAct() {
		return currentAct==null?acts.get(init):currentAct;
	}
    public static LazyOptional<ScenarioConductor> getCapability(@Nullable Player player) {
    	return FHCapabilities.SCENARIO.getCapability(player);
    }

	@Override
	public void save(CompoundTag data, boolean isPacket) {
    	data.put("vars", getContext().varData.save());
    	ListTag lacts=new ListTag();
    	for(Act v:acts.values()) {
    		if(v.name.isAct())
    			lacts.add(v.save());
    	}
    	
    	data.put("acts", lacts);
    	if(getCurrentAct()!=null&&getCurrentAct().name.isAct()) {
    		CodecUtil.encodeNBT(ActNamespace.CODEC, data, "current",getCurrentAct().name);
    	}
	}
	@Override
	public void load(CompoundTag data, boolean isPacket) {
		getContext().varData.load(data.getCompound("vars"));
    	getContext().takeSnapshot();
    	ListTag lacts=data.getList("acts", Tag.TAG_COMPOUND);
    	//Act initact=acts.get(init);
    	for(Tag v:lacts) {
    		CompoundTag t=(CompoundTag) v;
    		ActNamespace ns=new ActNamespace(t.getString("chapter"),t.getString("act"));
    		if(acts.containsKey(ns)) {
    			acts.get(ns).load(t);
    		}else {
    			Act i=new Act(t);
        		acts.put(i.name, i);
        		actids.register(i.name);
    		}
    		
    	}
    	//acts.put(init, initact);
    	if(data.contains("current")) {
    		lastCurrent=CodecUtil.decodeNBT(ActNamespace.CODEC, data, "current");
    	}
    	//lastQuest=

    	//currentAct=acts.get();
    	//if(currentAct==null)
    	//currentAct=acts.get(empty);
	}
	public ScenarioContext getContext() {
		return context;
	}

	public void jump(ExecuteTarget executeTarget) {
		getCurrentAct().jump(context,executeTarget);
	}
	public void queue(ExecuteTarget executeTarget) {
		getCurrentAct().queue(executeTarget);
	}

}
