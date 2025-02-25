package com.teammoeg.chorda.client.cui.editor;

import java.util.function.Function;

import com.teammoeg.chorda.client.cui.Layer;
import com.teammoeg.chorda.client.cui.editor.EditorDialogBuilder.SetterAndGetter;

public interface EditorItemFactory<T> {
	EditItem<T> create(Layer l,EditorDialog dialog,T originValue);

	default <O> SetterAndGetter<O,T> forGetter(Function<O,T> getter){
		return new SetterAndGetter<>(this,getter);
	}
	default <O> SetterAndGetter<O,T> decorator(){
		return new SetterAndGetter<>(this,o->null);
	}
}
