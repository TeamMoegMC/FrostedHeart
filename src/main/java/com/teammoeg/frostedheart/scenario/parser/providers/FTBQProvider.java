package com.teammoeg.frostedheart.scenario.parser.providers;

import java.util.function.Function;

import com.teammoeg.frostedheart.scenario.FHScenario;
import com.teammoeg.frostedheart.scenario.parser.Scenario;

import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestFile;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.task.CheckmarkTask;
import dev.ftb.mods.ftbquests.quest.task.Task;

public class FTBQProvider implements Function<String, Scenario> {

	public FTBQProvider() {

	}

	@Override
	public Scenario apply(String t) {
		if(!t.startsWith("quest:"))
			return null;
		String qid=t.substring("quest:".length());
		Quest quest=ServerQuestFile.INSTANCE.getQuest(QuestFile.parseCodeString(qid));
		if(quest==null)return null;
		StringBuilder b=new StringBuilder();
		b.append("@StartAct c=").append(quest.getChapter().getCodeString()).append(" a=").append(quest.getCodeString()).append("\n");
		b.append("@ActTitle t=\"").append(quest.title.replaceAll("\\", "\\\\").replaceAll("\"", "\\\"")).append("\"");
		if(quest.subtitle!=null&&!quest.subtitle.isEmpty()) {
			b.append(" st=\"").append(quest.subtitle.replaceAll("\\", "\\\\").replaceAll("\"", "\\\"")).append("\"");
		}
		b.append("\n@p\n");
		for(String s:quest.description) {
			b.append(s.replaceAll("\\[", "\\[").replaceAll("@", "\\@")).append("@p\n");
		}
		int it=0;
		for(Task tsk:quest.tasks) {
			if(tsk instanceof CheckmarkTask) {
				b.append("[link l=tsk").append(it).append("]").append(tsk.title==null?"Click here":tsk.title).append("[endlink]")
				.append("[wa]\n");
			}else {
				b.append("@WaitTaskCompleteShow l=tsk").append(it).append(" q=").append(quest.getCodeString())
				.append(" t=").append(it).append("\n").append("@wt\n");
			}
			b.append("@label name=tsk").append(it).append("\n");
			if(tsk instanceof CheckmarkTask)
				b.append("@CompleteTask q=").append(quest.getCodeString()).append(" t=").append(it).append("\n");
			b.append("@p\n");
			b.append("@EndAct\n");
			it++;
		}
		return FHScenario.parser.parseString(t,b.toString());
	}

}
