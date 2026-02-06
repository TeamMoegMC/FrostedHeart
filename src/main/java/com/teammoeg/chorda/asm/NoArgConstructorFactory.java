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

import static org.objectweb.asm.Opcodes.*;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import com.teammoeg.chorda.Chorda;

public class NoArgConstructorFactory extends AbstractConstructorFactory {
	private static final String READ_ACCESSOR_DESC = Type.getInternalName(Supplier.class);
	private static final String READ_ACCESSOR_READ_METHOD_DESC= Type.getMethodDescriptor(Type.getType(Object.class));

	public <AT> Supplier<AT> create(Class<AT> clazz) throws InvocationTargetException, NoSuchMethodException {
		try {// check if corresponding constructor exists
			clazz.getDeclaredConstructor();
		} catch (SecurityException e) {
			throw new InvocationTargetException(e);
		} catch (NoSuchMethodException e) {
			throw e;
		}
		Class<?> cls = createWrapper(clazz);
		try {
			Supplier<AT>supm=(Supplier) cls.getDeclaredConstructor().newInstance();
			return supm;
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException e) {
			throw new InvocationTargetException(e);
		}
	}

	public NoArgConstructorFactory() {
		super();
	}

	@Override
	protected void transformNode(String name, Class<?> retType, ClassNode target) {
		Chorda.LOGGER.debug("Making supplier of class "+name);
		MethodVisitor mv;
		target.visit(V16, ACC_PUBLIC | ACC_SUPER, name, null, "java/lang/Object", new String[] { READ_ACCESSOR_DESC });

		target.visitSource(".dynamic", null);
		{// define constructor of no-arg
			mv = target.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
			mv.visitInsn(RETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}
		{// define read method
			mv = target.visitMethod(ACC_PUBLIC, "get", READ_ACCESSOR_READ_METHOD_DESC, null, null);
			mv.visitCode();
			mv.visitInvokeDynamicInsn("invoke", "()" + Type.getDescriptor(retType), createCallSiteHandler());
			mv.visitInsn(ARETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}
		target.visitEnd();
	}
}