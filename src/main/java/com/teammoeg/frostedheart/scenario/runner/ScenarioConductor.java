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
import com.teammoeg.frostedheart.util.evaluator.Evaluator;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;

/**
 * ScenarioConductor
 * Main class for conducting and directing scenarios.
 * You shouldn't opearte this class from any other code except from scenario trigger and commands.
 * You should define triggers in script file and activate triggers to make it execute.
 * */
public class ScenarioConductor{
	//Conducting and scheduling data 
	private transient Scenario sp;//current scenario
	private transient int nodeNum=0;//Program register
	private transient RunStatus running;
	private transient LinkedList<Consumer<ScenarioConductor>> toExecute=new LinkedList<>();//Actions appended by trigger and awaiting execution
    private transient List<TriggerTarget> triggers=new ArrayList<>();
    private transient Map<String,ExecuteStackElement> macros=new HashMap<>();
    private final ScenarioVariables varData=new ScenarioVariables();

    //Text handling state machine

    private transient int waiting;
    private transient boolean isConducting;
    
    //Integrated Quest info
    public QuestData currentQuestData;
    public Map<ImmutableQuestNamespace,QuestData> quests=new HashMap<>();
    private transient boolean isQuestEnabled;
    
    //Server and Client info
    public transient PlayerEntity player;
    public transient boolean isSkip;
    private transient int clientStatus;
    
    public void enableQuest() {
    	if(!isQuestEnabled) {
    		isQuestEnabled=true;
    		
    		quests.values().forEach(t->{
    			if(t.isWaiting) {
    				t.getScene().setSlient(true);
    				this.queue(t.getExecutionPoint());
    			}
    		});
    	}
    }
    public ScenarioConductor(PlayerEntity player) {
		super();
		this.player = player;
	}
	public void addWait(int time) {
    	waiting+=time;
    	running=RunStatus.WAITING;
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
			currentQuestData.getScene().executionData.put("mp", mp);
			addCallStack();
			restorePosition(macros.get(name));
		}else
			FHScenario.callCommand(name, this, params);
	}
	public void addCallStack() {
		currentQuestData.callStack.add(getCurrentPosition());
	}
	public void call(ExecuteTarget target) {
		addCallStack();
		jump(target);
	}
    public void clearLink() {
		getParagraph().links.clear();
	}
    public String createLink(String id,String scenario,String label) {
		if(id==null||getParagraph().links.containsKey(id)) {
			id=UUID.randomUUID().toString();
		}
		getParagraph().links.put(id, new ExecuteTarget(scenario,label));
		return id;
	}
    public void popCallStack() {
		if(currentQuestData.callStack.isEmpty()) {
			throw new ScenarioExecutionException("Invalid return at "+getScenario().name);
		}
		restorePosition(currentQuestData.callStack.pollLast());
	}
    public double eval(String exp) {
        return Evaluator.eval(exp).eval(getVaribles());
    }
    public ExecuteStackElement getCurrentPosition() {
    	return new ExecuteStackElement(getScenario(),nodeNum);
    }
	public CompoundNBT getExecutionData() {
        return getScene().executionData;
    }
	
	public int getNodeNum() {
        return nodeNum;
    }
	private SceneHandler getParagraph() {
		return currentQuestData.getScene();
	}
	public ServerPlayerEntity getPlayer() {
        return (ServerPlayerEntity) player;
    }
	public SceneHandler getScene() {
    	return currentQuestData.getScene();
    }
	public ScenarioVariables getVaribles() {
    	return varData;
    }
	public void gotoNode(int target) {
        nodeNum = target;
    }
	public boolean isRunning() {
		return running==RunStatus.RUNNING;
	}
	public void jump(Consumer<ScenarioConductor> nxt) {
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
		if(running==RunStatus.WAITCLIENT) {
			this.isSkip=isSkip;
			this.clientStatus=status;
			run();
		}
    }
    public void onLinkClicked(String link) {
		ExecuteTarget jt=getParagraph().links.get(link);
		if(jt!=null) {
			jump(jt);
		}
	}

    public void paragraph(int pn) {
    	varData.takeSnapshot();
    	currentQuestData.newParagraph(getScenario(), pn);
		
	}
	public void prepareTextualModification() {
		getScene().sendNoreline();
	}
	public void queue(Consumer<ScenarioConductor> questExecuteTarget) {
		toExecute.add(questExecuteTarget);
	}
    public void restoreParagraph(ParagraphData paragraph) {
		Scenario sp=paragraph.getScenario();
		if(paragraph.getParagraphNum()==0)
			nodeNum=0;
		else
			nodeNum=sp.paragraphs[paragraph.getParagraphNum()-1];
		run();
	}

    public void restorePosition(ExecuteStackElement target) {
		
		target.accept(this);
		run();
	}
    /**
	 * Break any waits and stop, Execute now.
	 * 
	 * 
	 * */
    private void run() {
    	running=RunStatus.RUNNING;
    	if(isConducting)return;
    	try {
    		isConducting=true;
    		while(isRunning()) {
	    		running=RunStatus.RUNNING;
		    	while(isRunning()&&getScenario()!=null&&nodeNum<getScenario().pieces.size()) {
		    		Node node=getScenario().pieces.get(nodeNum);
		    		try {
		    			getParagraph().appendLiteral(node.getLiteral(this));
		    			node.run(this);
		    		}catch(Throwable t) {
		    			new ScenarioExecutionException("Unexpected error when executing scenario",t).printStackTrace();
		    			running=RunStatus.STOPPED;
		    			break;
		    		}
		    		nodeNum++;
		    	}
		    	if(getScenario()==null||nodeNum>=getScenario().pieces.size()) {
	    			running=RunStatus.STOPPED;
	    		}
		    	if(running==RunStatus.STOPPED) {
		    		paragraph(-1);
		    		Consumer<ScenarioConductor> nxt=toExecute.pollFirst();
		    		if(nxt!=null)
		    			jump(nxt);
		    	}
    		}
    	}finally {
    		isConducting=false;
    	}
    }


    public void run(Scenario sp) {
		this.setScenario(sp);
		nodeNum=0;
		varData.takeSnapshot();
		currentQuestData.newParagraph(sp, 0);
		run();
	}

    public void stop() {
    	nodeNum=getScenario().pieces.size();
    }




	public void tick() {
    	if(running==RunStatus.WAITING)
	    	if(waiting>0) {
	    		waiting--;
	    		if(waiting<=0)
	    			run();
	    	}
    	
    	//detect triggers
    	for(TriggerTarget t:triggers) {
    		if(t.doTrigger(this)) {
    			if(t.maxtrigger>0) {
    				t.maxtrigger--;
    			}
    			this.queue(t);
    		}
    	}
    	triggers.removeIf(t->t.maxtrigger==0);
    	
    	//Execute Queued actions
    	if(running==RunStatus.STOPPED&&!toExecute.isEmpty()) {
    		run();
    	}
    }
	public void waitClient() {
    	if(getScene().shouldWaitClient())
    		running=RunStatus.WAITCLIENT;
    }
	public void switchQuest(String chapter,String quest) {
		ImmutableQuestNamespace old=currentQuestData.currentQuest.asImmutable();
		QuestData olddata=quests.remove(old);
		if(olddata==null)
			olddata=new QuestData(this);
		olddata.isWaiting=false;
		currentQuestData.currentQuest.chapter=chapter;
		currentQuestData.currentQuest.quest=quest;
		currentQuestData=olddata;
		quests.put(currentQuestData.currentQuest.asImmutable(), olddata);
	}
	public void pauseQuest() {
		if(currentQuestData.currentQuest.has()) {
			ImmutableQuestNamespace old=currentQuestData.currentQuest.asImmutable();
			QuestData olddata=currentQuestData;
			quests.put(old, olddata);
		}
		emptyQuest();
		
	}
	private void emptyQuest() {
		ImmutableQuestNamespace empty=new ImmutableQuestNamespace("","");
		currentQuestData=quests.get(empty);
	}
	public void continueQuest(ImmutableQuestNamespace quest) {
		
		QuestData data=quests.get(quest);
		if(data!=null) {
			pauseQuest();
			this.currentQuestData=data;
			this.jump(data.getExecutionPoint());
		}
	}
	public void endQuest() {
		quests.remove(currentQuestData.currentQuest.asImmutable());
		emptyQuest();
	}
	public Scenario getScenario() {
		return sp;
	}
	public void setScenario(Scenario sp) {
		this.sp = sp;
	}
}
