/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.chorda.client.cui.editor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import com.mojang.datafixers.util.Pair;
import com.teammoeg.chorda.client.cui.UILayer;
import com.teammoeg.chorda.client.cui.LayerScrollBar;
import com.teammoeg.chorda.client.cui.TextField;
import com.teammoeg.chorda.client.cui.UIElement;
import com.teammoeg.chorda.client.cui.editor.EditorDialogBuilder.SetterAndGetter;
import com.teammoeg.chorda.util.struct.CurryApplicativeTemplate.BuildResult;
import com.teammoeg.chorda.util.struct.CurryApplicativeTemplate.BuiltParams;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.network.chat.Component;

public class EditorDialog<O> extends BaseEditDialog {
	UILayer mainPane;
	LayerScrollBar scroll;
	public static record EditorPair<O,A>(EditorItemFactory<A> factory,Function<O,A> getter,int index) {
		public Pair<Integer, EditItem<Object>> create(EditorDialog<O> dialog,UILayer parent,O o,Object[] params){
			A val=null;
			if(o!=null)
				val=getter.apply(o);
			if(index>=0)
				params[index]=val;
			return Pair.of(index, (EditItem)factory.create(parent,dialog,val));
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
		public EditorDialog<O> create(UIElement panel,Component title,O origin,Consumer<O> consumer){
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
	List<Pair<Integer,EditItem<Object>>> values=new ArrayList<>();
	Int2ObjectOpenHashMap<EditItem<Object>> map=new Int2ObjectOpenHashMap<>();
	Map<UIElement,Integer> widgetNum=new IdentityHashMap<>();
	Consumer<EditorResult> consumer;
	Object[] params;
	TextField title;
	private EditorDialog(UIElement panel,Component title,EditorDialogPrototype<O> prototype,O origin,Consumer<EditorResult> consumer) {
		super(panel);
		this.title=EditUtils.getTitle(this, title);
		this.params=new Object[prototype.widgets.size()];
		
		this.consumer=consumer;
        mainPane=new UILayer(this) {

			@Override
			public void addUIElements() {
				widgetNum.clear();
				for(Pair<Integer, EditItem<Object>> i:values) {
					UIElement widget=i.getSecond().getWidget();
					if(widget!=null) {
						this.add(widget);
						widgetNum.put(widget, i.getFirst());
					}
				}
			}

			@Override
			public void alignWidgets() {
				this.align(false);
			}
        	
        };
        scroll=new LayerScrollBar(this, true, mainPane);
		for(EditorPair<O, ?> i:prototype.widgets) {
			//System.out.println(i.index);
			Pair<Integer, EditItem<Object>> wgt=i.create(this,mainPane,origin,params);
			values.add(wgt);
			if(wgt.getFirst()>=0) {
				map.put((int)wgt.getFirst(), wgt.getSecond());
			}
		}
	
     

        this.title.setPos(2, 2);

	}

	boolean noSave=false;
	public void setNoSave() {
		noSave=true;
	}
	public int getCurrentIndex(UIElement wg) {
		return widgetNum.getOrDefault(wg, -1);
	}
	public <T> T getValue(int idx) {
		return (T) map.get(idx).getValue().result().flatMap(t->t).orElse(null);
	}
	public void setValue(int idx,Object value) {
		map.get(idx).setValue(value);
		params[idx]=value;
	}
	@Override
	public void onClose() {
		if(!noSave) {
			for(Pair<Integer, EditItem<Object>> i:values) {
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
		mainPane.setWidth(mainPane.getContentWidth());
		int curheight=Math.min(200, mainPane.getContentHeight());
		mainPane.setHeight(curheight);
		mainPane.setPos(5, 12);
		this.setSize(mainPane.getWidth()+21, mainPane.getHeight()+14);
        scroll.setPosAndSize(width - 16, 12, 16, mainPane.getHeight());
    }
}
