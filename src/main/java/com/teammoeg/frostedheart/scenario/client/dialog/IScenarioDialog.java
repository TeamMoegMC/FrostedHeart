package com.teammoeg.frostedheart.scenario.client.dialog;

import java.util.List;

import com.teammoeg.frostedheart.scenario.client.gui.layered.LayerManager;

public interface IScenarioDialog {
	void updateTextLines(List<TextInfo> queue) ;
	int getDialogWidth();
	void tickDialog();
	LayerManager getPrimary();
	void setPrimary(LayerManager primary);
	void closeScreen();
	boolean hasDialog();
}
