package com.teammoeg.chorda.client.cui.editor;

public interface EditableUIElement<T> {
	public void setOnChangeListener(EditItemChangeListener<T> listener);
}
