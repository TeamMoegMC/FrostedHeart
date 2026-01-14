package com.teammoeg.chorda.client.cui.editor;

import java.util.function.Supplier;

import com.teammoeg.chorda.client.cui.UIElement;

public class HiddenBox<T> extends UIElement {
	T value;
	public HiddenBox(UIElement parent,T value,Supplier<T> ifEmpty) {
		super(parent);
		if(value==null)
			this.value=ifEmpty.get();
		else
			this.value=value;
	}
	public T getValue() {
		return value;
	}
	public void setValue(T nval) {
		this.value=nval;
	}

}
