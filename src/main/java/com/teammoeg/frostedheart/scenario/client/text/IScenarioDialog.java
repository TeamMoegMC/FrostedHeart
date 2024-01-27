package com.teammoeg.frostedheart.scenario.client.text;

import java.util.List;

public interface IScenarioDialog {
	void updateTextLines(List<TextInfo> queue) ;
	int getDialogWidth();
	void tickDialog();
}
