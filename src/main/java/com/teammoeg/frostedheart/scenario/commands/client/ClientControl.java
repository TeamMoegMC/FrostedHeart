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

package com.teammoeg.frostedheart.scenario.commands.client;

import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.scenario.Param;
import com.teammoeg.frostedheart.scenario.client.ClientScene;
import com.teammoeg.frostedheart.scenario.runner.ScenarioConductor;

import dev.ftb.mods.ftbquests.FTBQuests;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestFile;
import dev.ftb.mods.ftbquests.quest.task.ItemTask;
import dev.ftb.mods.ftbquests.quest.task.KillTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.event.ClickEvent;

public class ClientControl {
	public void link(ScenarioConductor runner,@Param("lid")String linkId) {
		ClientScene.preset=Style.EMPTY.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"fh$scenario$link:"+linkId)).setUnderlined(true);
	}
	public void endlink(ScenarioConductor runner) {
		ClientScene.preset=null;
	}
	public void showTask(ScenarioConductor runner,@Param("q")String q,@Param("t")int t) {
		QuestFile qf=FTBQuests.PROXY.getQuestFile(false);
		Quest quest=qf.getQuest(QuestFile.parseCodeString(q));
		Task tsk=quest.tasks.get(t);
		if(tsk instanceof ItemTask) {
			
			ClientScene.processClient(GuiUtils.translateMessage("item_task",tsk.getTitle()), true, true);
		}else if(tsk instanceof KillTask) {
			ClientScene.processClient(GuiUtils.translateMessage("kill_task",tsk.getTitle()), true, true);
		}else {
			ClientScene.processClient(GuiUtils.translateMessage("other_task",tsk.getTitle()), true, true);
		}
	}
}
