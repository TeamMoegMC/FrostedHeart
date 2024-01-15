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
import java.util.function.Consumer;
import java.util.UUID;

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
public class ScenarioConductor{
	//Conducting and scheduling data 

	
	private transient LinkedList<IScenarioTarget> toExecute=new LinkedList<>();//Actions appended by trigger and awaiting execution
    private transient List<IScenarioTrigger> triggers=new ArrayList<>();
    private transient Map<String,ExecuteStackElement> macros=new HashMap<>();
    private final ScenarioVariables varData=new ScenarioVariables();

    //Text handling state machine


    private transient boolean isConducting;
    
    //Sence control
    private transient Act currentAct=new Act(this,empty);
    public Map<ActNamespace,Act> acts=new HashMap<>();
    private transient boolean isQuestEnabled;
    
    //Server and Client info
    public transient PlayerEntity player;
    public transient boolean isSkip;
    private transient int clientStatus;
    private static final ActNamespace empty=new ActNamespace();
    public CompoundNBT save() {
    	CompoundNBT data=new CompoundNBT();
    	data.put("vars", varData.snapshot);
    	ListNBT lacts=new ListNBT();
    	for(Act v:acts.values()) {
    		lacts.add(v.save());
    	}
    	data.put("acts", lacts);
    	data.putString("chapter", currentAct.name.chapter);
    	data.putString("act", currentAct.name.act);
    	
    	return data;
    }
    public void load(CompoundNBT data) {
    	varData.snapshot=data.getCompound("vars");
    	ListNBT lacts=data.getList("acts", Constants.NBT.TAG_COMPOUND);
    	for(INBT v:lacts) {
    		Act i=new Act(this,(ActNamespace) v);
    		acts.put(i.name, i);
    	}
    	currentAct=acts.get(new ActNamespace(data.getString("chapter"),data.getString("act")));
    	if(currentAct==null)
    		currentAct=acts.get(empty);
    }
    public void enableQuest() {
    	if(!isQuestEnabled) {
    		isQuestEnabled=true;
    		
    		acts.values().forEach(t->{
    			if(t.getStatus()==RunStatus.WAITTRIGGER) {
    				//t.getScene().setSlient(true);
    				t.queue(t.paragraph);
    				//t.getScene().setSlient(false);
    			}
    		});
    	}
    }
    public ScenarioConductor(PlayerEntity player) {
		super();
		this.player = player;
		if(acts.isEmpty()) {
			acts.put(empty, currentAct);
		}
	}

	public void call(String scenario, String label) {
		call(new ExecuteTarget(scenario,label));
	}
	public void callCommand(String name,Map<String,String> params) {
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
		getCurrentAct().addCallStack();
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
		return id;
	}
    public void popCallStack() {
		if(getCurrentAct().callStack.isEmpty()) {
			throw new ScenarioExecutionException("Invalid return at "+getScenario().name);
		}
		jump(getCurrentAct().callStack.pollLast());
	}
    public double eval(String exp) {
        return Evaluator.eval(exp).eval(getVaribles());
    }

	public CompoundNBT getExecutionData() {
        return getScene().executionData;
    }
	
	public int getNodeNum() {
        return getCurrentAct().nodeNum;
    }
	public ServerPlayerEntity getPlayer() {
        return (ServerPlayerEntity) player;
    }
	public Scene getScene() {
    	return getCurrentAct().getScene();
    }
	public ScenarioVariables getVaribles() {
    	return varData;
    }
	public void gotoNode(int target) {
		getCurrentAct().nodeNum = target;
    }
	public boolean isRunning() {
		return getCurrentAct().getStatus()==RunStatus.RUNNING;
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
		getScene().sendNewline();
	}
	public void notifyClientResponse(boolean isSkip,int status) {
		if(getCurrentAct().getStatus()==RunStatus.WAITCLIENT) {
			this.isSkip=isSkip;
			this.clientStatus=status;
			run();
		}
    }
    public void onLinkClicked(String link) {
		ExecuteTarget jt=getScene().links.get(link);
		if(jt!=null) {
			jump(jt);
		}
	}

    public void paragraph(int pn) {
    	varData.takeSnapshot();
    	getCurrentAct().newParagraph(getScenario(), pn);
		
	}
	public void prepareTextualModification() {
		getScene().sendNoreline();
	}
	public void addTrigger(IScenarioTrigger trig) {
		if(currentAct.name.isAct()) {
			currentAct.getScene().addTrigger(trig);
		}else {
			triggers.add(trig);
		}
	}
	public void queue(IScenarioTarget questExecuteTarget) {
		toExecute.add(questExecuteTarget);
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
    	getCurrentAct().setStatus(RunStatus.RUNNING);
    	while(isRunning()) {
    		getCurrentAct().setStatus(RunStatus.RUNNING);
	    	while(isRunning()&&getScenario()!=null&&getCurrentAct().nodeNum<getScenario().pieces.size()) {
	    		Node node=getScenario().pieces.get(getCurrentAct().nodeNum);
	    		try {
	    			getScene().appendLiteral(node.getLiteral(this));
	    			node.run(this);
	    		}catch(Throwable t) {
	    			new ScenarioExecutionException("Unexpected error when executing scenario",t).printStackTrace();
	    			getCurrentAct().setStatus(RunStatus.STOPPED);
	    			break;
	    		}
	    		getCurrentAct().nodeNum++;
	    	}
	    	if(getScenario()==null||getCurrentAct().nodeNum>=getScenario().pieces.size()) {
	    		getCurrentAct().setStatus(RunStatus.STOPPED);
    		}

		}
    }
    private void runScheduled() {
    	if(getCurrentAct().getStatus().shouldPause) {
    		paragraph(-1);
    		IScenarioTarget nxt=toExecute.pollFirst();
    		if(nxt!=null) {
    			globalScope();
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
		getCurrentAct().nodeNum=0;
		varData.takeSnapshot();
		getCurrentAct().newParagraph(sp, 0);
		run();
	}

    public void stop() {
    	getCurrentAct().nodeNum=getScenario().pieces.size();
    }




	public void tick() {    	
    	//detect triggers
    	for(IScenarioTrigger t:triggers) {
    		if(t.test(this)) {
    			if(t.use())
    				this.queue(t);
    		}
    	}
    	for(Act a:acts.values())
	    	a.getScene().tickTriggers(this, currentAct==a);
    	triggers.removeIf(t->!t.canUse());
    	if(getCurrentAct().getStatus()==RunStatus.WAITTIMER)
    		if(getScene().tickWait()) {
    			run();
    			return;
    		}
    	//Execute Queued actions
    	if(getCurrentAct().getStatus().shouldPause&&!toExecute.isEmpty()) {
    		run();
    	}
    }

	/*public void switchQuest(String chapter,String quest) {
		QuestNamespace old=currentAct.currentQuest.asImmutable();
		Act olddata=acts.remove(old);
		if(olddata==null)
			olddata=new Act(this);
		olddata.isWaiting=false;
		currentAct.currentQuest.chapter=chapter;
		currentAct.currentQuest.quest=quest;
		currentAct=olddata;
		acts.put(currentAct.currentQuest.asImmutable(), olddata);
	}*/
	public void pauseQuest() {
		if(getCurrentAct().name.isAct()) {
			ActNamespace old=getCurrentAct().name;
			Act olddata=getCurrentAct();
			if(olddata.getStatus()==RunStatus.WAITACTION) {
				olddata.paragraph.accept(this);
			}
			varData.restoreSnapshot();
			acts.put(old, olddata);
			globalScope();
		}else {
			varData.takeSnapshot();
		}
		
		
	}
	private void globalScope() {
		
		currentAct=acts.get(empty);
	}
	public void createQuest(ActNamespace quest) {
		if(quest.equals(getCurrentAct().name))return;
		Act data=acts.get(quest);
		pauseQuest();
		if(data!=null) {
			this.currentAct=data;
			if(data.getStatus().shouldRun)
				run();
		}else {
			data=new Act(this,quest);
			acts.put(quest, data);
			this.currentAct=data;
			run();
		}
	}
	public void continueQuest(ActNamespace quest) {
		if(quest.equals(getCurrentAct().name))return;
		Act data=acts.get(quest);
		if(data!=null) {
			pauseQuest();
			this.currentAct=data;
			if(data.getStatus().shouldRun)
				run();
		}
	}
	public void endQuest() {
		acts.remove(getCurrentAct().name);
		globalScope();
	}
	public Scenario getScenario() {
		return getCurrentAct().sp;
	}
	public void setScenario(Scenario sp) {
		getCurrentAct().sp = sp;
	}
	public int getClientStatus() {
		return clientStatus;
	}
	public Act getCurrentAct() {
		return currentAct;
	}
}
