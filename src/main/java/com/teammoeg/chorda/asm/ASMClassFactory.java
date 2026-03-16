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

package com.teammoeg.chorda.asm;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

/**
 * ASM 动态类生成工厂的基类。使用 ObjectWeb ASM 库在运行时动态生成字节码类。
 * 提供自定义类加载器以加载动态生成的类。
 * <p>
 * Base class for ASM dynamic class generation factories. Uses the ObjectWeb ASM
 * library to dynamically generate bytecode classes at runtime. Provides a custom
 * class loader to load dynamically generated classes.
 */
public class ASMClassFactory {

	private static final ASMClassLoader LOADER = new ASMClassLoader();

	/**
	 * 将 ASM {@link ClassNode} 编译为字节码并加载为 Java 类。
	 * <p>
	 * Compiles an ASM {@link ClassNode} into bytecode and loads it as a Java class.
	 *
	 * @param node 要编译的类节点 / the class node to compile
	 * @return 加载后的 Java 类 / the loaded Java class
	 */
	protected static final Class<?> defineClass(ClassNode node) {
		var cw = new ClassWriter(0);
		node.accept(cw);
		return LOADER.define(node.name.replace('/', '.'), cw.toByteArray());
	}

	/**
	 * 为给定的类生成唯一的 ASM 类名，用于避免类名冲突。
	 * <p>
	 * Generates a unique ASM class name for the given class to avoid name conflicts.
	 *
	 * @param clazz 源类 / the source class
	 * @return 唯一的内部类名（使用 '/' 分隔） / unique internal class name (using '/' separators)
	 */
	protected String getUniqueName(Class<?> clazz) {
		return (clazz.getPackageName()+".__ChordaAsm"+clazz.getSimpleName()+"Accessor").replace('.', '/');
	}

	/**
	 * 自定义类加载器，用于加载 ASM 动态生成的类。
	 * 使用当前线程的上下文类加载器来解析已有的类。
	 * <p>
	 * Custom class loader for loading ASM-generated dynamic classes.
	 * Uses the current thread's context class loader to resolve existing classes.
	 */
	protected static class ASMClassLoader extends ClassLoader {
			private ASMClassLoader() {
				super(null);
			}
	
			@Override
			protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
				return Class.forName(name, resolve, Thread.currentThread().getContextClassLoader());
			}
	
			Class<?> define(String name, byte[] data) {
				return defineClass(name, data, 0, data.length);
			}
		}

	protected ASMClassFactory() {
		super();
	}

}