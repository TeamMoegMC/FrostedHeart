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

package com.teammoeg.frostedheart.scenario.runner;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHCapabilities;
import com.teammoeg.frostedheart.scenario.parser.Scenario;
import com.teammoeg.frostedheart.scenario.runner.target.ActTarget;
import com.teammoeg.frostedheart.scenario.runner.target.ExecuteStackElement;
import com.teammoeg.frostedheart.scenario.runner.target.ExecuteTarget;
import com.teammoeg.frostedheart.scenario.runner.target.IScenarioTarget;
import com.teammoeg.frostedheart.scenario.runner.target.TriggerTarget;
import com.teammoeg.frostedheart.team.SpecialDataManager;
import com.teammoeg.frostedheart.util.client.GuiUtils;
import com.teammoeg.frostedheart.util.io.NBTSerializable;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.LazyOptional;

/**
 * ScenarioConductor
 * Main class for conducting and directing scenarios.
 * You shouldn't opearte this class from any other code except from scenario trigger and commands.
 * You should define triggers in script file and activate triggers to make it execute.
 * */
public class ScenarioConductor extends ScenarioVM implements NBTSerializable{
    //Sence control
    private transient Act currentAct;
    public Map<ActNamespace,Act> acts=new HashMap<>();
    private transient boolean isActsEnabled;
    private transient ActNamespace lastQuest;
    private static final ActNamespace global=new ActNamespace();

	private static final ActNamespace init=new ActNamespace(null,null);
    boolean inited=false;


    public void copy() {}
    public void enableActs() {
    	if(!isActsEnabled) {
    		isActsEnabled=true;
       		if(lastQuest!=null) {
    			acts.get(lastQuest).queue(acts.get(lastQuest).paragraph);
    		}
    		acts.values().forEach(t->{
    			if(t.getStatus()==RunStatus.WAITTRIGGER) {
    				//t.getScene().setSlient(true);
    				t.setStatus(RunStatus.RUNNING);
    				if(t.name.equals(lastQuest))lastQuest=null;
    				t.queue(t.paragraph);
    				//t.getScene().setSlient(false);
    			}
    		});
 
    	}
    }
    public ScenarioConductor() {
		super();
		setCurrentAct(new Act(this,init));
		acts.put(init, getCurrentAct());
		acts.put(global, new Act(this,global));
	}

    public void init(ServerPlayerEntity player) {
    	if(!isInited())inited=true;
		this.player = player.getUniqueID();
	}

    public ScenarioConductor(ServerPlayerEntity player) {
		super();
		this.player = player.getUniqueID();
		setCurrentAct(new Act(this,init));
		acts.put(init, getCurrentAct());
		acts.put(global, new Act(this,global));

		
	}



	public Scene getScene() {
    	return getCurrentAct().getScene();
    }



    public void paragraph(int pn) {
		varData.takeSnapshot();
    	getCurrentAct().newParagraph(getScenario(), pn);
    	super.paragraph(pn);
	}

	public void notifyClientResponse(boolean isSkip,int status) {
		this.isSkip=isSkip;
		this.clientStatus=status;
		if(this.getStatus()==RunStatus.WAITCLIENT) {
			if(clearAfterClick)
				doParagraph();
			run();
		}else if(this.getStatus()==RunStatus.WAITTIMER&&isSkip) {
			this.stopWait();
			run();
		}
		
    }

	public void onLinkClicked(String link) {
		ExecuteTarget jt=getScene().getLinks().get(link);
		if(jt!=null) {
			jump(jt);
		}
	}


	public void addTrigger(IScenarioTrigger trig,IScenarioTarget targ) {
		//if(getCurrentAct().name.isAct()) {
			getCurrentAct().addTrigger(trig,targ);
		//}else super.addTrigger(trig,targ);
	}
    public void run(Scenario sp) {
		if (sp == null) {
			FHMain.LOGGER.error("[Scenario Conductor] Scenario to run is null");
		} else {
			FHMain.LOGGER.info("[Scenario Conductor] Running scenario "+sp.name);
		}

		this.setScenario(sp);
		nodeNum=0;
		varData.takeSnapshot();
		getCurrentAct().newParagraph(sp, 0);

		run();
	}

	@Override
	public LinkedList<ExecuteStackElement> getCallStack() {
		return getCurrentAct().getCallStack();
	}
	@Override
	public void jump(IScenarioTarget nxt) {
		getCurrentAct().setActState();
		super.jump(nxt);
	}
	public void tick() {
		if(!isInited())return;
    	//detect triggers
		if(getStatus()==RunStatus.RUNNING) {
			run();
		}
    	for(TriggerTarget t:triggers) {
    		if(t.test(this)) {
    			if(t.use()) {
    				if(t.isAsync())
						addToQueue(t);
					else
						jump(t);
    			}
    		}
    	}
    	triggers.removeIf(t->!t.canUse());
    	for(Act a:acts.values())
	    	a.getScene().tickTriggers(this, getCurrentAct()==a);
    	
    	if(getStatus()==RunStatus.WAITTIMER) {
    		if(getScene().tickWait()) {
    			
    			run();
    			return;
    		}
    	}
    	//Execute Queued actions
    	runScheduled();
    }


	public void pauseAct() {
		if(getCurrentAct().name.isAct()) {
			ActNamespace old=getCurrentAct().name;
			Act olddata=getCurrentAct();
			varData.restoreSnapshot();//Protective against modify varibles without save
			if(!status.shouldPause) {//Back to last savepoint unless it is waiting trigger
				olddata.paragraph.apply(olddata);
				olddata.setStatus(RunStatus.RUNNING);
			}else {//Save current state if stopped or waiting trigger.
				olddata.setActState();
			}
			olddata.getScene().clear(this);
			acts.put(old, olddata);
			globalScope();
		}else {
			varData.takeSnapshot();
		}	
	}
	public void continueAct(ActNamespace quest) {
		if(quest.equals(getCurrentAct().name))return;
		Act data=acts.get(quest);
		if(data!=null) {
			pauseAct();
			this.setCurrentAct(data);
			data.prepareForRun();
			if(data.getScenario()!=null) {
				this.sp=data.getScenario();
				this.nodeNum=data.getNodeNum();
				this.status=data.getStatus();
			}
			data.sendTitles(true, true);
			if(getStatus().shouldRun) {
				getScene().forcedClear(this);
				run();
			}
		}
	}
	private void globalScope() {
		setCurrentAct(acts.get(global));
	}
	public void enterAct(ActNamespace quest) {
		if(quest.equals(getCurrentAct().name))return;
		Act old=getCurrentAct();
		Act data=acts.get(quest);
		pauseAct();
		if(data!=null) {
			this.setCurrentAct(data);
			data.paragraph.setScenario(this.getScenario());
			data.paragraph.setParagraphNum(old.paragraph.getParagraphNum());
		}else {
			data=new Act(this,quest);
			acts.put(quest, data);
			data.paragraph.setScenario(this.getScenario());
			data.paragraph.setParagraphNum(old.paragraph.getParagraphNum());
			this.setCurrentAct(data);
		}
	}
	public void queue(IScenarioTarget questExecuteTarget) {
		getCurrentAct().queue(questExecuteTarget);
		//toExecute.add(questExecuteTarget);
	}
	public void queueAct(ActNamespace quest,String scene,String label) {
		Act data=getCurrentAct();
		if(!quest.equals(getCurrentAct().name)) {
			data=acts.get(quest);
			if(data==null){
				data=new Act(this,quest);
				acts.put(quest, data);
			}
		}
		if(scene==null)
			scene=getScenario().name;
		ExecuteTarget target;
		if(label!=null) {
			data.label=label;
			target=new ExecuteTarget(this,scene,label);
		}else {
			target=new ExecuteTarget(this,scene,null);
		}
		target.apply(data);
		data.paragraph.setScenario(target.getScenario());
		data.paragraph.setParagraphNum(0);
		addToQueue(new ActTarget(quest,target));
	}

	public void endAct() {
		if(getCurrentAct().name.isAct()) {
			acts.remove(getCurrentAct().name);
		}
		globalScope();
	}


	public Act getCurrentAct() {
		return currentAct;
	}
	private void setCurrentAct(Act currentAct) {
		this.currentAct = currentAct;
	}
    public static LazyOptional<ScenarioConductor> getCapability(@Nullable PlayerEntity player) {
    	return FHCapabilities.SCENARIO.getCapability(player);
    }

	public boolean isInited() {
		return inited;
	}
	@Override
	public void save(CompoundNBT data, boolean isPacket) {
    	if(varData.snapshot==null)
    		data.put("vars", varData.extraData);
    	else
    		data.put("vars", varData.snapshot);
    	ListNBT lacts=new ListNBT();
    	for(Act v:acts.values()) {
    		if(v.name.isAct())
    			lacts.add(v.save());
    	}
    	data.put("acts", lacts);
    	if(getCurrentAct().name.isAct()) {
    		data.putString("chapter", getCurrentAct().name.chapter);
    		data.putString("act", getCurrentAct().name.act);
    	}
	}
	@Override
	public void load(CompoundNBT data, boolean isPacket) {
		// TODO Auto-generated method stub
		varData.extraData=data.getCompound("vars");
    	varData.snapshot=varData.extraData;
    	ListNBT lacts=data.getList("acts", Constants.NBT.TAG_COMPOUND);
    	for(INBT v:lacts) {
    		Act i=new Act(this,(CompoundNBT) v);
    		acts.put(i.name, i);
    	}
    	lastQuest=new ActNamespace(data.getString("chapter"),data.getString("act"));
    	//currentAct=acts.get();
    	//if(currentAct==null)
    	//currentAct=acts.get(empty);
	}
	public ServerPlayerEntity getPlayer() {
        return SpecialDataManager.getServer().getPlayerList().getPlayerByUUID(player);
    }
    public String getLang() {
    	return getPlayer().getLanguage();
    }
	public void sendMessage(String s) {
		getPlayer().sendStatusMessage(GuiUtils.str(s), false);
	}
}
