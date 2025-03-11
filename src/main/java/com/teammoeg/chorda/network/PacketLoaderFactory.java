package com.teammoeg.chorda.network;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import net.minecraft.network.FriendlyByteBuf;

import static org.objectweb.asm.Opcodes.*;

public class PacketLoaderFactory {
	private static final String READ_ACCESSOR_DESC = Type.getInternalName(MessageReader.class);
	private static final String READ_ACCESSOR_READ_METHOD_DESC = Type.getMethodDescriptor(Type.getType(CMessage.class), Type.getType(FriendlyByteBuf.class));
	private static final ASMClassLoader LOADER = new ASMClassLoader();

	public <T extends CMessage> MessageReader<T> create(Constructor<T> method) throws InvocationTargetException {
		Class<?> cls = createWrapper(method);
		try {
			return (MessageReader) cls.getDeclaredConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException e) {
			throw new InvocationTargetException(e);
		}
	}

	/*public static void main(String[] args)
		throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException {
		// System.out.println(Type.getDescriptor(FriendlyByteBuf.class));
		PacketLoaderFactory fact = new PacketLoaderFactory();
		Constructor ctor = C2SOpenNutritionScreenMessage.class.getDeclaredConstructor(FriendlyByteBuf.class);
		ctor.setAccessible(true);
		MessageReader mr = fact.create(ctor);
		FriendlyByteBuf fake = new FriendlyByteBuf(ByteBufAllocator.DEFAULT.buffer());
		long start = System.nanoTime();
		for (int i = 0; i < 10000000; i++) {
			mr.read(fake);
		}
		long end = System.nanoTime();
		System.out.println((end - start) / 1000 / 1000.0);
		start = System.nanoTime();
		for (int i = 0; i < 10000000; i++) {
			ctor.newInstance(fake);
		}
		end = System.nanoTime();
		System.out.println((end - start) / 1000 / 1000.0);
	}*/

	String getUniqueName(Constructor callback) {
		return String.format("%s.__%s",
			callback.getDeclaringClass().getPackageName(),
			callback.getDeclaringClass().getSimpleName()).replace('.', '/');
	}

	protected Class<?> createWrapper(Constructor callback) {
		var node = new ClassNode();
		transformNode(getUniqueName(callback), callback, node);
		return defineClass(node);
	}

	private static final Class<?> defineClass(ClassNode node) {
		var cw = new ClassWriter(0);
		node.accept(cw);
		return LOADER.define(node.name.replace('/', '.'), cw.toByteArray());
	}

	public static CallSite createCallSite(MethodHandles.Lookup lookup, String name, MethodType type) throws Exception {
		// Derive the constructor signature from the signature of this INVOKEDYNAMIC
		Constructor c = type.returnType().getDeclaredConstructor(type.parameterArray());
		c.setAccessible(true);
		// Convert Constructor to MethodHandle which will serve as a target of
		// INVOKEDYNAMIC
		MethodHandle mh = lookup.unreflectConstructor(c);
		return new ConstantCallSite(mh);
	}

	protected static void transformNode(String name, Constructor callback, ClassNode target) {
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
			mv = target.visitMethod(ACC_PUBLIC, "read", READ_ACCESSOR_READ_METHOD_DESC, null, null);
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 1);
			mv.visitInvokeDynamicInsn("invoke", "(" + Type.getDescriptor(FriendlyByteBuf.class) + ")" + Type.getDescriptor(callback.getDeclaringClass()),
				new Handle(H_INVOKESTATIC, Type.getInternalName(PacketLoaderFactory.class), "createCallSite",
					"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;", false));
			mv.visitInsn(ARETURN);
			mv.visitMaxs(1, 2);
			mv.visitEnd();
		}
		target.visitEnd();
	}

	private static class ASMClassLoader extends ClassLoader {
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
}