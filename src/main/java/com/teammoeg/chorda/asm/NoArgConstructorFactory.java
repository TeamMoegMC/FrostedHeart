package com.teammoeg.chorda.asm;

import static org.objectweb.asm.Opcodes.*;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

public class NoArgConstructorFactory<R> extends AbstractConstructorFactory {
	private static final String READ_ACCESSOR_DESC = Type.getInternalName(Supplier.class);
	private final String READ_ACCESSOR_READ_METHOD_DESC;

	public <AT extends R> Supplier<AT> create(Class<AT> clazz) throws InvocationTargetException, NoSuchMethodException {
		try {// check if corresponding constructor exists
			clazz.getConstructor();
		} catch (SecurityException e) {
			throw new InvocationTargetException(e);
		} catch (NoSuchMethodException e) {
			throw e;
		}
		Class<?> cls = createWrapper(clazz);
		try {
			return (Supplier) cls.getDeclaredConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException e) {
			throw new InvocationTargetException(e);
		}
	}

	public NoArgConstructorFactory(Class<R> outType) {
		super();
		READ_ACCESSOR_READ_METHOD_DESC = Type.getMethodDescriptor(Type.getType(outType));
	}

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
			mv.visitInvokeDynamicInsn("invoke", "()" + Type.getDescriptor(retType), createCallSiteHandler());
			mv.visitInsn(ARETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}
		target.visitEnd();
	}
}