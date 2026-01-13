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
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.teammoeg.chorda.client.cui.UIElement;
import com.teammoeg.chorda.lang.Components;

import net.minecraft.network.chat.Component;

public class EditorSelector<T> extends BaseEditDialog {
	private static record EditorDefinition<T>(Component label, Editor<T> editor, Predicate<T> isSuitable) {
	}

	public static class EditorSelectorBuilder<T> {
		private final List<EditorDefinition<T>> editors = new ArrayList<>();
		private final Predicate<T> isNull;
		
		public EditorSelectorBuilder(Predicate<T> isEmpty) {
			isNull = isEmpty;
		}

		public EditorSelectorBuilder() {
			isNull = t -> t == null;
		}

		public EditorSelectorBuilder<T> addEditor(String name, Editor<? extends T> e) {
			return addEditor(Components.str(name), e);
		}

		public EditorSelectorBuilder<T> addEditor(Component name, Editor<? extends T> e) {
			editors.add(new EditorDefinition<T>(name, (Editor)e, o -> true));
			return this;
		}
		public EditorSelectorBuilder<T> addEditorWhenEmpty(String name, Editor<? extends T> e) {
			return addEditorWhenEmpty(Components.str(name), e);
		}

		public EditorSelectorBuilder<T> addEditorWhenEmpty(Component name, Editor<? extends T> e) {
			editors.add(new EditorDefinition<T>(name, (Editor)e, isNull));
			return this;
		}
		public EditorSelectorBuilder<T> addEditorWhenNotEmpty(String name, Editor<? extends T> e, Predicate<T> isSuitable) {
			return addEditorWhenNotEmpty(Components.str(name), e, isNull.negate().and(isSuitable));
		}

		public EditorSelectorBuilder<T> addEditorWhenNotEmpty(Component name, Editor<? extends T> e, Predicate<T> isSuitable) {
			editors.add(new EditorDefinition<T>(name, (Editor)e, isNull.negate().and(isSuitable)));
			return this;
		}
		public EditorSelectorBuilder<T> addEditorWhenNotEmpty(String name, Editor<? extends T> e) {
			return addEditorWhenNotEmpty(Components.str(name), e);
		}

		public EditorSelectorBuilder<T> addEditorWhenNotEmpty(Component name, Editor<? extends T> e) {
			editors.add(new EditorDefinition<T>(name, (Editor)e, isNull.negate()));
			return this;
		}
		public EditorSelectorBuilder<T> addEditor(String name, Editor<? extends T> e, Predicate<T> isSuitable) {
			return addEditor(Components.str(name), e, isNull.or(isSuitable));
		}

		public EditorSelectorBuilder<T> addEditor(Component name, Editor<? extends T> e, Predicate<T> isSuitable) {
			editors.add(new EditorDefinition<T>(name, (Editor)e, isNull.or(isSuitable)));
			return this;
		}
		public Editor<T> buildEdit(){
			return (p,l,v,c)->{
				List<EditorDefinition<T>> suitbleEditors=new ArrayList<>();
	    		for(EditorDefinition<T> i:editors) {
	    			if(i.isSuitable.test(v)) {
	    				suitbleEditors.add(i);
	    			}
	    		}
	    		if(suitbleEditors.size()==1) {
	    			EditorDefinition<T> le=suitbleEditors.get(0);
	    			le.editor.open(p, le.label(), v, c);
	    			
	    		}else new EditorSelector<T>(p,suitbleEditors,l,v,c).open();
			};
		}
		public Editor<T> build(){
			Editor<T> orig=buildEdit();
			return new EditorSelectorBuilder<T>(isNull)
			.addEditorWhenNotEmpty(Components.translatable("gui.chorda.editor.edit"),orig)
            .addEditor(Components.translatable("gui.chorda.editor.new"), orig.withValue(()->null)).buildEdit();

	    }
	}

	private final Component label;
	private final T val;
	private final Consumer<T> callback;
	private final List<EditorDefinition<T>> type;
	
	private EditorSelector(UIElement panel, List<EditorDefinition<T>> type, Component label, T val, Consumer<T> callback) {
		super(panel);
		this.label = label;
		this.val = val;
		this.callback = callback;
		this.type = type;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void addUIElements() {
		add(EditUtils.getTitle(this, label));
		for (EditorDefinition<T> ent : type) {
			add(new OpenEditorButton<T>(this, ent.label(), (Editor<T>) ent.editor.beforeOpen(v-> close(false)), val, callback));
		}
	}

	@Override
	public void onClose() {
	}

}
