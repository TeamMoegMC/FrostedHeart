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

import com.teammoeg.frostedheart.client.util.ClientUtils;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.scenario.Param;
import com.teammoeg.frostedheart.scenario.client.ClientScene;
import com.teammoeg.frostedheart.scenario.client.IClientScene;
import com.teammoeg.frostedheart.scenario.runner.ScenarioConductor;

import dev.ftb.mods.ftblibrary.util.ClientTextComponentUtils;
import dev.ftb.mods.ftbquests.FTBQuests;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestFile;
import dev.ftb.mods.ftbquests.quest.task.ItemTask;
import dev.ftb.mods.ftbquests.quest.task.KillTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.event.ClickEvent;

public class ClientControl implements IClientControlCommand {
	public void link(IClientScene runner,@Param("lid")String linkId) {
		runner.setPreset(Style.EMPTY.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"fh$scenario$link:"+linkId)).setUnderlined(true));
	}
	public void endlink(IClientScene runner) {
		runner.setPreset(null);
	}
	@Override
	public void showTask(IClientScene runner,@Param("q")String q,@Param("t")int t) {
		QuestFile qf=FTBQuests.PROXY.getQuestFile(false);
		Quest quest=qf.getQuest(QuestFile.parseCodeString(q));
		Task tsk=quest.tasks.get(t);
		if(tsk instanceof ItemTask) {
			
			runner.processClient(GuiUtils.translateMessage("item_task",tsk.getTitle()), true, true);
		}else if(tsk instanceof KillTask) {
			runner.processClient(GuiUtils.translateMessage("kill_task",tsk.getTitle()), true, true);
		}else {
			runner.processClient(GuiUtils.translateMessage("other_task",tsk.getTitle()), true, true);
		}
	}
	@Override
	public void showTitle(IClientScene runner,@Param("t")String t,@Param("st")String st,@Param("in")Integer i1,@Param("show")Integer i2,@Param("out")Integer i3) {
		ITextComponent t1=null,t2=null;
		if(t!=null)
			t1=ClientTextComponentUtils.parse(t);
		if(st!=null)
			t2=ClientTextComponentUtils.parse(st);
		ClientUtils.mc().ingameGUI.renderTitles(t1,t2,i1==null?-1:i1,i2==null?-1:i2, i3==null?-1:i3);
	}
}
