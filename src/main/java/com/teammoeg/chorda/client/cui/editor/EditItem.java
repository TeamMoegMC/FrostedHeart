package com.teammoeg.chorda.client.cui.editor;

import com.mojang.serialization.DataResult;
import com.teammoeg.chorda.client.cui.UIWidget;

public interface EditItem<T> {
	DataResult<T> getValue();
	UIWidget getWidget();
	default void onSave() {};
	default void onCreated() {};
}
