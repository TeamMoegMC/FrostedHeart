package com.teammoeg.chorda.client.cui.editor;

/**
 * 编辑项变更监听器接口，当编辑项的值发生变化时被调用。
 * <p>
 * Edit item change listener interface, invoked when an edit item's value changes.
 *
 * @param <T> 被编辑的值类型 / The type of value being edited
 */
public interface EditItemChangeListener<T> {
	void call(EditItem<T> item,T oldVal,T newVal);
}
