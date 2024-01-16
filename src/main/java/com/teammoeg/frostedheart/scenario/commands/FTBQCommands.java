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

package com.teammoeg.frostedheart.scenario.commands;

import com.teammoeg.frostedheart.scenario.FHScenario;
import com.teammoeg.frostedheart.scenario.Param;
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
			return quest.isCompletedRaw(ServerQuestFile.INSTANCE.getData(r.getPlayer()));
		}));
	}
	public void waittaskComplete(ScenarioConductor runner,@Param("s")String s,@Param("l")String l,@Param("q")String q,@Param("t")int t) {
		QuestFile qf=FTBQuests.PROXY.getQuestFile(false);
		Quest quest=qf.getQuest(QuestFile.parseCodeString(q));
		Task tsk=quest.tasks.get(t);
		runner.addTrigger(new SingleExecuteTargerTrigger(s,l,r->{
			return tsk.isCompletedRaw(ServerQuestFile.INSTANCE.getData(r.getPlayer()));
		}));
	}
	public void waittaskCompleteShow(ScenarioConductor runner,@Param("s")String s,@Param("l")String l,@Param("q")String q,@Param("t")int t) {
		QuestFile qf=FTBQuests.PROXY.getQuestFile(false);
		Quest quest=qf.getQuest(QuestFile.parseCodeString(q));
		Task tsk=quest.tasks.get(t);
		FHScenario.callClientCommand("showTask", runner, "q",q,"t",""+t);
		runner.addTrigger(new SingleExecuteTargerTrigger(s,l,r->{
			boolean rx=tsk.isCompletedRaw(ServerQuestFile.INSTANCE.getData(r.getPlayer()));
			//System.out.println(rx);
			return rx;
		}));
		
	}
	public void waitquestStart(ScenarioConductor runner,@Param("s")String s,@Param("l")String l,@Param("q")String q) {
		QuestFile qf=FTBQuests.PROXY.getQuestFile(false);
		Quest quest=qf.getQuest(QuestFile.parseCodeString(q));
		runner.addTrigger(new SingleExecuteTargerTrigger(s,l,r->{
			return qf.getData(r.getPlayer()).canStartTasks(quest);
		}));
	}
	public void completequest(ScenarioConductor runner,@Param("s")String s,@Param("l")String l,@Param("q")String q) {
		Quest quest=ServerQuestFile.INSTANCE.getQuest(QuestFile.parseCodeString(q));
		TeamData td=ServerQuestFile.INSTANCE.getData(runner.getPlayer());
		ProgressChange change=new ProgressChange(ServerQuestFile.INSTANCE);
		change.origin = quest;
		change.reset = false;
		ServerQuestFile.INSTANCE.forceProgress(td,change);

	}
	public void completetask(ScenarioConductor runner,@Param("q")String q,@Param("t")int t) {
		Quest quest=ServerQuestFile.INSTANCE.getQuest(QuestFile.parseCodeString(q));
		TeamData td=ServerQuestFile.INSTANCE.getData(runner.getPlayer());
		ProgressChange change=new ProgressChange(ServerQuestFile.INSTANCE);
		change.origin = quest.tasks.get(t);
		change.reset = false;
		ServerQuestFile.INSTANCE.forceProgress(td,change);

	}
}
