package com.teammoeg.frostedheart.content.research.gui.editor;

import com.teammoeg.chorda.client.cui.Layer;

public interface EditorItemFactory<T> {
	EditItem<T> create(Layer l,T originValue);

	
	
}
