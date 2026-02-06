/*
 * Copyright (c) 2026 TeamMoeg
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

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.teammoeg.chorda.client.cui.base.UIElement;

import net.minecraft.network.chat.Component;
/**
 * Fundamental of editor framework
 * */
@FunctionalInterface
public interface Editor<T> {
	/**
	 * calls listener with previous value before opening the editor
	 * */
    default Editor<T> beforeOpen(Consumer<T> listener) {

        return (p, l, v, c) -> {
            listener.accept(v);
            this.open(p, l, v, c);

        };
    }
    /**
     * Opens an editor, either a dialog or something alike, even just something returns value
     * @param parent the widget that user interacts with to trigger the editor, if user is not trigger by widget, user {@link EditUtils#openEditorScreen()} instead.
     * @param label the title of the opening editor
     * @param previousValue the previous value to edit, may be null, editor should make use of this to fill its initial value
     * @param onCommit if user closes the editor and request any save, this would be called to provide new values
     * 
     * */
    void open(UIElement parent, Component label,@Nullable final T previousValue, Consumer<T> onCommit);
    /** Map the editor with convertions */
    default <A> Editor<A> xmap(Function<T,A> to,Function<A,T> from){
    	return (p,l,v,c)->{
    		this.open(p, l, v==null?null:from.apply(v), e->c.accept(to.apply(e)));
    	};
    	
    }
    /**Add a listener when user request save*/
    default Editor<T> addOnChangeAction(BiConsumer<T,T> onChange){
    	return (p,l,v,c)->this.open(p, l, v, cobj->{
    		onChange.accept(v, cobj);
    		c.accept(cobj);
    	});
    }
    /**Add a default value when previous is null*/
    default Editor<T> withDefault(Supplier<T> def){
    	return (p,l,v,c)->this.open(p, l, v==null?def.get():v,c);
    }
    /**Replace previous value with given value*/
    default Editor<T> withValue(Supplier<T> def){
    	return (p,l,v,c)->this.open(p, l,def.get(),c);
    }
}
