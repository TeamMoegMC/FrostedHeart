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

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

/**
 * 构造函数工厂的抽象基类。通过 ASM 动态生成字节码来创建高性能的构造函数访问器，
 * 使用 {@code invokedynamic} 指令避免传统反射的性能开销。
 * <p>
 * Abstract base class for constructor factories. Dynamically generates bytecode
 * via ASM to create high-performance constructor accessors, using the
 * {@code invokedynamic} instruction to avoid the performance overhead of traditional reflection.
 */
public abstract class AbstractConstructorFactory extends ASMClassFactory {

	/**
	 * {@code invokedynamic} 的引导方法。根据调用点的方法类型推导出目标构造函数，
	 * 并返回一个 {@link ConstantCallSite} 以缓存方法句柄。
	 * <p>
	 * Bootstrap method for {@code invokedynamic}. Derives the target constructor
	 * from the call site's method type and returns a {@link ConstantCallSite}
	 * to cache the method handle.
	 *
	 * @param lookup 调用者的查找上下文 / the caller's lookup context
	 * @param name   调用点名称（未使用） / the call site name (unused)
	 * @param type   调用点的方法类型 / the call site's method type
	 * @return 绑定到目标构造函数的调用点 / a call site bound to the target constructor
	 * @throws Exception 如果构造函数不可访问 / if the constructor is inaccessible
	 */
	public static CallSite createCallSite(MethodHandles.Lookup lookup, String name, MethodType type) throws Exception {
		// Derive the constructor signature from the signature of this INVOKEDYNAMIC
		Constructor c = type.returnType().getDeclaredConstructor(type.parameterArray());
		c.setAccessible(true);
		// Convert Constructor to MethodHandle which will serve as a target of
		// INVOKEDYNAMIC
		MethodHandle mh = lookup.unreflectConstructor(c);
		return new ConstantCallSite(mh);
	}

	/**
	 * 将 ASM 字节码变换应用到类节点上，由子类实现以生成特定的包装器类。
	 * <p>
	 * Applies ASM bytecode transformations to a class node. Implemented by subclasses
	 * to generate specific wrapper classes.
	 *
	 * @param name    生成类的内部名称 / the internal name of the generated class
	 * @param retType 构造函数的返回类型 / the return type of the constructor
	 * @param target  要变换的类节点 / the class node to transform
	 */
	protected abstract void transformNode(String name, Class<?> retType, ClassNode target);

	/**
	 * 为给定类创建一个包装器类，该包装器可高效调用其构造函数。
	 * <p>
	 * Creates a wrapper class for the given class that can efficiently invoke its constructor.
	 *
	 * @param clazz 需要包装的目标类 / the target class to wrap
	 * @return 生成的包装器类 / the generated wrapper class
	 */
	protected Class<?> createWrapper(Class<?> clazz) {
		var node = new ClassNode();
		transformNode(getUniqueName(clazz), clazz, node);
		return defineClass(node);
	}
	/**
	 * 创建 {@code invokedynamic} 指令使用的引导方法句柄。
	 * <p>
	 * Creates the bootstrap method handle used by the {@code invokedynamic} instruction.
	 *
	 * @return 指向 {@link #createCallSite} 的 ASM 方法句柄 / an ASM Handle pointing to {@link #createCallSite}
	 */
	protected Handle createCallSiteHandler() {
		return new Handle(H_INVOKESTATIC, Type.getInternalName(AbstractConstructorFactory.class), "createCallSite",
			"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;", false);
	}
	protected AbstractConstructorFactory() {
		super();
	}

}