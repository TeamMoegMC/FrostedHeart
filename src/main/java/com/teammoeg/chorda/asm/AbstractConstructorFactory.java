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

public abstract class AbstractConstructorFactory extends ASMClassFactory {

	public static CallSite createCallSite(MethodHandles.Lookup lookup, String name, MethodType type) throws Exception {
		// Derive the constructor signature from the signature of this INVOKEDYNAMIC
		Constructor c = type.returnType().getDeclaredConstructor(type.parameterArray());
		c.setAccessible(true);
		// Convert Constructor to MethodHandle which will serve as a target of
		// INVOKEDYNAMIC
		MethodHandle mh = lookup.unreflectConstructor(c);
		return new ConstantCallSite(mh);
	}

	protected abstract void transformNode(String name, Class<?> retType, ClassNode target);

	protected Class<?> createWrapper(Class<?> clazz) {
		var node = new ClassNode();
		transformNode(getUniqueName(clazz), clazz, node);
		return defineClass(node);
	}
	protected Handle createCallSiteHandler() {
		return new Handle(H_INVOKESTATIC, Type.getInternalName(AbstractConstructorFactory.class), "createCallSite",
			"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;", false);
	}
	protected AbstractConstructorFactory() {
		super();
	}

}