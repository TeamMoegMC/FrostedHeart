package com.teammoeg.frostedheart.scenario.runner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.teammoeg.frostedheart.FHPacketHandler;
import com.teammoeg.frostedheart.scenario.network.ServerSenarioScenePacket;
import com.teammoeg.frostedheart.scenario.runner.target.ActTarget;
import com.teammoeg.frostedheart.scenario.runner.target.ExecuteTarget;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.network.PacketDistributor;

/**
 * A scene is a place to present content to client You should NOT store this
 * object, always get it from {@link ScenarioConductor#getScene()}
 */
public class Scene {
	transient final Map<String, ExecuteTarget> links = new HashMap<>();
	private transient StringBuilder currentLiteral;
	private transient final ScenarioConductor parent;
	public transient boolean isNowait;
	private boolean isSaveNowait;
	private transient boolean isSlient;
	private transient int waiting;
	LinkedList<StringBuilder> log = new LinkedList<>();
	private transient List<IScenarioTrigger> triggers = new ArrayList<>();
	List<String> savedLog = new ArrayList<>();
	private transient Act act;
	private transient boolean requireClear;
	public boolean isClick=true;
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
			savedLog.add(s.getString());
			log.add(new StringBuilder(s.getString()));
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

	public Scene(ScenarioConductor paraData, Act act) {
		super();
		this.parent = paraData;
		this.act = act;
	}

	public void clear() {
		if (requireClear)
			forcedClear();
	}

	public void paragraph() {
		clear();
		isSaveNowait = isNowait;
		savedLog.clear();
		if(!log.isEmpty()&&log.peekLast().length()==0)
			log.pollLast();
		for (StringBuilder sb : log)
			savedLog.add(sb.toString());

		log.clear();
		triggers.clear();
	}

	public void forcedClear() {
		sendClear();
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

	private void sendScene(String text, boolean wrap, boolean reset) {
	
		FHPacketHandler.send(PacketDistributor.PLAYER.with(() -> parent.getPlayer()), new ServerSenarioScenePacket(text, wrap, isNowait, reset,parent.getStatus(),isClick));
		isClick=true;
	}
	/**
	 * sync all remaining cached text and send a 'clear current dialog' message to client
	 * Also sync current state, so call this after all status operation
	 * */
	public void sendClear() {
		String tosend="";
		if (currentLiteral != null) {
			tosend=currentLiteral.toString();
			addLogLn(tosend);
		}
		if (!isSlient())
			sendScene(tosend, false, true);
		currentLiteral = null;
	}
	/**
	 * Send all current message and start a new line after that
	 * Also sync current state, so call this after all status operation
	 * */
	public void sendNewLine() {
		String tosend="";
		if (currentLiteral != null) {
			tosend=currentLiteral.toString();
			addLogLn(tosend);
		}
		if (!isSlient())
			sendScene(tosend, true, false);
		currentLiteral = null;
	}
	/**
	 * Send all current message
	 * Also sync current state, so call this after all status operation
	 * */
	public void sendCurrent() {
		if (currentLiteral != null) {
			addLog(currentLiteral.toString());
			if (!isSlient())
				sendScene(currentLiteral.toString(), false, false);
		}

		currentLiteral = null;
	}

	public boolean isSlient() {
		return isSlient;
	}

	public void setSlient(boolean isSlient) {
		this.isSlient = isSlient;
	}

	public void waitClientIfNeeded() {
		if (shouldWaitClient() && !isSlient)
			parent.setStatus(RunStatus.WAITCLIENT);
	}

	public void waitClient(boolean isClick) {
		if (!isSlient) {
			parent.setStatus(RunStatus.WAITCLIENT);
			this.isClick=isClick;
		}
	}

	public void addWait(int time) {
		waiting += time;
		parent.setStatus(RunStatus.WAITTIMER);
	}

	public boolean tickWait() {
		if (waiting > 0) {
			waiting--;

			if (waiting <= 0)
				return true;
		}
		return false;
	}

	public void tickTriggers(ScenarioConductor runner, boolean isCurrentAct) {
		IScenarioTrigger acttrigger = null;
		for (IScenarioTrigger t : triggers) {
			if (t.test(runner)) {
				if (t.use()) {
					if (isCurrentAct) {
						acttrigger = t;
						break;
					}
					parent.queue(new ActTarget(act.name, t));
				}
			}
		}
		triggers.removeIf(t -> !t.canUse());
		if (acttrigger != null) {
			parent.jump(acttrigger);
		}
	}

	public void clearLink() {
		links.clear();
	}
	public void markChatboxDirty() {
		requireClear=true;
	}
	public void addTrigger(IScenarioTrigger trig) {
		triggers.add(trig);
	}

	public void stopWait() {
		if(parent.getStatus()==RunStatus.WAITTIMER) {
			waiting=0;
			parent.setStatus(RunStatus.RUNNING);
		}
	}

}
