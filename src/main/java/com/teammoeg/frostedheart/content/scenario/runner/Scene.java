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
public abstract class Scene {

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

	public Scene() {
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
	public void notifyClientResponse(ScenarioContext ctx,int clientStatus) {
		if(clearAfterClick) {
			this.clear(ctx);
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

	public void clear(ScenarioContext parent) {
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
	}

	public void forcedClear(ScenarioContext parent) {
		sendClear(parent,false);
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
	public void sendClear(ScenarioContext ctx,boolean waitClick) {
		String tosend="";
		if (currentLiteral != null) {
			tosend=currentLiteral.toString();
			addLogLn(tosend);
		}
		if (!isSlient())
			sendScene(ctx,tosend,RunStatus.STOPPED, false, true,waitClick);
		currentLiteral = null;
	}

	/**
	 * Send all current message and start a new line after that
	 * Also sync current state, so call this after all status operation
	 * */
	public void sendNewLine(ScenarioContext ctx,RunStatus status,boolean noAutoDelay) {
		String tosend="";
		if (currentLiteral != null) {
			tosend=currentLiteral.toString();
			addLogLn(tosend);
		}
		if (!isSlient())
			sendScene(ctx,tosend,status, true, false,noAutoDelay);
		currentLiteral = null;
	}
	/**
	 * Send all current message
	 * Also sync current state, so call this after all status operation
	 * */
	public void sendCurrent(ScenarioContext ctx,RunStatus status,boolean noAutoDelay) {
		if (currentLiteral != null) {
			addLog(currentLiteral.toString());
			if (!isSlient())
				sendScene(ctx,currentLiteral.toString(),status, false, false,noAutoDelay);
		}
	
		currentLiteral = null;
	}
	public void sendCached(ScenarioContext ctx) {
		sendCurrent(ctx,RunStatus.RUNNING,false);
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

	public abstract void sendTitles(ScenarioContext ctx, String title, String subtitle);

	protected abstract void sendScene(ScenarioContext ctx,String text,RunStatus status, boolean wrap, boolean reset,boolean noAutoDelay);
}