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

import java.util.function.Function;

import com.teammoeg.chorda.client.cui.editor.EditorFieldsDialog.EditorDialogPrototype;
import com.teammoeg.chorda.util.struct.CurryApplicativeTemplate;
import com.teammoeg.chorda.util.struct.CurryApplicativeTemplate.Applicatable;
import com.teammoeg.chorda.util.struct.CurryApplicativeTemplate.Applicative0;
import com.teammoeg.chorda.util.struct.CurryApplicativeTemplate.BuildResult;

/**
 * 编辑器对话框构建器，使用柯里化应用函子模式（Curry Applicative）声明式构建多字段编辑对话框。
 * 将多个字段的getter和编辑器工厂组合成一个完整的编辑器对话框。
 * <p>
 * Editor dialog builder using the Curry Applicative pattern to declaratively build
 * multi-field edit dialogs. Combines multiple field getters and editor factories
 * into a complete editor dialog.
 */
public class EditorDialogBuilder {

	private EditorDialogBuilder() {

	}
	public static record SetterAndGetter<O, A>(EditorItemFactory<A> factory, Function<O, A> func) implements Applicatable<SetterAndGetter<O, ?>, A> {

	}

	/**
	 * 创建一个多字段编辑器。
	 * <p>
	 * Creates a multi-field editor.
	 *
	 * @param builder 构建器函数 / the builder function
	 * @param <O>     被编辑对象的类型 / the type of object being edited
	 * @return 编辑器实例 / the editor instance
	 */
	public static <O> Editor<O> create(Function<Applicative0<SetterAndGetter<O, ?>>, BuildResult<SetterAndGetter<O, ?>, O>> builder) {
		EditorDialogPrototype<O> proto=new EditorDialogPrototype<>(CurryApplicativeTemplate.build(builder));
		return (p,l,v,c)->proto.create(p, l, v, c).open();
	}
	/**
	 * 创建一个编辑器对话框原型，可重复使用。
	 * <p>
	 * Creates a reusable editor dialog prototype.
	 *
	 * @param builder 构建器函数 / the builder function
	 * @param <O>     被编辑对象的类型 / the type of object being edited
	 * @return 编辑器对话框原型 / the editor dialog prototype
	 */
	public static <O> EditorDialogPrototype<O> createPrototype(Function<Applicative0<SetterAndGetter<O, ?>>, BuildResult<SetterAndGetter<O, ?>, O>> builder) {
		return new EditorDialogPrototype<>(CurryApplicativeTemplate.build(builder));
	}
	public static <O> Applicative0<SetterAndGetter<O, ?>> startBuilder(){
		return Applicative0.getInstance();
	}
}
