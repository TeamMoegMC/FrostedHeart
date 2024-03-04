package com.teammoeg.frostedheart.content.scenario.runner;

import java.util.Collection;

import com.teammoeg.frostedheart.content.scenario.parser.Scenario;
import com.teammoeg.frostedheart.content.scenario.runner.target.ExecuteStackElement;
import com.teammoeg.frostedheart.content.scenario.runner.target.IScenarioTarget;

import net.minecraft.entity.player.PlayerEntity;

public interface IScenarioThread {
	void setScenario(Scenario s);
	Scenario getScenario();
	void setNodeNum(int num);
	int getNodeNum();
	String getLang();
	void sendMessage(String s);
	void queue(IScenarioTarget t);
	void jump(IScenarioTarget acttrigger);
	RunStatus getStatus();
	PlayerEntity getPlayer();
	void setStatus(RunStatus waitclient);
	Collection<? extends ExecuteStackElement> getCallStack();
}
