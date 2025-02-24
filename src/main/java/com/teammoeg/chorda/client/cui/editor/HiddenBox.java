package com.teammoeg.chorda.client.cui.editor;

import java.util.function.Supplier;

import com.teammoeg.chorda.client.cui.UIWidget;

public class HiddenBox<T> extends UIWidget {
	T value;
	public HiddenBox(UIWidget parent,T value,Supplier<T> ifEmpty) {
		super(parent);
		if(value==null)
			this.value=ifEmpty.get();
		else
			this.value=value;
	}
	public T getValue() {
		return value;
	}

}
