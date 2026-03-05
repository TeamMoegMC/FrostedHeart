package com.teammoeg.chorda.client.cui.editor;

public interface EditItemChangeListener<T> {
	void call(EditItem<T> item,T oldVal,T newVal);
}
