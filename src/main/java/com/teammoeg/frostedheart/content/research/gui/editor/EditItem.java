package com.teammoeg.frostedheart.content.research.gui.editor;

import com.teammoeg.chorda.client.cui.UIWidget;

public interface EditItem<T> {
	T getValue();
	UIWidget getWidget();
}
