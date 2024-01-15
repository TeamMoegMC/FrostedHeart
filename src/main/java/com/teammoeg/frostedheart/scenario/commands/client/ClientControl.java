package com.teammoeg.frostedheart.scenario.commands.client;

import com.teammoeg.frostedheart.scenario.Param;
import com.teammoeg.frostedheart.scenario.client.ClientTextProcessor;
import com.teammoeg.frostedheart.scenario.runner.ScenarioConductor;

import dev.ftb.mods.ftbquests.FTBQuests;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestFile;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.event.ClickEvent;

public class ClientControl {
	public void link(ScenarioConductor runner,@Param("lid")String linkId) {
		ClientTextProcessor.preset=Style.EMPTY.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"fh$scenario$link:"+linkId)).setUnderlined(true);
	}
	public void endlink(ScenarioConductor runner) {
		ClientTextProcessor.preset=null;
	}
	public void showTask(ScenarioConductor runner,@Param("q")String q,@Param("t")int t) {
		QuestFile qf=FTBQuests.PROXY.getQuestFile(false);
		Quest quest=qf.getQuest(QuestFile.parseCodeString(q));
		Task tsk=quest.tasks.get(t);
		ClientTextProcessor.processClient(tsk.getTitle(), true, true);
	}
}
