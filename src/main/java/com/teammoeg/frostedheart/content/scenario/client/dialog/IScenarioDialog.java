package com.teammoeg.frostedheart.content.scenario.client.dialog;

import java.util.List;

import com.teammoeg.frostedheart.content.scenario.client.gui.layered.LayerManager;

public interface IScenarioDialog {
	void updateTextLines(List<TextInfo> queue) ;
	int getDialogWidth();
	void tickDialog();
	LayerManager getPrimary();
	void setPrimary(LayerManager primary);
	void closeDialog();
	boolean hasDialog();
}
