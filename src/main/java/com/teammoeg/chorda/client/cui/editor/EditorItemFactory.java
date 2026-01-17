package com.teammoeg.chorda.client.cui.editor;

import java.util.function.Function;

import com.teammoeg.chorda.client.cui.UILayer;
import com.teammoeg.chorda.client.cui.editor.EditorDialogBuilder.SetterAndGetter;

public interface EditorItemFactory<T> {
	EditItem<T> create(UILayer l,EditorDialog dialog,T val);

	default <O> SetterAndGetter<O,T> forGetter(Function<O,T> getter){
		return new SetterAndGetter<>(this,getter);
	}
	default <O> SetterAndGetter<O,T> decorator(){
		return new SetterAndGetter<>(this,o->null);
	}
}
