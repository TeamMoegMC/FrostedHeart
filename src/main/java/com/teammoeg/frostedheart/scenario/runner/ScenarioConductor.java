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

import java.util.UUID;

import com.teammoeg.frostedheart.scenario.FHScenario;
import com.teammoeg.frostedheart.scenario.parser.Node;
import com.teammoeg.frostedheart.scenario.parser.ScenarioPiece;
import com.teammoeg.frostedheart.util.evaluator.Evaluator;
import com.teammoeg.frostedheart.util.evaluator.IEnvironment;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;

public class ScenarioConductor{
	public static class ExecuteTarget{
		String scenario;
		String label;
		public ExecuteTarget(String scenario, String label) {
			super();
			this.scenario = scenario;
			this.label = label;
		}
		
	}
    int paragraphNum;
    int nodeNum=0;
    private transient final SceneHandler paraData=new SceneHandler(this);
    private CompoundNBT varSnapshot;
    private transient final ScenarioVariables varData=new ScenarioVariables();
    private ScenarioPiece sp;
    public transient PlayerEntity player;
    public transient boolean waitClient;
    public boolean isNowait;
    public transient boolean isSkip;
    transient int waiting;
    transient int status;
    
    public ScenarioConductor(PlayerEntity player) {
		super();
		this.player = player;
	}
    public void addWait(int time) {
    	waiting+=time;
    }
    public void tick() {
    	if(!waitClient)
	    	if(waiting>0) {
	    		waiting--;
	    		if(waiting<=0)
	    			run();
	    	}
    }
    public ScenarioVariables getVaribles() {
    	return varData;
    }
    public SceneHandler getScene() {
    	return paraData;
    }
	public void run(ScenarioPiece sp) {
		this.sp=sp;
		nodeNum=0;
		paragraphNum=0;
		paraData.clear(paragraphNum);
		for(Node n:sp.pieces) {
			System.out.println(n.getText());
		}
		run();
	}
	public String createLink(String id,String scenario,String label) {
		if(id==null||getParagraph().links.containsKey(id)) {
			id=UUID.randomUUID().toString();
		}
		getParagraph().links.put(id, new ExecuteTarget(scenario,label));
		return id;
	}
	private SceneHandler getParagraph() {
		return paraData;
	}
	public void clearLink() {
		getParagraph().links.clear();
	}
	public void jump(ExecuteTarget target) {
		jump(target.scenario,target.label);
	}
	public void prepareTextualModification() {
		getScene().sendNoreline();
	}
	public void newLine() {
		getScene().sendNewline();
	}
	public void jump(String scenario,String label) {
		if(scenario!=null) {
			if(this.sp==null||this.sp.fileName!=scenario)
				this.sp=FHScenario.loadScenario(scenario);
			nodeNum=0;
			paragraphNum=0;
			paraData.clear(paragraphNum);
		}
		if(label!=null) {
			Integer ps=this.sp.labels.get(label);
			if(ps!=null) {
				nodeNum=ps;
			}
		}
		run();
	}
	boolean isRunning;
    public void run() {
    	if(isRunning)return;
    	try {
    		isRunning=true;
	    	while(!waitClient&&waiting<=0&&nodeNum<sp.pieces.size()) {
	    		Node node=sp.pieces.get(nodeNum);
	    		getParagraph().appendLiteral(node.getLiteral(this));
	    		node.run(this);
	    		nodeNum++;
	    		if(nodeNum>=sp.pieces.size()) {
	    			paragraph(-1);
	    		}
	    	}
    	}finally {
    		isRunning=false;
    	}
    }
    public void stop() {
    	nodeNum=sp.pieces.size();
    }

    public void waitClient() {
    	if(getScene().shouldWaitClient())
    		waitClient=true;
    }
	public void notifyClientResponse(boolean isSkip,int status) {
		if(waitClient) {
			waitClient=false;
			this.isSkip=isSkip;
			this.status=status;
			run();
		}
    }
	public void paragraph(int pn) {
		paragraphNum=pn;
		varSnapshot=varData.takeSnapshot();
		paraData.clear(paragraphNum);
		
	}
    public double eval(String exp) {
        return Evaluator.eval(exp).eval(varData);
    }

    public CompoundNBT getExecutionData() {
        return paraData.executionData;
    }

    public int getNodeNum() {
        return nodeNum;
    }


    public ServerPlayerEntity getPlayer() {
        return (ServerPlayerEntity) player;
    }

    public void gotoNode(int target) {
        nodeNum = target;
    }




	public void onLinkClicked(String link) {
		ExecuteTarget jt=getParagraph().links.get(link);
		if(jt!=null) {
			jump(jt);
		}
	}

}
