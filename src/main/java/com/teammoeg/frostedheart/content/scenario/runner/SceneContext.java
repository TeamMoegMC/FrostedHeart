/*
 * Copyright (c) 2026 TeamMoeg
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.teammoeg.frostedheart.content.scenario.runner.target.ExecuteTarget;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
/**
 * A scene object is used to display things on client, acts as a bridge between user interaction and scenario execution.
 * 
 * */
public class SceneContext {

	private final transient Map<String, ExecuteTarget> links = new HashMap<>();
	private transient StringBuilder currentLiteral;
	public transient boolean isNowait;
	private boolean isSaveNowait;
	private transient boolean isSlient;
	private boolean clearAfterClick;
	public  int clientStatus;
	LinkedList<StringBuilder> log = new LinkedList<>();
	List<String> savedLog = new ArrayList<>();
	private transient boolean requireClear;

	public SceneContext() {
		super();
	}

	public CompoundTag save() {
		CompoundTag nbt = new CompoundTag();
		nbt.putBoolean("nowait", isSaveNowait);
		ListTag logs = new ListTag();
		for (String s : savedLog) {
			logs.add(StringTag.valueOf(s));
		}
		nbt.put("logs", logs);
		return nbt;
	}

	public void load(CompoundTag nbt) {
		isSaveNowait = nbt.getBoolean("nowait");
		ListTag logs = nbt.getList("logs", Tag.TAG_STRING);
		savedLog.clear();
		log.clear();
		for (Tag s : logs) {
			savedLog.add(s.getAsString());
			log.add(new StringBuilder(s.getAsString()));
		}
	}
	public void markClearAfterClick() {
		clearAfterClick=true;
	}
	public void notifyClientResponse(ScenarioContext ctx,ScenarioThread thread,int clientStatus) {
		if(clearAfterClick) {
			clearAfterClick=false;
			this.clear(ctx,thread,RunStatus.RUNNING);
		}
	}
	public void addLog(String text) {
		if (!log.isEmpty()) {
			log.peekLast().append(text);
		} else {
			log.add(new StringBuilder(text));
		}
	}

	public void addLogLn(String text) {
		addLog(text);
		log.add(new StringBuilder());
	}

	public void clear(ScenarioContext ctx,ScenarioThread thread,RunStatus status) {
		//System.out.println("clear:"+requireClear);
		if (requireClear)
			forcedClear(ctx,thread,status);
	}

	public void rollLogs() {

		savedLog.clear();
		if(!log.isEmpty()&&log.peekLast().length()==0)
			log.pollLast();
		for (StringBuilder sb : log)
			savedLog.add(sb.toString());
	
		log.clear();
	}

	public void forcedClear(ScenarioContext ctx,ScenarioThread thread,RunStatus status) {
		sendClear(ctx,thread,status,false);
		requireClear = false;
		clearLink();
	}
    
    /**
     * Creates the link.
     *
     * @param id the id
     * @param scenario the scenario
     * @param label the label
     * @return the string
     */
    public String createLink(String id,String scenario,String label) {
		if(id==null||getLinks().containsKey(id)) {
			id=UUID.randomUUID().toString();
		}
		getLinks().put(id, new ExecuteTarget(scenario,label));
		markChatboxDirty();
		return id;
	}
	public boolean shouldWaitClient() {
		return (currentLiteral != null && currentLiteral.length() != 0) && !isNowait && !isSlient;
	}

	public void appendLiteral(String text) {
		markChatboxDirty();
		if (!text.isEmpty()) {
			if (currentLiteral == null)
				currentLiteral = new StringBuilder();
			currentLiteral.append(text);
		}
	}

	/**
	 * sync all remaining cached text and send a 'clear current dialog' message to client
	 * Also sync current state, so call this after all status operation
	 * */
	public void sendClear(ScenarioContext ctx,ScenarioThread thread,RunStatus status,boolean waitClick) {
		String tosend="";
		if (currentLiteral != null) {
			tosend=currentLiteral.toString();
			addLogLn(tosend);
		}
		if (!isSlient())
			ctx.getScene().sendScene(thread,tosend,status, false, true,isNowait,waitClick);
		currentLiteral = null;
	}

	/**
	 * Send all current message and start a new line after that
	 * Also sync current state, so call this after all status operation
	 * */
	public void sendNewLine(ScenarioContext ctx,ScenarioThread thread,RunStatus status,boolean noAutoDelay) {
		String tosend="";
		if (currentLiteral != null) {
			tosend=currentLiteral.toString();
			addLogLn(tosend);
		}
		if (!isSlient())
			ctx.getScene().sendScene(thread,tosend,status, true, false,isNowait,noAutoDelay);
		currentLiteral = null;
	}
	/**
	 * Send all current message
	 * Also sync current state, so call this after all status operation
	 * */
	public void sendCurrent(ScenarioContext ctx,ScenarioThread thread,RunStatus status,boolean noAutoDelay) {
		if (currentLiteral != null) {
			addLog(currentLiteral.toString());
			if (!isSlient())
				ctx.getScene().sendScene(thread,currentLiteral.toString(),status, false, false,isNowait,noAutoDelay);
		}
	
		currentLiteral = null;
	}
	public void sendCached(ScenarioContext ctx,ScenarioThread thread) {
		sendCurrent(ctx,thread,RunStatus.RUNNING,false);
	}
	public boolean isSlient() {
		return isSlient;
	}

	public void setSlient(boolean isSlient) {
		this.isSlient = isSlient;
	}
	public void clearLink() {
		getLinks().clear();
	}

	public void markChatboxDirty() {
		requireClear=true;
	}



	public Map<String, ExecuteTarget> getLinks() {
		return links;
	}

}