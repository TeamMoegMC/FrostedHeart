package com.teammoeg.chorda.client.cui.editor;

import java.util.Optional;

import com.mojang.serialization.DataResult;
import com.teammoeg.chorda.client.cui.UIWidget;

public interface EditItem<T> {
	DataResult<Optional<T>> getValue();
	UIWidget getWidget();
	void setValue(T val);
	default void onSave() {};
	default void onCreated() {};
}
