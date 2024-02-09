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
import java.util.Map;

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.scenario.ScenarioExecutionException;
import com.teammoeg.frostedheart.scenario.parser.Node;
import com.teammoeg.frostedheart.scenario.parser.Scenario;
import com.teammoeg.frostedheart.scenario.runner.target.ActTarget;
import com.teammoeg.frostedheart.scenario.runner.target.ExecuteTarget;
import com.teammoeg.frostedheart.scenario.runner.target.IScenarioTarget;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.LazyOptional;

/**
 * ScenarioConductor
 * Main class for conducting and directing scenarios.
 * You shouldn't opearte this class from any other code except from scenario trigger and commands.
 * You should define triggers in script file and activate triggers to make it execute.
 * */
public class ScenarioConductor extends ScenarioVM implements ICapabilitySerializable<CompoundNBT>{
    //Sence control
    private transient Act currentAct;
    public Map<ActNamespace,Act> acts=new HashMap<>();
    private transient boolean isActsEnabled;
    private transient ActNamespace lastQuest;
    private static final ActNamespace global=new ActNamespace();
    private static final ActNamespace init=new ActNamespace(null,null);
    @CapabilityInject(ScenarioConductor.class)
    public static Capability<ScenarioConductor> CAPABILITY;
    public static final ResourceLocation ID = new ResourceLocation(FHMain.MODID, "scenario");
    private final LazyOptional<ScenarioConductor> capability=LazyOptional.of(()->this);
    public CompoundNBT save() {
    	CompoundNBT data=new CompoundNBT();
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
    	return data;
    }
    public void load(CompoundNBT data) {
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
    boolean inited=false;
    public void init(ServerPlayerEntity player) {
    	if(!inited)inited=true;
		this.player = player.getUniqueID();
	}
    public ScenarioConductor(ServerPlayerEntity player) {
		super();
		this.player = player.getUniqueID();
		setCurrentAct(new Act(this,init));
		acts.put(init, getCurrentAct());
		acts.put(global, new Act(this,global));

		
	}
    public ScenarioConductor(ServerPlayerEntity player,CompoundNBT data) {
		super();
		this.player = player.getUniqueID();
		load(data);
		setCurrentAct(new Act(this,init));
		acts.put(init, getCurrentAct());
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


	public void addTrigger(IScenarioTrigger trig) {
		if(getCurrentAct().name.isAct()) {
			getCurrentAct().addTrigger(trig);
		}else super.addTrigger(trig);
	}

    /*public void restoreParagraph(ParagraphData paragraph) {
		Scenario sp=paragraph.getScenario();
		if(paragraph.getParagraphNum()==0)
			currentQuestData.nodeNum=0;
		else
			currentQuestData.nodeNum=sp.paragraphs[paragraph.getParagraphNum()-1];
		run();
	}*/

    protected void runCode() {
    	clearAfterClick=false;
    	
    	
    	while(isRunning()&&getScenario()!=null&&nodeNum<getScenario().pieces.size()) {
    		Node node=getScenario().pieces.get(nodeNum++);
    		try {
    			getScene().appendLiteral(node.getLiteral(this));
    			node.run(this);
    		}catch(Throwable t) {
    			new ScenarioExecutionException("Unexpected error when executing scenario",t).printStackTrace();
    			this.sendMessage("Execution Exception when executing scenario: "+t.getMessage()+" see logs for more detail");
    			setStatus((RunStatus.STOPPED));
	    		getScene().clear();
	    		sendCachedSence();
    			break;
    		}
	    	if(getScenario()==null||nodeNum>=getScenario().pieces.size()) {
	    		
	    		setStatus((RunStatus.STOPPED));
	    		getScene().clear();
	    		sendCachedSence();
	    		return;
    		}
    	}
    	if(isRunning())
    		setStatus((RunStatus.STOPPED));
		
    }


    public void run(Scenario sp) {
		this.setScenario(sp);
		nodeNum=0;
		varData.takeSnapshot();
		getCurrentAct().newParagraph(sp, 0);

		run();
	}

	public void tick() {
		if(!inited)return;
    	//detect triggers
		if(getStatus()==RunStatus.RUNNING) {
			run();
		}
    	for(IScenarioTrigger t:triggers) {
    		if(t.test(this)) {
    			if(t.use()) {
    				if(t.isAsync())
						queue(t);
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
				olddata.saveActState();
			}
			olddata.getScene().clear();
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
			this.sp=data.getScenario();
			this.nodeNum=data.getNodeNum();
			this.status=data.getStatus();
			data.sendTitles(true, true);
			if(getStatus().shouldRun) {
				getScene().forcedClear();
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
		queue(new ActTarget(quest,target));
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
    public static void setup() {
        CapabilityManager.INSTANCE.register(ScenarioConductor.class, new Capability.IStorage<ScenarioConductor>() {
            public void readNBT(Capability<ScenarioConductor> capability, ScenarioConductor instance, Direction side, INBT nbt) {
                instance.deserializeNBT((CompoundNBT) nbt);
            }

            public INBT writeNBT(Capability<ScenarioConductor> capability, ScenarioConductor instance, Direction side) {
                return instance.serializeNBT();
            }
        }, ScenarioConductor::new);
    }
    public static LazyOptional<ScenarioConductor> getCapability(@Nullable PlayerEntity player) {
        if (player != null) {
            return player.getCapability(CAPABILITY);
        }
        return LazyOptional.empty();
    }
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == CAPABILITY)
            return capability.cast();
        return LazyOptional.empty();
	}
	@Override
	public CompoundNBT serializeNBT() {
		return save();
	}
	@Override
	public void deserializeNBT(CompoundNBT nbt) {
		load(nbt);
	}

}
