package com.teammoeg.frostedheart.scenario.commands;

import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.scenario.Param;
import com.teammoeg.frostedheart.scenario.client.ClientTextProcessor;
import com.teammoeg.frostedheart.scenario.runner.ScenarioConductor;

import net.minecraft.util.text.Style;
import net.minecraft.util.text.event.ClickEvent;

public class ClientControl {
	public void link(ScenarioConductor runner,@Param("lid")String linkId) {
		ClientTextProcessor.preset=Style.EMPTY.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"fh$scenario$link:"+linkId)).setUnderlined(true);
	}
	public void endlink(ScenarioConductor runner) {
		ClientTextProcessor.preset=null;
	}
}
