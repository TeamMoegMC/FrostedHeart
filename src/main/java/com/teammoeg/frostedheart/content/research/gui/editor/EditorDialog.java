package com.teammoeg.frostedheart.content.research.gui.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import com.teammoeg.chorda.client.cui.Layer;
import com.teammoeg.chorda.client.cui.LayerScrollBar;
import com.teammoeg.chorda.client.cui.TextField;
import com.teammoeg.chorda.client.cui.UIWidget;
import com.teammoeg.chorda.lang.Components;

import net.minecraft.network.chat.Component;

public class EditorDialog<O> extends BaseEditDialog {
	Layer mainPane;
	LayerScrollBar scroll;
	public static class EditorDialogPrototype<O>{
		List<EditorPair<O,?>> widgets=new ArrayList<>();
		public EditorDialogPrototype() {}
		public EditorDialogPrototype(EditorDialogPrototype<O> dialog) {
			this.widgets.addAll(dialog.widgets);
		}
		public <A> void add(EditorItemFactory<A> factory,Function<O,A> getter) {
			widgets.add(new EditorPair(factory,getter));
		}
		public EditorDialog<O> create(UIWidget panel,Component title,O origin,Consumer<List<?>> consumer){
			return new EditorDialog(panel,title,widgets,origin,consumer);
		}
		
	}
	public static record EditorPair<O,A>(EditorItemFactory<A> factory,Function<O,A> getter) {
		public EditItem<A> create(Layer parent,O o){
			return factory.create(parent, getter.apply(o));
		}
	}
	List<EditItem<?>> values=new ArrayList<>();
	Consumer<List<?>> consumer;
	TextField title;
	private EditorDialog(UIWidget panel,Component title,List<EditorPair<O,?>> widgets,O origin,Consumer<List<?>> consumer) {
		super(panel);
		this.title=EditUtils.getTitle(this, title);
		for(EditorPair<O, ?> i:widgets) {
			values.add(i.create(mainPane,origin));
		}
		this.consumer=consumer;
        mainPane=new Layer(this) {

			@Override
			public void addUIElements() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void alignWidgets() {
				// TODO Auto-generated method stub
				
			}
        	
        };
        scroll=new LayerScrollBar(this, true, mainPane);
        setWidth(400);
        setHeight(250);
	}
	@Override
	public void refresh() {
		recalcContentSize();
		add(mainPane);
    	add(scroll);
		clearElement();
		mainPane.refresh();
		mainPane.setPosAndSize(5, 20, width - 16, height - 25);
		mainPane.alignWidgets();
        scroll.setPosAndSize(width - 16, 20, 16, height - 25);
	}
	@Override
	public void onClose() {
		consumer.accept(values.stream().map(t->t.getValue()).toList());
	}
	@Override
	public void addUIElements() {
		add(this.title);
		for(EditItem<?> i:values) {
			this.add(i.getWidget());
		}
		
	}
}
