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

import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.cui.editor.EditorDialogBuilder.SetterAndGetter;

/**
 * 编辑器项工厂接口，负责在EditorFieldsDialog中创建EditItem实例。
 * 提供将工厂与getter函数绑定的方法，用于EditorDialogBuilder。
 * <p>
 * Editor item factory interface responsible for creating EditItem instances
 * within EditorFieldsDialog. Provides methods to bind the factory with getter
 * functions for use in EditorDialogBuilder.
 *
 * @param <T> 被编辑的值类型 / The type of value being edited
 */
public interface EditorItemFactory<T> {
	/**
	 * 创建一个编辑项实例。
	 * <p>
	 * Creates an edit item instance.
	 *
	 * @param l      父层容器 / the parent layer container
	 * @param dialog 所属编辑器对话框 / the owning editor dialog
	 * @param val    初始值 / the initial value
	 * @return 编辑项实例 / the edit item instance
	 */
	EditItem<T> create(UILayer l,EditorFieldsDialog dialog,T val);
	
	/**
	 * 将工厂与getter函数绑定，用于EditorDialogBuilder。
	 * <p>
	 * Binds the factory with a getter function for use in EditorDialogBuilder.
	 *
	 * @param getter 获取对象字段值的函数 / the function to get the field value from an object
	 * @param <O>    对象类型 / the object type
	 * @return 绑定后的SetterAndGetter / the bound SetterAndGetter
	 */
	default <O> SetterAndGetter<O,T> forGetter(Function<O,T> getter){
		return new SetterAndGetter<>(this,getter);
	}
	/**
	 * 创建一个装饰器绑定，getter为恒等函数。
	 * <p>
	 * Creates a decorator binding with an identity getter function.
	 *
	 * @return 装饰器SetterAndGetter / the decorator SetterAndGetter
	 */
	default SetterAndGetter<T,T> decorator(){
		return new SetterAndGetter<>(this,o->o);
	}
}
