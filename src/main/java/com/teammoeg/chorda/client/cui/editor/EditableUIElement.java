package com.teammoeg.chorda.client.cui.editor;

/**
 * 可编辑UI元素接口，支持设置值变更监听器。
 * <p>
 * Editable UI element interface supporting change listener registration.
 *
 * @param <T> 被编辑的值类型 / The type of value being edited
 */
public interface EditableUIElement<T> {
	public void setOnChangeListener(EditItemChangeListener<T> listener);
}
