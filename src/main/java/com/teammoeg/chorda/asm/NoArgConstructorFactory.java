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

/**
 * 无参构造函数工厂。通过 ASM 动态生成实现 {@link Supplier} 接口的类，
 * 以高性能方式调用目标类的无参构造函数（比传统反射快 10-100 倍）。
 * <p>
 * No-argument constructor factory. Dynamically generates classes implementing
 * {@link Supplier} via ASM to invoke a target class's no-arg constructor
 * with high performance (10-100x faster than traditional reflection).
 */
public class NoArgConstructorFactory extends AbstractConstructorFactory {
	private static final String READ_ACCESSOR_DESC = Type.getInternalName(Supplier.class);
	private static final String READ_ACCESSOR_READ_METHOD_DESC= Type.getMethodDescriptor(Type.getType(Object.class));

	/**
	 * 为指定类创建一个 {@link Supplier}，该 Supplier 每次调用时通过 invokedynamic 调用其无参构造函数。
	 * <p>
	 * Creates a {@link Supplier} for the specified class that invokes its no-arg
	 * constructor via invokedynamic on each call.
	 *
	 * @param clazz 目标类 / the target class
	 * @param <AT>  目标类型 / the target type
	 * @return 调用无参构造函数的 Supplier / a Supplier that invokes the no-arg constructor
	 * @throws InvocationTargetException 如果构造函数不可访问 / if the constructor is inaccessible
	 * @throws NoSuchMethodException     如果无参构造函数不存在 / if no no-arg constructor exists
	 */
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

	/**
	 * 生成实现 {@link Supplier} 接口的类的字节码。
	 * 生成的 {@code get()} 方法通过 invokedynamic 调用目标类的无参构造函数。
	 * <p>
	 * Generates bytecode for a class implementing the {@link Supplier} interface.
	 * The generated {@code get()} method invokes the target class's no-arg constructor
	 * via invokedynamic.
	 */
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