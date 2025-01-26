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

import java.util.LinkedList;

import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.frostedheart.content.scenario.runner.target.ExecuteStackElement;
import com.teammoeg.frostedheart.content.scenario.runner.target.ExecuteTarget;
import com.teammoeg.frostedheart.content.scenario.runner.target.ScenarioTarget;

import net.minecraft.nbt.CompoundTag;

/**
 * An act is a basic unit of execution code
 * You should NOT store this object, always get it from {@link ScenarioConductor#getCurrentAct()}
 * */
public class Act extends BaseScenarioRunner{
	@Override
	public String toString() {
		return "Act [name=" + name + ", sp=" + sp + ", nodeNum=" + nodeNum + ", status=" + status + "]";
	}
	final int actid;
	ActNamespace name;
    private String title="";
    private String subtitle="";

    public Act(int actid,ActNamespace name) {
		super();
		this.scene=new ServerScene();
		this.name=name;
		this.actid=actid;
		//nodeNum=-1;
	}
    public Act(int actid,CompoundTag data) {
		super();
		this.scene=new ServerScene();
		load(data);
		this.actid=actid;
	}
    public void prepareForRun(ScenarioContext ctx) {
    	//if(nodeNum<0) {
    		this.restoreLocation(ctx);
    	//}
    }
    public CompoundTag save() {
    	CompoundTag nbt=new CompoundTag();
    	if(currentLabel!=null)
    		CodecUtil.encodeNBT(ExecuteTarget.CODEC, nbt,"currentLabel",currentLabel);
    	
    	CodecUtil.encodeNBT(ExecuteStackElement.LIST_CODEC,nbt,"callStack",getCallStack());
    	nbt.putString("chapter", name.chapter());
    	nbt.putString("act", name.act());
    	nbt.putString("title", title);
    	nbt.putString("subtitle", subtitle);
    	nbt.put("scene", scene.save());
    	if(getStatus().doPersist) {
    		nbt.putInt("status", getStatus().ordinal());
    	}else {
    		nbt.putInt("status", RunStatus.RUNNING.ordinal());
    	}
    	return nbt;
    }
    public void load(CompoundTag nbt) {
    	currentLabel=CodecUtil.decodeNBTIfPresent(ExecuteTarget.CODEC, nbt, "currentLabel");
    	getCallStack().clear();
    	getCallStack().addAll(CodecUtil.decodeNBTIfPresent(ExecuteStackElement.LIST_CODEC, nbt, "callStack"));
    	name=new ActNamespace(nbt.getString("chapter"),nbt.getString("act"));
    	title=nbt.getString("title");
    	subtitle=nbt.getString("subtitle");
    	scene.load(nbt.getCompound("scene"));
    	setStatus((RunStatus.values()[nbt.getInt("status")]));
    	
    }

	@Override
	public void setExecutePos(int num) {
		this.nodeNum=num;
	}
	@Override
	public int getExecutePos() {
		// TODO Auto-generated method stub
		return nodeNum;
	}
	public RunStatus getStatus() {
		return status;
	}
	public void setStatus(RunStatus status) {
		//if(!name.isAct()&&status==RunStatus.RUNNING)
		//	new Exception().printStackTrace();
		this.status = status;
	}
    public String getTitle() {
		return title;
	}
	public String getSubtitle() {
		return subtitle;
	}
	public void sendTitles(ScenarioContext ctx,ScenarioThread thread,boolean updateT,boolean updateSt) {
		this.scene.sendTitles(ctx,thread, updateT?title:null, updateSt?subtitle:null);
		//FHNetwork.send(PacketDistributor.PLAYER.with(()->parent.getPlayer()), new ServerSenarioActPacket(updateT?title:null,updateSt?subtitle:null));
	}
	public void setTitles(ScenarioContext ctx,ScenarioThread thread,String t,String st) {
		//System.out.println(t+","+st);
		boolean b1 = false,b2 = false;
		if(t!=null&&!title.equals(t)) {
			this.title=t;
			b1=true;
		}
		if(st!=null&&!subtitle.equals(st)) {
			this.subtitle=st;
			b2=true;
		}
		sendTitles(ctx,thread,b1,b2);
	}
	public void setTitle(ScenarioContext ctx,ScenarioThread thread,String title) {
		if(title!=null&&!this.title.equals(title)) {
			this.title=title;
			sendTitles(ctx,thread,true,false);
		}
	}
	public void setSubtitle(ScenarioContext ctx,ScenarioThread thread,String subtitle) {
		if(subtitle!=null&&!this.subtitle.equals(subtitle)) {
			this.subtitle=subtitle;
			sendTitles(ctx,thread,false,true);
		}
	}
	public void setCallStack(LinkedList<ScenarioTarget> linkedList) {
		this.callStack.clear();;
		this.callStack.addAll(linkedList);
	}
	@Override
	public void stop() {
		super.stop();
		currentLabel=null;
	}
	@Override
	public int getRunId() {
		return actid;
	}
}
