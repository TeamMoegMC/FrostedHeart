package com.teammoeg.chorda.client.cui.editor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import com.mojang.datafixers.util.Pair;
import com.teammoeg.chorda.client.cui.Layer;
import com.teammoeg.chorda.client.cui.LayerScrollBar;
import com.teammoeg.chorda.client.cui.TextField;
import com.teammoeg.chorda.client.cui.UIWidget;
import com.teammoeg.chorda.client.cui.editor.EditorDialogBuilder.SetterAndGetter;
import com.teammoeg.chorda.util.struct.CurryApplicativeTemplate.BuildResult;
import com.teammoeg.chorda.util.struct.CurryApplicativeTemplate.Item;
import com.teammoeg.chorda.util.struct.CurryApplicativeTemplate.BuiltParams;

import net.minecraft.network.chat.Component;

public class EditorDialog<O> extends BaseEditDialog {
	Layer mainPane;
	LayerScrollBar scroll;
	public static record EditorPair<O,A>(EditorItemFactory<A> factory,Function<O,A> getter,int index) {
		public Pair<Integer, EditItem<?>> create(EditorDialog<O> dialog,Layer parent,O o,Object[] params){
			A val=null;
			if(o!=null)
				val=getter.apply(o);
			if(index>=0)
				params[index]=val;
			return Pair.of(index, factory.create(parent,dialog,val));
		}
	}
	public static class EditorDialogPrototype<O>{
		List<EditorPair<O,?>> widgets;
		Function<BuiltParams, O> consumer;
		int paramSize=0;
		public EditorDialogPrototype(BuildResult<SetterAndGetter<O, ?>,O> dialog) {
			this.widgets=new ArrayList<>();
			Arrays.stream(dialog.obj()).map(t->new EditorPair((EditorItemFactory<?>)t.obj().factory(),(Function)t.obj().func(),t.index())).forEach(widgets::add);
			this.paramSize=dialog.parcount();
			this.consumer=dialog.consumer();
		}
		public EditorDialog<O> create(UIWidget panel,Component title,O origin,Consumer<O> consumer){
			return new EditorDialog<>(panel,title,this,origin,li->consumer.accept(this.consumer.apply(li)));
		}
		
	}

	public static class EditorResult implements BuiltParams{
		Object[] par;
		
		public EditorResult(Object[] par) {
			super();
			this.par = par;
		}
		@Override
		public Object getRaw(int params) {
			return par[params];
		}

	}
	List<Pair<Integer,EditItem<?>>> values=new ArrayList<>();
	Consumer<EditorResult> consumer;
	Object[] params;
	TextField title;
	private EditorDialog(UIWidget panel,Component title,EditorDialogPrototype<O> prototype,O origin,Consumer<EditorResult> consumer) {
		super(panel);
		this.title=EditUtils.getTitle(this, title);
		this.params=new Object[prototype.widgets.size()];
		this.consumer=consumer;
        mainPane=new Layer(this) {

			@Override
			public void addUIElements() {
				for(Pair<Integer, EditItem<?>> i:values) {
					UIWidget widget=i.getSecond().getWidget();
					if(widget!=null)
						this.add(widget);
				}
			}

			@Override
			public void alignWidgets() {
				this.align(false);
			}
        	
        };
        scroll=new LayerScrollBar(this, true, mainPane);
		for(EditorPair<O, ?> i:prototype.widgets) {
			values.add(i.create(this,mainPane,origin,params));
		}
	
     
        setWidth(400);
        setHeight(250);
        this.title.setPos(2, 2);
		mainPane.setPosAndSize(5, 12, width - 16, height - 14);
        scroll.setPosAndSize(width - 16, 12, 16, height - 14);
	}

	boolean noSave=false;
	public void setNoSave() {
		noSave=true;
	}
	
	@Override
	public void onClose() {
		if(!noSave) {
			for(Pair<Integer, EditItem<?>> i:values) {
				if(i.getFirst()>=0) {
					i.getSecond().getValue().result().ifPresent(t->params[i.getFirst()]=t.orElse(null));
				}
			}
			consumer.accept(new EditorResult(params));
		}
	}
	@Override
	public void addUIElements() {

		add(this.title);
		add(mainPane);
    	add(scroll);
		
	}
	@Override
    public void alignWidgets() {
    }
}
