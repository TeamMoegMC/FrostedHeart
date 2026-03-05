package com.teammoeg.chorda.client.cui.editor;

import java.util.ArrayList;
import java.util.List;

public class EditItemListenerList<T> {
	List<EditItemChangeListener<T>> listners=new ArrayList<>();
	public void call(EditItem<T> item,T old,T newval) {
		listners.forEach(t->t.call(item, old, newval));
	}
	public void add(EditItemChangeListener<T> listener) {
		listners.add(listener);
	}
}
