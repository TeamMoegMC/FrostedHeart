package com.teammoeg.frostedheart.scenario.commands;

import com.teammoeg.frostedheart.scenario.FHScenario;
import com.teammoeg.frostedheart.scenario.Param;
import com.teammoeg.frostedheart.scenario.client.ClientTextProcessor;
import com.teammoeg.frostedheart.scenario.runner.ScenarioConductor;
import com.teammoeg.frostedheart.scenario.runner.target.SingleExecuteTargerTrigger;

import dev.ftb.mods.ftbquests.FTBQuests;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestFile;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.util.ProgressChange;

public class FTBQCommands {
	public void waitquestComplete(ScenarioConductor runner,@Param("s")String s,@Param("l")String l,@Param("q")String q) {
		QuestFile qf=FTBQuests.PROXY.getQuestFile(false);
		Quest quest=qf.getQuest(QuestFile.parseCodeString(q));
		runner.addTrigger(new SingleExecuteTargerTrigger(s,l,r->{
			return quest.isCompletedRaw(ServerQuestFile.INSTANCE.getData(r.player));
		}));
	}
	public void waittaskComplete(ScenarioConductor runner,@Param("s")String s,@Param("l")String l,@Param("q")String q,@Param("t")int t) {
		QuestFile qf=FTBQuests.PROXY.getQuestFile(false);
		Quest quest=qf.getQuest(QuestFile.parseCodeString(q));
		Task tsk=quest.tasks.get(t);
		runner.addTrigger(new SingleExecuteTargerTrigger(s,l,r->{
			return tsk.isCompletedRaw(ServerQuestFile.INSTANCE.getData(r.player));
		}));
	}
	public void waittaskCompleteShow(ScenarioConductor runner,@Param("s")String s,@Param("l")String l,@Param("q")String q,@Param("t")int t) {
		QuestFile qf=FTBQuests.PROXY.getQuestFile(false);
		Quest quest=qf.getQuest(QuestFile.parseCodeString(q));
		Task tsk=quest.tasks.get(t);
		FHScenario.callClientCommand("showTask", runner, "q",q,"t",""+t);
		runner.addTrigger(new SingleExecuteTargerTrigger(s,l,r->{
			return tsk.isCompletedRaw(ServerQuestFile.INSTANCE.getData(r.player));
		}));
	}
	public void waitquestStart(ScenarioConductor runner,@Param("s")String s,@Param("l")String l,@Param("q")String q) {
		QuestFile qf=FTBQuests.PROXY.getQuestFile(false);
		Quest quest=qf.getQuest(QuestFile.parseCodeString(q));
		runner.addTrigger(new SingleExecuteTargerTrigger(s,l,r->{
			return qf.getData(r.player).canStartTasks(quest);
		}));
	}
	public void completequest(ScenarioConductor runner,@Param("s")String s,@Param("l")String l,@Param("q")String q) {
		Quest quest=ServerQuestFile.INSTANCE.getQuest(QuestFile.parseCodeString(q));
		TeamData td=ServerQuestFile.INSTANCE.getData(runner.player);
		ProgressChange change=new ProgressChange(ServerQuestFile.INSTANCE);
		change.origin = quest;
		change.reset = false;
		ServerQuestFile.INSTANCE.forceProgress(td,change);

	}
	public void completetask(ScenarioConductor runner,@Param("q")String q,@Param("t")int t) {
		Quest quest=ServerQuestFile.INSTANCE.getQuest(QuestFile.parseCodeString(q));
		TeamData td=ServerQuestFile.INSTANCE.getData(runner.player);
		ProgressChange change=new ProgressChange(ServerQuestFile.INSTANCE);
		change.origin = quest.tasks.get(t);
		change.reset = false;
		ServerQuestFile.INSTANCE.forceProgress(td,change);

	}
}
