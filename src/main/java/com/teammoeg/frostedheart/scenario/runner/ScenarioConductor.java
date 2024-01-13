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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.teammoeg.frostedheart.FHPacketHandler;
import com.teammoeg.frostedheart.scenario.FHScenario;
import com.teammoeg.frostedheart.scenario.network.ServerSenarioTextPacket;
import com.teammoeg.frostedheart.scenario.parser.Node;
import com.teammoeg.frostedheart.scenario.parser.ScenarioPiece;
import com.teammoeg.frostedheart.util.evaluator.Evaluator;
import com.teammoeg.frostedheart.util.evaluator.IEnvironment;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.network.PacketDistributor;

public class ScenarioConductor implements IEnvironment {
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
    public ParagraphData paraData;
    ArrayList<FlowElement> flowControlStack = new ArrayList<>();

    ScenarioPiece sp;
    public PlayerEntity player;
    File folder = FMLPaths.CONFIGDIR.get().toFile();
    File rf = new File(folder, "fhresearches");
    public boolean waitClient;
    public boolean isNowait;
    public boolean isSkip;
    boolean lastIsReline;
    int waiting;
    int status;
    StringBuilder currentLiteral;
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
    public ParagraphData getParagraph() {
    	if(paraData==null)
    		paraData=new ParagraphData(paragraphNum);
    	return paraData;
    }
	public void run(ScenarioPiece sp) {
		this.sp=sp;
		nodeNum=0;
		paragraphNum=0;
		paraData=null;
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
	public void clearLink() {
		getParagraph().links.clear();
	}
	public void jump(ExecuteTarget target) {
		jump(target.scenario,target.label);
	}
	public void jump(String scenario,String label) {
		if(scenario!=null) {
			if(this.sp==null||this.sp.fileName!=scenario)
				this.sp=FHScenario.loadScenario(scenario);
			nodeNum=0;
			paragraphNum=0;
			paraData=null;
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
	    		if(currentLiteral==null)
	        		currentLiteral=new StringBuilder();
	    		Node node=sp.pieces.get(nodeNum);
	    		currentLiteral.append(node.getDisplay(this));
	    		node.run(this);
	    		nodeNum++;
	    		if(nodeNum>=sp.pieces.size()) {
	    			sendNormal();
	    		}
	    	}
    	}finally {
    		isRunning=false;
    	}
    }
    public void stop() {
    	nodeNum=sp.pieces.size();
    }
    public boolean shouldWaitClient() {
    	return currentLiteral!=null&&currentLiteral.length()!=0;
    }
    
    public void sendNormal() {
    	if(currentLiteral!=null&&currentLiteral.length()>0) {
    		//System.out.println("Reline "+currentLiteral.toString());
    		FHPacketHandler.send(PacketDistributor.PLAYER.with(()->(ServerPlayerEntity)player), new ServerSenarioTextPacket(currentLiteral.toString(),true,isNowait));
    	}else if(lastIsReline) {
    		//System.out.println("Reline");
    		FHPacketHandler.send(PacketDistributor.PLAYER.with(()->(ServerPlayerEntity)player), new ServerSenarioTextPacket("",true,isNowait));
    		lastIsReline=false;
    	}
    	currentLiteral=null;
    }
    public void sendNoreline() {
    	if(currentLiteral!=null&&currentLiteral.length()>0) {
    		//System.out.println("NoReline "+currentLiteral.toString());
    		lastIsReline=true;
    		FHPacketHandler.send(PacketDistributor.PLAYER.with(()->(ServerPlayerEntity)player), new ServerSenarioTextPacket(currentLiteral.toString(),false,isNowait));
    	}
    	currentLiteral=null;
    }
    public void waitClient() {
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
		paraData=null;
		if(shouldWaitClient())
			waitClient();
		sendNormal();
		
	}
    public boolean containsPath(String path) {
        return getParagraph().containsPath(path);
    }

    public double eval(String exp) {
        return Evaluator.eval(exp).eval(this);
    }

    public INBT evalPath(String path) {
        return getParagraph().evalPath(path);
    }

    public Double evalPathDouble(String path) {
        return getParagraph().evalPathDouble(path);
    }

    public String evalPathString(String path) {
        return getParagraph().evalPathString(path);
    }

    @Override
    public double get(String key) {

        return evalPathDouble(key);
    }

    public FlowElement getByCaller(int caller) {
        for (int i = flowControlStack.size() - 1; i >= 0; i--) {
            FlowElement cur = flowControlStack.get(i);
            if (cur.getCaller() == caller)
                return cur;
        }
        return null;
    }

    public CompoundNBT getExecutionData() {
        return null;
    }

    public FlowElement getLast() {
        return flowControlStack.get(flowControlStack.size() - 1);
    }

    public int getNodeNum() {
        return nodeNum;
    }

    @Override
    public Double getOptional(String key) {
        if (!containsPath(key))
            return null;
        return get(key);
    }

    public ServerPlayerEntity getPlayer() {
        return (ServerPlayerEntity) player;
    }

    public void jump(int target) {
        nodeNum = target;
    }

    public boolean popCall() {
        if (flowControlStack.size() > 0) {
            nodeNum = flowControlStack.remove(flowControlStack.size() - 1).getTarget();
            return true;
        }
        return false;
    }

    public boolean popCaller(int caller) {
        if (flowControlStack.size() > 0) {
            if (getLast().getCaller() == caller) {
                nodeNum = flowControlStack.remove(flowControlStack.size() - 1).getTarget();
                return true;
            }
        }
        return false;
    }

    public void pushCall(int caller, int target) {
        flowControlStack.add(new FlowElement(caller, target));
    }

    public boolean removeCall() {
        if (flowControlStack.size() > 0) {
            flowControlStack.remove(flowControlStack.size() - 1).getTarget();
            return true;
        }
        return false;
    }

    public boolean removeCaller(int caller) {
        if (flowControlStack.size() > 0) {
            if (getLast().getCaller() == caller) {
                flowControlStack.remove(flowControlStack.size() - 1).getTarget();
                return true;
            }
        }
        return false;
    }

    @Override
    public void set(String key, double v) {
        setPathNumber(key, v);
    }

    public void setNodeNum(int nodeNum) {
        this.nodeNum = nodeNum;
    }

    public void setPath(String path, INBT val) {
        getParagraph().setPath(path, val);
    }

    public void setPathNumber(String path, Number val) {
        getParagraph().setPathNumber(path, val);
    }

    public void setPathString(String path, String val) {
        getParagraph().setPathString(path, val);
    }
	public void onLinkClicked(String link) {
		ExecuteTarget jt=getParagraph().links.get(link);
		if(jt!=null) {
			jump(jt);
		}
	}

}
