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

public class ASMClassFactory {

	private static final ASMClassLoader LOADER = new ASMClassLoader();

	protected static final Class<?> defineClass(ClassNode node) {
		var cw = new ClassWriter(0);
		node.accept(cw);
		return LOADER.define(node.name.replace('/', '.'), cw.toByteArray());
	}

	protected String getUniqueName(Class<?> clazz) {
		return (clazz.getPackageName()+".__ChordaAsm"+clazz.getSimpleName()+"Accessor").replace('.', '/');
	}

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