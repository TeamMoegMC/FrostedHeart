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
import java.util.function.Function;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
/**
 * 单参数构造函数工厂。通过 ASM 动态生成实现 {@link Function} 接口的类，
 * 以高性能方式调用目标类的单参数构造函数（比传统反射快 10-100 倍）。
 * <p>
 * One-argument constructor factory. Dynamically generates classes implementing
 * {@link Function} via ASM to invoke a target class's one-arg constructor
 * with high performance (10-100x faster than traditional reflection).
 *
 * @param <T> 构造函数参数类型 / the constructor parameter type
 * @param <R> 构造函数返回类型 / the constructor return type
 */
public class OneArgConstructorFactory<T, R> extends AbstractConstructorFactory {
	private final Class<T> clazz;
	private final Type inType;
	private static final String READ_ACCESSOR_DESC = Type.getInternalName(Function.class);
	private static final String READ_ACCESSOR_READ_METHOD_DESC = Type.getMethodDescriptor(Type.getType(Object.class), Type.getType(Object.class));

	/**
	 * 为指定类创建一个 {@link Function}，该 Function 通过 invokedynamic 调用其单参数构造函数。
	 * <p>
	 * Creates a {@link Function} for the specified class that invokes its one-arg
	 * constructor via invokedynamic.
	 *
	 * @param clazz 目标类 / the target class
	 * @param <AT>  目标类型（必须是 R 的子类型） / the target type (must extend R)
	 * @return 调用单参数构造函数的 Function / a Function that invokes the one-arg constructor
	 * @throws InvocationTargetException 如果构造函数不可访问 / if the constructor is inaccessible
	 * @throws NoSuchMethodException     如果匹配的构造函数不存在 / if no matching constructor exists
	 */
	public <AT extends R> Function<T, AT> create(Class<AT> clazz) throws InvocationTargetException, NoSuchMethodException {
		try {// check if corresponding constructor exists
			clazz.getDeclaredConstructor(this.clazz);
		} catch (SecurityException e) {
			throw new InvocationTargetException(e);
		} catch (NoSuchMethodException e) {
			throw e;
		}
		Class<?> cls = createWrapper(clazz);
		
		try {
			return (Function) cls.getDeclaredConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException e) {
			throw new InvocationTargetException(e);
		}
	}

	/**
	 * 创建一个单参数构造函数工厂。
	 * <p>
	 * Creates a one-argument constructor factory.
	 *
	 * @param inType 构造函数参数的类型 / the type of the constructor parameter
	 */
	public OneArgConstructorFactory(Class<T> inType) {
		super();
		this.clazz = inType;
		this.inType = Type.getType(this.clazz);
	}


	/**
	 * 生成实现 {@link Function} 接口的类的字节码。
	 * 生成的 {@code apply()} 方法通过 invokedynamic 调用目标类的单参数构造函数。
	 * <p>
	 * Generates bytecode for a class implementing the {@link Function} interface.
	 * The generated {@code apply()} method invokes the target class's one-arg constructor
	 * via invokedynamic.
	 */
	@Override
	protected void transformNode(String name, Class<?> retType, ClassNode target) {
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
			mv = target.visitMethod(ACC_PUBLIC, "apply", READ_ACCESSOR_READ_METHOD_DESC, null, null);
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 1);
			mv.visitTypeInsn(CHECKCAST, inType.getInternalName());
			mv.visitInvokeDynamicInsn("invoke", "(" + inType.getDescriptor() + ")" + Type.getDescriptor(retType), createCallSiteHandler());
			mv.visitInsn(ARETURN);
			mv.visitMaxs(1, 2);
			mv.visitEnd();
		}
		target.visitEnd();
	}
}