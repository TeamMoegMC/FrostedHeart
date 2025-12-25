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

package com.teammoeg.frostedheart.content.scenario.parser.providers;

import java.util.Map;

import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.task.CheckmarkTask;
import dev.ftb.mods.ftbquests.quest.task.Task;

public class FTBQProvider extends StringScenarioProvider {

	public FTBQProvider() {

	}

	@Override
	public String get(String t,Map<String,String> param) {
		if(!t.startsWith("quest:"))
			return null;
		String qid=t.substring("quest:".length());
		//System.out.println("loading quest "+qid);
		Quest quest=ServerQuestFile.INSTANCE.getQuest(BaseQuestFile.parseCodeString(qid));
		if(quest==null)return null;
		StringBuilder b=new StringBuilder();
		b.append("[WaitQuestStart q=").append(qid).append(" l=qstart][wt][label name=qstart]\n");
		//b.append("@act c=").append(quest.getChapter().getCodeString()).append(" a=").append(quest.getCodeString()).append("\n");
		/*b.append("@ActTitle t=\"").append(quest.title.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\"")).append("\"");
		if(quest.subtitle!=null&&!quest.subtitle.isEmpty()) {
			b.append(" st=\"").append(quest.subtitle.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\"")).append("\"");
		}*/
		//b.append("\n[p]\n");
		for(String s:quest.getRawDescription()) {
			b.append(s.replaceAll("\\[", "[").replaceAll("@", "@")).append("[p]\n");
		}
		int it=0;
		for(Task tsk:quest.getTasks()) {
			if(tsk instanceof CheckmarkTask) {
				b.append("@actTitle st=\"").append(tsk.getRawTitle()==null||tsk.getRawTitle().isEmpty()?"{message.frostedheart.complete_title}":tsk.getRawTitle()).append("\"\n");
				b.append("[link l=tsk").append(it).append("]").append(tsk.getRawTitle()==null||tsk.getRawTitle().isEmpty()?"{message.frostedheart.click_complete}":tsk.getRawTitle()).append("[endlink]")
				.append("[wa]\n");
			}else {
				b.append("@WaitTaskCompleteShow l=tsk").append(it).append(" q=").append(quest.getCodeString())
				.append(" t=").append(it).append("\n").append("@wt\n");
			}
			b.append("@label name=tsk").append(it).append("\n");
			if(tsk instanceof CheckmarkTask)
				b.append("@CompleteTask q=").append(quest.getCodeString()).append(" t=").append(it).append("\n");
			b.append("@p\n");
			b.append("@actTitle st=\"\"\n");
			it++;
		}
		if(param.containsKey("call"))
			b.append("@Return\n");
		//b.append("@EndAct\n");
		// System.out.println(b.toString());
		return b.toString();
	}

	@Override
	public String getName() {
		return "FTB-Quests";
	}

}
