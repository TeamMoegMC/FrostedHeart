package com.teammoeg.frostedheart.content.scenario.runner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.teammoeg.frostedheart.content.scenario.runner.target.ExecuteTarget;
import com.teammoeg.frostedheart.content.scenario.runner.target.IScenarioTarget;
import com.teammoeg.frostedheart.content.scenario.runner.target.TriggerTarget;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraftforge.common.util.Constants;

public abstract class Scene {

	private final transient Map<String, ExecuteTarget> links = new HashMap<>();
	private transient StringBuilder currentLiteral;
	public transient boolean isNowait;
	private boolean isSaveNowait;
	private transient boolean isSlient;
	private transient int waiting;
	LinkedList<StringBuilder> log = new LinkedList<>();
	private transient List<TriggerTarget> triggers = new ArrayList<>();
	List<String> savedLog = new ArrayList<>();
	private transient boolean requireClear;
	public boolean isClick = true;

	public Scene() {
		super();
	}

	public CompoundNBT save() {
		CompoundNBT nbt = new CompoundNBT();
		nbt.putBoolean("nowait", isSaveNowait);
		ListNBT logs = new ListNBT();
		for (String s : savedLog) {
			logs.add(StringNBT.valueOf(s));
		}
		nbt.put("logs", logs);
		return nbt;
	}

	public void load(CompoundNBT nbt) {
		isSaveNowait = nbt.getBoolean("nowait");
		ListNBT logs = nbt.getList("logs", Constants.NBT.TAG_STRING);
		savedLog.clear();
		log.clear();
		for (INBT s : logs) {
			savedLog.add(s.getAsString());
			log.add(new StringBuilder(s.getAsString()));
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

	public void clear(IScenarioThread parent) {
		if (requireClear)
			forcedClear(parent);
	}

	public void paragraph() {
		isSaveNowait = isNowait;
		savedLog.clear();
		if(!log.isEmpty()&&log.peekLast().length()==0)
			log.pollLast();
		for (StringBuilder sb : log)
			savedLog.add(sb.toString());
	
		log.clear();
		triggers.clear();
	}

	public void forcedClear(IScenarioThread parent) {
		sendClear(parent);
		requireClear = false;
		clearLink();
	}

	public boolean shouldWaitClient() {
		return (currentLiteral != null && currentLiteral.length() != 0) && !isNowait && !isSlient;
	}

	public void appendLiteral(String text) {
		requireClear=true;
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
	public void sendClear(IScenarioThread parent) {
		String tosend="";
		if (currentLiteral != null) {
			tosend=currentLiteral.toString();
			addLogLn(tosend);
		}
		if (!isSlient())
			sendScene(parent,tosend, false, true);
		currentLiteral = null;
	}

	/**
	 * Send all current message and start a new line after that
	 * Also sync current state, so call this after all status operation
	 * */
	public void sendNewLine(IScenarioThread parent) {
		String tosend="";
		if (currentLiteral != null) {
			tosend=currentLiteral.toString();
			addLogLn(tosend);
		}
		if (!isSlient())
			sendScene(parent,tosend, true, false);
		currentLiteral = null;
	}

	/**
	 * Send all current message
	 * Also sync current state, so call this after all status operation
	 * */
	public void sendCurrent(IScenarioThread parent) {
		if (currentLiteral != null) {
			addLog(currentLiteral.toString());
			if (!isSlient())
				sendScene(parent,currentLiteral.toString(), false, false);
		}
	
		currentLiteral = null;
	}

	public boolean isSlient() {
		return isSlient;
	}

	public void setSlient(boolean isSlient) {
		this.isSlient = isSlient;
	}

	public void waitClientIfNeeded(IScenarioThread parent) {
		if (shouldWaitClient() && !isSlient)
			parent.setStatus(RunStatus.WAITCLIENT);
	}

	public void waitClient(IScenarioThread parent, boolean isClick) {
		if (!isSlient) {
			parent.setStatus(RunStatus.WAITCLIENT);
			this.isClick=isClick;
		}
	}

	public void addWait(IScenarioThread parent, int time) {
		waiting += time;
		parent.setStatus(RunStatus.WAITTIMER);
	}

	public boolean tickWait() {
		if (waiting > 0) {
			waiting--;

            return waiting <= 0;
		}
		return false;
	}

	public void tickTriggers(IScenarioThread parent, boolean isCurrentAct) {
		TriggerTarget acttrigger = null;
		for (TriggerTarget t : triggers) {
			if (t.test(parent)) {
				if (t.use()) {
					if (isCurrentAct) {
						acttrigger = t;
						break;
					}
					if(t.isAsync())
						parent.queue(t);
					else
						parent.jump(t);
				}
			}
		}
		triggers.removeIf(t -> !t.canUse());
		if (acttrigger != null) {
			parent.jump(acttrigger);
		}
	}

	public void clearLink() {
		getLinks().clear();
	}

	public void markChatboxDirty() {
		requireClear=true;
	}

	public void addTrigger(IScenarioTrigger trig, IScenarioTarget targ) {
		triggers.add(new TriggerTarget(trig,targ));
	}

	public void stopWait(IScenarioThread parent) {
		if(parent.getStatus()==RunStatus.WAITTIMER) {
			waiting=0;
			parent.setStatus(RunStatus.RUNNING);
		}
	}

	public Map<String, ExecuteTarget> getLinks() {
		return links;
	}

	public abstract void sendTitles(IScenarioThread parent, String title, String subtitle);

	protected abstract void sendScene(IScenarioThread parent, String text, boolean wrap, boolean reset);
}