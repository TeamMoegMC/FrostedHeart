package com.teammoeg.chorda.client.cui.editor;

import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.lang3.function.TriFunction;

import com.mojang.serialization.DataResult;
import com.teammoeg.chorda.client.cui.Layer;
import com.teammoeg.chorda.client.cui.UIWidget;
import com.teammoeg.chorda.lang.Components;

import net.minecraft.network.chat.Component;

public interface EditorWidgetFactory<T,W extends UIWidget> {
	interface WidgetConstructor<T,W extends UIWidget>{
		W create(Layer parent,Component prompt,T origin);
	}
	interface ActionWidgetConstructor<T,W extends UIWidget>{
		W create(EditorDialog dialog,Layer parent,Component prompt,T origin);
	}
	W create(Layer parent,Component prompt,T origin,EditorDialog dialog);
	DataResult<Optional<T>> getValue(W widget);
	
	default EditorItemFactory<T> withName(String prompt){
		return withName(Components.str(prompt));
	}
	default EditorItemFactory<T> withName(Component prompt){
		return new EditorItemFactory<T>() {
			@Override
			public EditItem<T> create(Layer layer,EditorDialog dialog,T originValue) {
				return new EditItem<>() {
					W widget=EditorWidgetFactory.this.create(layer,prompt,originValue,dialog);
					@Override
					public DataResult<Optional<T>> getValue() {
						return EditorWidgetFactory.this.getValue(widget);
					}
					@Override
					public UIWidget getWidget() {
						return widget;
					}
					
				};
			}
		};
	}
	default <X> EditorWidgetFactory<X,W> xmap(Function<T,X> from,Function<X,T> to){
		EditorWidgetFactory<T,W> objthis=this;
		return new EditorWidgetFactory<>() {
			@Override
			public W create(Layer parent, Component prompt, X origin,EditorDialog dialog) {
				return objthis.create(parent, prompt, origin==null?null:to.apply(origin),dialog);
			}

			@Override
			public DataResult<Optional<X>> getValue(W widget) {
				
				return objthis.getValue(widget).map(n->n.map(from));
			}
		};
	}
	default <X> EditorWidgetFactory<X,W> flatXmap(Function<T,DataResult<X>> from,Function<X,T> to){
		EditorWidgetFactory<T,W> objthis=this;
		return new EditorWidgetFactory<>() {
			@Override
			public W create(Layer parent, Component prompt, X origin,EditorDialog dialog) {
				return objthis.create(parent, prompt, to.apply(origin),dialog);
			}

			@Override
			public DataResult<Optional<X>> getValue(W widget) {
				
				return objthis.getValue(widget).flatMap(n->from.apply(n.orElse(null))).map(Optional::of);
			}
		};
	}
	public static <T,W extends UIWidget> EditorWidgetFactory<T,W> create(WidgetConstructor<T,W> constr,Function<W,T> func){
		return new EditorWidgetFactory<>() {
			@Override
			public W create(Layer parent, Component prompt, T origin,EditorDialog dialog) {
				return constr.create(parent, prompt, origin);
			}

			@Override
			public DataResult<Optional<T>> getValue(W widget) {
				return DataResult.success(Optional.ofNullable(func.apply(widget)));
			}
		};
		
	}
	public static <T,W extends UIWidget> EditorWidgetFactory<T,W> create(ActionWidgetConstructor<T,W> constr){
		return new EditorWidgetFactory<>() {
			@Override
			public W create(Layer parent, Component prompt, T origin,EditorDialog dialog) {
				return constr.create(dialog,parent, prompt, origin);
			}

			@Override
			public DataResult<Optional<T>> getValue(W widget) {
				return DataResult.error(()->"Not an input");
			}
		};
		
	}
}
