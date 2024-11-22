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

package com.teammoeg.frostedheart.content.scenario.commands;

import com.teammoeg.frostedheart.content.scenario.FHScenario;
import com.teammoeg.frostedheart.content.scenario.Param;
import com.teammoeg.frostedheart.content.scenario.runner.ScenarioCommandContext;
import com.teammoeg.frostedheart.content.scenario.runner.target.ExecuteTarget;
import com.teammoeg.frostedheart.content.scenario.runner.target.SingleExecuteTargetTrigger;

import dev.ftb.mods.ftbquests.api.FTBQuestsAPI;
import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.util.ProgressChange;

public class FTBQCommands {
	public void waitquestComplete(ScenarioCommandContext runner,@Param("s")String s,@Param("l")String l,@Param("q")String q) {
		BaseQuestFile qf=FTBQuestsAPI.api().getQuestFile(false);
		Quest quest=qf.getQuest(BaseQuestFile.parseCodeString(q));
		runner.thread().addTrigger(new SingleExecuteTargetTrigger(r-> quest.isCompletedRaw(ServerQuestFile.INSTANCE.getOrCreateTeamData(r.player()))),new ExecuteTarget(s,l));
	}
	public void waittaskComplete(ScenarioCommandContext runner,@Param("s")String s,@Param("l")String l,@Param("q")String q,@Param("t")int t) {
		BaseQuestFile qf=FTBQuestsAPI.api().getQuestFile(false);
		Quest quest=qf.getQuest(BaseQuestFile.parseCodeString(q));
		Task tsk=quest.getTasksAsList().get(t);
		runner.thread().addTrigger(new SingleExecuteTargetTrigger(r-> tsk.isCompletedRaw(ServerQuestFile.INSTANCE.getOrCreateTeamData(r.player()))),new ExecuteTarget(s,l));
	}
	public void waittaskCompleteShow(ScenarioCommandContext runner,@Param("s")String s,@Param("l")String l,@Param("q")String q,@Param("t")int t) {
		//System.out.println("wtcs");
		BaseQuestFile qf=FTBQuestsAPI.api().getQuestFile(false);
		Quest quest=qf.getQuest(BaseQuestFile.parseCodeString(q));
		Task tsk=quest.getTasksAsList().get(t);
		FHScenario.callClientCommand("showTask", runner.context(), "q",q,"t",""+t);
		runner.thread().addTrigger(new SingleExecuteTargetTrigger(r->{
			TeamData td=ServerQuestFile.INSTANCE.getOrCreateTeamData(r.player());
            return td.isCompleted(tsk);
		}),new ExecuteTarget(s,l));
		
	}
	public void waitquestStart(ScenarioCommandContext runner,@Param("s")String s,@Param("l")String l,@Param("q")String q) {
		BaseQuestFile qf=FTBQuestsAPI.api().getQuestFile(false);
		Quest quest=qf.getQuest(BaseQuestFile.parseCodeString(q));
		runner.thread().addTrigger(new SingleExecuteTargetTrigger(r-> qf.getOrCreateTeamData(r.player()).canStartTasks(quest)),new ExecuteTarget(s,l));
	}
	public void completequest(ScenarioCommandContext runner,@Param("q")String q) {
		Quest quest=ServerQuestFile.INSTANCE.getQuest(BaseQuestFile.parseCodeString(q));
		TeamData td=ServerQuestFile.INSTANCE.getOrCreateTeamData(runner.context().player());
		ProgressChange change=new ProgressChange(ServerQuestFile.INSTANCE, quest, runner.context().player().getUUID());
		change.setReset(false);
		quest.forceProgress(td, change);

	}
	public void completetask(ScenarioCommandContext runner,@Param("q")String q,@Param("t")int t) {
		Quest quest=ServerQuestFile.INSTANCE.getQuest(BaseQuestFile.parseCodeString(q));
		Task tsk=quest.getTasksAsList().get(t);
		TeamData td=ServerQuestFile.INSTANCE.getOrCreateTeamData(runner.context().player());
		ProgressChange change=new ProgressChange(ServerQuestFile.INSTANCE,tsk,runner.context().player().getUUID());
		change.setReset(false);
	
		tsk.forceProgress(td, change);
		//ServerQuestFile.INSTANCE.forceProgress(td,change);

	}
}
