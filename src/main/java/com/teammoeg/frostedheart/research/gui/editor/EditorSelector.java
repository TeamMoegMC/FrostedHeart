package com.teammoeg.frostedheart.research.gui.editor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import dev.ftb.mods.ftblibrary.ui.Widget;

public class EditorSelector<T> extends BaseEditDialog {
	private final Map<String,Editor<? extends T>> editors=new LinkedHashMap<>();
	private final String label;
	private final T val;
	private final BiPredicate<T,String> getEditor;
	private final Consumer<T> callback;


	public EditorSelector(Widget panel, String label,BiPredicate<T,String> pred, T val, Consumer<T> callback) {
		super(panel);
		this.label = label;
		this.val = val;
		this.getEditor=pred;
		this.callback = callback;
	}
	public EditorSelector(Widget panel, String label, Consumer<T> callback) {
		this(panel,label,(o,s)->false,null,callback);
	}
	public EditorSelector<T> addEditor(String name,Editor<? extends T> e){
		editors.put(name, e);
		return this;
	}
	@Override
	public void onClose() {
	}

	@SuppressWarnings("unchecked")
	@Override
	public void addWidgets() {
		add(EditUtils.getTitle(this, label));
		for(Entry<String, Editor<? extends T>> ent:editors.entrySet()) {
			add(new OpenEditorButton<>(this,ent.getKey(),(Editor<T>)ent.getValue().and((p,l,v,c)->close(false)),(val!=null&&getEditor.test(val,ent.getKey())?val:null),e->callback.accept(e)));
		}
	}

}
