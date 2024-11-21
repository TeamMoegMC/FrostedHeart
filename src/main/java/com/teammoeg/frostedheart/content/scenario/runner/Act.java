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

import com.teammoeg.frostedheart.content.scenario.runner.target.ExecuteStackElement;
import com.teammoeg.frostedheart.util.io.CodecUtil;

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
	String label;
	
	ActNamespace name;
    private String title="";
    private String subtitle="";

	private LinkedList<ExecuteStackElement> callStack=new LinkedList<>();
    public Act(ActNamespace name) {
		super();
		this.scene=new ServerScene();
		this.name=name;
		nodeNum=-1;
	}
    public Act(CompoundTag data) {
		super();
		this.scene=new ServerScene();
		load(data);
	}
    public void prepareForRun(ScenarioContext ctx) {
    	if(nodeNum<0) {
    		this.restoreLocation(ctx);
    		
    		if(label!=null) {
    			Integer nn=sp.labels().get(label);
    			if(nn!=null)
    				this.nodeNum=nn;
    			label=null;
    		}
    	}
    }
    public CompoundTag save() {
    	CompoundTag nbt=new CompoundTag();
    	if(savedLocation!=null)
    		CodecUtil.encodeNBT(ParagraphData.CODEC, nbt,"location",savedLocation);
    	
    	CodecUtil.encodeNBT(ExecuteStackElement.LIST_CODEC,nbt,"callStack",callStack);
    	nbt.putString("chapter", name.chapter());
    	nbt.putString("act", name.act());
    	nbt.putString("title", title);
    	nbt.putString("subtitle", subtitle);
    	if(label!=null)
    		nbt.putString("label", label);
    	nbt.put("scene", scene.save());
    	if(getStatus().doPersist) {
    		nbt.putInt("status", getStatus().ordinal());
    	}else {
    		nbt.putInt("status", RunStatus.RUNNING.ordinal());
    	}
    	return nbt;
    }
    public void load(CompoundTag nbt) {
    	savedLocation=CodecUtil.decodeNBTIfPresent(ParagraphData.CODEC, nbt, "location");
    	callStack.clear();
    	callStack.addAll(CodecUtil.decodeNBTIfPresent(ExecuteStackElement.LIST_CODEC, nbt, "callStack"));
    	name=new ActNamespace(nbt.getString("chapter"),nbt.getString("act"));
    	title=nbt.getString("title");
    	if(nbt.contains("label"))
    		label=nbt.getString("label");
    	else
    		label=null;
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
		this.status = status;
	}
    public String getTitle() {
		return title;
	}
	public String getSubtitle() {
		return subtitle;
	}
	public void sendTitles(ScenarioContext ctx,boolean updateT,boolean updateSt) {
		this.scene.sendTitles(ctx, updateT?title:null, updateSt?subtitle:null);
		//FHNetwork.send(PacketDistributor.PLAYER.with(()->parent.getPlayer()), new ServerSenarioActPacket(updateT?title:null,updateSt?subtitle:null));
	}
	public void setTitles(ScenarioContext ctx,String t,String st) {
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
		sendTitles(ctx,b1,b2);
	}
	public void setTitle(ScenarioContext ctx,String title) {
		if(title!=null&&!this.title.equals(title)) {
			this.title=title;
			sendTitles(ctx,true,false);
		}
	}
	public void setSubtitle(ScenarioContext ctx,String subtitle) {
		if(subtitle!=null&&!this.subtitle.equals(subtitle)) {
			this.subtitle=subtitle;
			sendTitles(ctx,false,true);
		}
	}
	@Override
	public LinkedList<ExecuteStackElement> getCallStack() {
		return this.callStack;
	}
}
