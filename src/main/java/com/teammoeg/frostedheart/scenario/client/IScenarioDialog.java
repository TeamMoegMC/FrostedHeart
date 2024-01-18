package com.teammoeg.frostedheart.scenario.client;

import java.util.List;

import com.teammoeg.frostedheart.scenario.client.ClientScene.TextInfo;

public interface IScenarioDialog {
	void updateTextLines(List<TextInfo> queue) ;
	int getDialogWidth();
}
