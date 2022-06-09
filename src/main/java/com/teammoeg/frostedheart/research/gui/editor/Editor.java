package com.teammoeg.frostedheart.research.gui.editor;

import java.util.function.Consumer;

import dev.ftb.mods.ftblibrary.ui.Widget;

@FunctionalInterface
public interface Editor<T> {
	void open(Widget w,String lab,T val,Consumer<T> onCommit);
	default Editor<T> and(Editor<T> listener){
		
		return (p,l,v,c)->{listener.open(p, l, v, c);
		this.open(p, l, v, c);};
	};
}
