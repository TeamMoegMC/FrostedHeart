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

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import com.mojang.serialization.DataResult;
import com.teammoeg.chorda.client.cui.UILayer;
import com.teammoeg.chorda.client.cui.UIElement;
import com.teammoeg.chorda.lang.Components;

import net.minecraft.network.chat.Component;

public interface EditorWidgetFactory<T,W extends UIElement> {
	interface WidgetConstructor<T,W extends UIElement>{
		W create(UILayer parent,Component prompt,T origin);
	}
	interface ActionWidgetConstructor<T,W extends UIElement>{
		W create(EditorDialog dialog,UILayer parent,Component prompt,T origin);
	}
	W create(UILayer parent,Component prompt,T origin,EditorDialog dialog);
	DataResult<Optional<T>> getValue(W widget);
	W setValue(W widget,T value);
	default EditorItemFactory<T> withName(String prompt){
		return withName(Components.str(prompt));
	}
	
	default EditorItemFactory<T> withName(Component prompt){
		return new EditorItemFactory<T>() {
			@Override
			public EditItem<T> create(UILayer layer,EditorDialog dialog,T val) {
				return new EditItem<>() {
					W widget=EditorWidgetFactory.this.create(layer,prompt,val,dialog);
					@Override
					public DataResult<Optional<T>> getValue() {
						return EditorWidgetFactory.this.getValue(widget);
					}
					@Override
					public UIElement getWidget() {
						return widget;
					}
					@Override
					public void setValue(T val) {
						widget=EditorWidgetFactory.this.setValue(widget, val);
					}
					
				};
			}

		};
	}
	default <X> EditorWidgetFactory<X,W> xmap(Function<T,X> from,Function<X,T> to){
		EditorWidgetFactory<T,W> objthis=this;
		return new EditorWidgetFactory<>() {
			@Override
			public W create(UILayer parent, Component prompt, X origin,EditorDialog dialog) {
				return objthis.create(parent, prompt, origin==null?null:to.apply(origin),dialog);
			}

			@Override
			public DataResult<Optional<X>> getValue(W widget) {
				
				return objthis.getValue(widget).map(n->n.map(from));
			}

			@Override
			public W setValue(W widget, X value) {
				return objthis.setValue(widget, to.apply(value));
			}
		};
	}
	default <X> EditorWidgetFactory<X,W> flatXmap(Function<T,DataResult<X>> from,Function<X,T> to){
		EditorWidgetFactory<T,W> objthis=this;
		return new EditorWidgetFactory<>() {
			@Override
			public W create(UILayer parent, Component prompt, X origin,EditorDialog dialog) {
				return objthis.create(parent, prompt, to.apply(origin),dialog);
			}

			@Override
			public DataResult<Optional<X>> getValue(W widget) {
				
				return objthis.getValue(widget).flatMap(n->from.apply(n.orElse(null))).map(Optional::of);
			}

			@Override
			public W setValue(W widget, X value) {
				return objthis.setValue(widget, to.apply(value));
			}
		};
	}
	default EditorWidgetFactory<T,W> withDefault(Supplier<T> def){
		EditorWidgetFactory<T,W> objthis=this;
		return new EditorWidgetFactory<>() {
			@Override
			public W create(UILayer parent, Component prompt, T origin,EditorDialog dialog) {
				return objthis.create(parent, prompt, origin==null?def.get():origin,dialog);
			}

			@Override
			public DataResult<Optional<T>> getValue(W widget) {
				return objthis.getValue(widget);
			}

			@Override
			public W setValue(W widget, T value) {
				return objthis.setValue(widget, value);
			}
		};
		
	}
	public static <T,W extends UIElement> EditorWidgetFactory<T,W> create(WidgetConstructor<T,W> constr,Function<W,T> func,BiFunction<W,T,W> setValue){
		return new EditorWidgetFactory<>() {
			@Override
			public W create(UILayer parent, Component prompt, T origin,EditorDialog dialog) {
				return constr.create(parent, prompt, origin);
			}

			@Override
			public DataResult<Optional<T>> getValue(W widget) {
				return DataResult.success(Optional.ofNullable(func.apply(widget)));
			}

			@Override
			public W setValue(W widget, T value) {
				return setValue.apply(widget, value);
			}
		};
		
	}
	public static <T,W extends UIElement> EditorWidgetFactory<T,W> create(WidgetConstructor<T,W> constr,Function<W,T> func,BiConsumer<W,T> setValue){
		return new EditorWidgetFactory<>() {
			@Override
			public W create(UILayer parent, Component prompt, T origin,EditorDialog dialog) {
				return constr.create(parent, prompt, origin);
			}

			@Override
			public DataResult<Optional<T>> getValue(W widget) {
				return DataResult.success(Optional.ofNullable(func.apply(widget)));
			}

			@Override
			public W setValue(W widget, T value) {
				setValue.accept(widget, value);
				return widget;
			}
		};
		
	}
	public static <T,W extends UIElement> EditorWidgetFactory<T,W> create(ActionWidgetConstructor<T,W> constr){
		return new EditorWidgetFactory<>() {
			@Override
			public W create(UILayer parent, Component prompt, T origin,EditorDialog dialog) {
				return constr.create(dialog,parent, prompt, origin);
			}

			@Override
			public DataResult<Optional<T>> getValue(W widget) {
				return DataResult.error(()->"Not an input");
			}

			@Override
			public W setValue(W widget, T value) {
				return widget;
			}
		};
		
	}
}
