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

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import com.teammoeg.frostedheart.scenario.parser.Scenario;
import com.teammoeg.frostedheart.scenario.runner.target.ActTarget;
import com.teammoeg.frostedheart.scenario.runner.target.ExecuteStackElement;
import com.teammoeg.frostedheart.scenario.runner.target.IScenarioTarget;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.common.util.Constants;

/**
 * An act is a basic unit of execution code
 * 
 * */
public class Act {
	ParagraphData paragraph=new ParagraphData();
	LinkedList<ExecuteStackElement> callStack=new LinkedList<>();
	ActNamespace name;
	transient Scenario sp;//current scenario
	transient int nodeNum=0;//Program register
	private RunStatus status=RunStatus.STOPPED;
    private final Scene scene;
    public String title="";
    public String subtitle="";
    private final ScenarioConductor parent;
    public Act(ScenarioConductor paraData,ActNamespace name) {
		super();
		this.scene=new Scene(this);
		parent=paraData;
		this.name=name;
	}
    public Act(ScenarioConductor paraData,CompoundNBT data) {
		super();
		this.scene=new Scene(this);
		parent=paraData;
		load(data);
	}
    public CompoundNBT save() {
    	CompoundNBT nbt=new CompoundNBT();
    	nbt.putString("pname", paragraph.getName());
    	nbt.putInt("pn", paragraph.getParagraphNum());
    	ListNBT css=new ListNBT();
    	for(ExecuteStackElement cs:callStack) {
    		css.add(cs.save());
    	}
    	nbt.put("callStack", css);
    	nbt.putString("chapter", name.chapter);
    	nbt.putString("act", name.act);
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
    public void load(CompoundNBT nbt) {
    	paragraph=new ParagraphData(nbt.getString("pname"),nbt.getInt("pn"));
    	ListNBT css=nbt.getList("callStack", Constants.NBT.TAG_COMPOUND);
    	for(INBT n:css) {
    		callStack.add(new ExecuteStackElement((CompoundNBT) n));
    	}
    	name=new ActNamespace(nbt.getString("chapter"),nbt.getString("quest"));
    	title=nbt.getString("title");
    	subtitle=nbt.getString("subtitle");
    	scene.load(nbt.getCompound("scene"));
    	setStatus(RunStatus.values()[nbt.getInt("status")]);
    	
    }
	public void newParagraph(Scenario sp,int pn) {
		paragraph.setParagraphNum(pn);
		paragraph.setScenario(sp);
		getScene().waitClient();
		getScene().clear();
    }
	public Scene getScene() {
		return scene;
	}
    public ExecuteStackElement getCurrentPosition() {
    	return new ExecuteStackElement(sp,nodeNum);
    }
	public void addCallStack() {
		callStack.add(getCurrentPosition());
	}
	public void jump(IScenarioTarget target) {
		parent.jump(new ActTarget(name,target));
	}
	public void queue(IScenarioTarget target) {
		parent.queue(new ActTarget(name,target));
	}
	public ServerPlayerEntity getPlayer() {
		return parent.getPlayer();
	}
	/*public IScenarioTarget getExecutionPoint(){
		return new QuestExecuteTarget(paragraph.getScenario(),paragraph.getParagraphNum(),currentQuest.asImmutable());
	}*/
	public RunStatus getStatus() {
		return status;
	}
	public void setStatus(RunStatus status) {
		this.status = status;
	}
}
