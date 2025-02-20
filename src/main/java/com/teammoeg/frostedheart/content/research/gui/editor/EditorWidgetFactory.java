package com.teammoeg.frostedheart.content.research.gui.editor;

import java.util.function.Function;

import org.apache.commons.lang3.function.TriFunction;

import com.teammoeg.chorda.client.cui.Layer;
import com.teammoeg.chorda.client.cui.UIWidget;
import com.teammoeg.chorda.lang.Components;

import net.minecraft.network.chat.Component;

public interface EditorWidgetFactory<T,W extends UIWidget> {
	interface WidgetConstructor<T,W extends UIWidget>{
		W create(Layer parent,Component prompt,T origin);
	}
	W create(Layer parent,Component prompt,T origin);
	T getValue(W widget);
	default EditorItemFactory<T> withName(String prompt){
		return withName(Components.str(prompt));
	}
	default EditorItemFactory<T> withName(Component prompt){
		return new EditorItemFactory<T>() {
			@Override
			public EditItem<T> create(Layer layer,T originValue) {
				return new EditItem<>() {
					W widget=EditorWidgetFactory.this.create(layer,prompt,originValue);
					@Override
					public T getValue() {
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
			public W create(Layer parent, Component prompt, X origin) {
				return objthis.create(parent, prompt, to.apply(origin));
			}

			@Override
			public X getValue(W widget) {
				return from.apply(objthis.getValue(widget));
			}
		};
	}
	public static <T,W extends UIWidget> EditorWidgetFactory<T,W> create(WidgetConstructor<T,W> constr,Function<W,T> func){
		return new EditorWidgetFactory<>() {
			@Override
			public W create(Layer parent, Component prompt, T origin) {
				return constr.create(parent, prompt, origin);
			}

			@Override
			public T getValue(W widget) {
				return func.apply(widget);
			}
		};
		
	}
}
