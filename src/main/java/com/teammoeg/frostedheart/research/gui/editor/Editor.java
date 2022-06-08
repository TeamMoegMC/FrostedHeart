package com.teammoeg.frostedheart.research.gui.editor;

import java.util.function.Consumer;

import dev.ftb.mods.ftblibrary.ui.Widget;

@FunctionalInterface
public interface Editor<T> {
	void open(Widget w,String lab,T val,Consumer<T> onCommit);
}
