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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import com.teammoeg.frostedheart.research.data.FHResearchDataManager;
import com.teammoeg.frostedheart.scenario.FHScenario;
import com.teammoeg.frostedheart.scenario.ScenarioExecutionException;
import com.teammoeg.frostedheart.scenario.parser.Node;
import com.teammoeg.frostedheart.scenario.parser.Scenario;
import com.teammoeg.frostedheart.scenario.runner.target.ExecuteStackElement;
import com.teammoeg.frostedheart.scenario.runner.target.ExecuteTarget;
import com.teammoeg.frostedheart.scenario.runner.target.IScenarioTarget;
import com.teammoeg.frostedheart.util.evaluator.Evaluator;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.common.util.Constants;

/**
 * ScenarioConductor
 * Main class for conducting and directing scenarios.
 * You shouldn't opearte this class from any other code except from scenario trigger and commands.
 * You should define triggers in script file and activate triggers to make it execute.
 * */
public class ScenarioConductor implements IScenarioConductor{
	//Conducting and scheduling data 

	
	private transient LinkedList<IScenarioTarget> toExecute=new LinkedList<>();//Actions appended by trigger and awaiting execution
    private transient List<IScenarioTrigger> triggers=new ArrayList<>();
    private transient Map<String,ExecuteStackElement> macros=new HashMap<>();
    private final ScenarioVariables varData=new ScenarioVariables();
	transient Scenario sp;//current scenario
	transient int nodeNum=0;//Program register
	private RunStatus status=RunStatus.STOPPED;
    //Text handling state machine
	private transient LinkedList<ExecuteStackElement> callStack=new LinkedList<>();

    private transient boolean isConducting;
    private transient int nextParagraph;
    //Sence control
    private transient Act currentAct;
    public Map<ActNamespace,Act> acts=new HashMap<>();
    private transient boolean isActsEnabled;
    private transient ActNamespace lastQuest;
    //Server and Client info
    public transient UUID player;
    public transient boolean isSkip;
    private transient int clientStatus;
    private static final ActNamespace global=new ActNamespace();
    private static final ActNamespace init=new ActNamespace(null,null);
    public CompoundNBT save() {
    	CompoundNBT data=new CompoundNBT();
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
    	varData.snapshot=data.getCompound("vars");
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
    		
    		acts.values().forEach(t->{
    			if(t.getStatus()==RunStatus.WAITTRIGGER) {
    				//t.getScene().setSlient(true);
    				t.setStatus(RunStatus.RUNNING);
    				if(t.name.equals(lastQuest))lastQuest=null;
    				t.queue(t.paragraph);
    				//t.getScene().setSlient(false);
    			}
    		});
    		if(lastQuest!=null) {
    			acts.get(lastQuest).queue(acts.get(lastQuest).paragraph);
    		}
    	}
    }
    public ScenarioConductor(PlayerEntity player) {
		super();
		this.player = player.getUniqueID();
		setCurrentAct(new Act(this,init));
		acts.put(init, getCurrentAct());
		acts.put(global, new Act(this,global));
		
	}
    public ScenarioConductor(PlayerEntity player,CompoundNBT data) {
		super();
		this.player = player.getUniqueID();
		load(data);
		setCurrentAct(new Act(this,init));
		acts.put(init, getCurrentAct());
	}
	public void call(String scenario, String label) {
		call(new ExecuteTarget(scenario,label));
	}
	public void callCommand(String name,Map<String,String> params) {
		name=name.toLowerCase();
		if(macros.containsKey(name)) {
			CompoundNBT mp=new CompoundNBT();
			for(Entry<String, String> e:params.entrySet()) {
				mp.putString(e.getKey(), e.getValue());
			}
			varData.extraData.put("mp", mp);
			call(macros.get(name));
		}else
			FHScenario.callCommand(name, this, params);
	}

	public void addCallStack() {
		getCallStack().add(getCurrentPosition());
	}
	public void call(IScenarioTarget target) {
		addCallStack();
		jump(target);
	}
    public void clearLink() {
    	getScene().clearLink();
	}
    public String createLink(String id,String scenario,String label) {
		if(id==null||getScene().links.containsKey(id)) {
			id=UUID.randomUUID().toString();
		}
		getScene().links.put(id, new ExecuteTarget(scenario,label));
		getScene().markChatboxDirty();
		return id;
	}
    public void popCallStack() {
		if(getCallStack().isEmpty()) {
			throw new ScenarioExecutionException("Invalid return at "+getScenario().name);
		}
		jump(getCallStack().pollLast());
	}
    public double eval(String exp) {
        return Evaluator.eval(exp).eval(getVaribles());
    }

	public int getNodeNum() {
        return nodeNum;
    }
	public ServerPlayerEntity getPlayer() {
        return FHResearchDataManager.server.getPlayerList().getPlayerByUUID(player);
    }
	public boolean isOfflined() {
		return FHResearchDataManager.server.getPlayerList().getPlayerByUUID(player)==null;
	}
	public Scene getScene() {
    	return getCurrentAct().getScene();
    }
	public ScenarioVariables getVaribles() {
    	return varData;
    }
	public void gotoNode(int target) {
		nodeNum = target;
    }
	public boolean isRunning() {
		return getStatus()==RunStatus.RUNNING;
	}
	public void jump(IScenarioTarget nxt) {
		nxt.accept(this);
		run();

	}
	public void jump(String scenario,String label) {
		if(scenario==null)
			jump(new ExecuteTarget(scenario,label));
		else
			jump(new ExecuteTarget(getScenario(),label));
	}
	public void newLine() {
		getScene().sendNewLine();
		//getScene().sendCurrent();
	}
	public void notifyClientResponse(boolean isSkip,int status) {
		this.isSkip=isSkip;
		this.clientStatus=status;
		if(this.getStatus()==RunStatus.WAITCLIENT) {
			if(nextParagraph>=0)
				doParagraph(nextParagraph);
			run();
		}else if(this.getStatus()==RunStatus.WAITTIMER&&isSkip) {
			this.stopWait();
			run();
		}
    }
    public void stopWait() {
		getScene().stopWait();
	}
	public void onLinkClicked(String link) {
		ExecuteTarget jt=getScene().links.get(link);
		if(jt!=null) {
			jump(jt);
		}
	}
	private void doParagraph(int pn) {
		varData.takeSnapshot();
    	getCurrentAct().newParagraph(getScenario(), pn);
    	nextParagraph=-1;
	}
    public void paragraph(int pn) {
    	if(getScene().shouldWaitClient()) {
    		getScene().waitClientIfNeeded();
    		nextParagraph=pn;
    	}else doParagraph(pn);
	}
	public void sendCachedSence() {
		getScene().sendCurrent();
	}
	public void addTrigger(IScenarioTrigger trig) {
		if(getCurrentAct().name.isAct()) {
			getCurrentAct().getScene().addTrigger(trig);
		}else {
			triggers.add(trig);
		}
	}
	public void queue(IScenarioTarget questExecuteTarget) {
		getCurrentAct().queue(questExecuteTarget);
		
	}
    /*public void restoreParagraph(ParagraphData paragraph) {
		Scenario sp=paragraph.getScenario();
		if(paragraph.getParagraphNum()==0)
			currentQuestData.nodeNum=0;
		else
			currentQuestData.nodeNum=sp.paragraphs[paragraph.getParagraphNum()-1];
		run();
	}*/

    private void runCode() {
    	nextParagraph=-1;
    	setStatus((RunStatus.RUNNING));
    	while(isRunning()) {
	    	while(isRunning()&&getScenario()!=null&&nodeNum<getScenario().pieces.size()) {
	    		Node node=getScenario().pieces.get(nodeNum++);
	    		try {
	    			getScene().appendLiteral(node.getLiteral(this));
	    			node.run(this);
	    		}catch(Throwable t) {
	    			new ScenarioExecutionException("Unexpected error when executing scenario",t).printStackTrace();
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
		}
    }
    private void runScheduled() {
    	if(getStatus().shouldPause) {
    		IScenarioTarget nxt=toExecute.pollFirst();
    		if(nxt!=null) {
    			//globalScope();
    			//paragraph(-1);
    			jump(nxt);
    		}
    	}
    }
    /**
	 * Break any waits and stop, Execute now.
	 * 
	 * 
	 * */
    private void run() {
    	
    	if(isConducting)return;
    	try {
    		isConducting=true;
    		runCode();
    		runScheduled();
    	}finally {
    		isConducting=false;
    	}
    }


    public void run(Scenario sp) {
		this.setScenario(sp);
		nodeNum=0;
		varData.takeSnapshot();
		getCurrentAct().newParagraph(sp, 0);
		for(Node n:sp.pieces)
			System.out.println(n.getText());
		run();
	}

    public void stop() {
    	nodeNum=getScenario().pieces.size();
    	setStatus(RunStatus.STOPPED);
    }




	public void tick() {    	
    	//detect triggers
		if(getStatus()==RunStatus.RUNNING) {
			run();
		}
    	for(IScenarioTrigger t:triggers) {
    		if(t.test(this)) {
    			if(t.use())
    				this.queue(t);
    		}
    	}
    	for(Act a:acts.values())
	    	a.getScene().tickTriggers(this, getCurrentAct()==a);
    	triggers.removeIf(t->!t.canUse());
    	if(getStatus()==RunStatus.WAITTIMER) {
    		if(getScene().tickWait()) {
    			
    			run();
    			return;
    		}
    	}
    	//Execute Queued actions
    	if(getStatus().shouldPause&&!toExecute.isEmpty()) {
    		run();
    	}
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
		Act data=acts.get(quest);
		pauseAct();
		if(data!=null) {
			this.setCurrentAct(data);
		}else {
			data=new Act(this,quest);
			acts.put(quest, data);
			this.setCurrentAct(data);
		}
	}
	public void queueAct(ActNamespace quest,String scene,String label) {
		Act data=getCurrentAct();
		if(!quest.equals(getCurrentAct().name)) {
			acts.get(quest);
			pauseAct();
			if(data==null){
				data=new Act(this,quest);
				acts.put(quest, data);
			}
		}

		if(label!=null) {
			data.label=label;
			new ExecuteTarget(scene,label).apply(data);
		}else {
			new ExecuteTarget(scene,null).apply(data);
		}
		data.paragraph.setScenario(data.getScenario());
		data.paragraph.setParagraphNum(0);
	}
	public ExecuteStackElement getCurrentPosition() {
    	return new ExecuteStackElement(sp,nodeNum);
    }
	public void endAct() {
		if(getCurrentAct().name.isAct()) {
			acts.remove(getCurrentAct().name);
		}
		globalScope();
	}
	public Scenario getScenario() {
		return sp;
	}
	public void setScenario(Scenario sp) {
		this.sp = sp;
	}
	public int getClientStatus() {
		return clientStatus;
	}
	public Act getCurrentAct() {
		return currentAct;
	}
	public void addMacro(String name) {
		macros.put(name.toLowerCase(), getCurrentPosition().next());
	}
	@Override
	public void setNodeNum(int num) {
		gotoNode(num);
	}
	public RunStatus getStatus() {
		return status;
	}
	public void setStatus(RunStatus status) {
		this.status = status;
	}
	public LinkedList<ExecuteStackElement> getCallStack() {
		return callStack;
	}
	private void setCurrentAct(Act currentAct) {
		this.currentAct = currentAct;
	}
}
