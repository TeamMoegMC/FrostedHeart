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

package com.teammoeg.frostedheart.content.research.gui.editor;

import java.util.function.Consumer;
import java.util.function.Function;

import com.teammoeg.chorda.client.cui.UIElement;

import dev.ftb.mods.ftblibrary.ui.Widget;

@FunctionalInterface
public interface Editor<T> {
    default Editor<T> and(Editor<T> listener) {

        return (p, l, v, c) -> {
            listener.open(p, l, v, c);
            this.open(p, l, v, c);
        };
    }

    void open(UIElement parent, String label, T previousValue, Consumer<T> onCommit);
    default <A> Editor<A> xmap(Function<T,A> to,Function<A,T> from){
    	return (p,l,v,c)->{
    		this.open(p, l, from.apply(v), e->c.accept(to.apply(e)));
    	};
    	
    };
}
