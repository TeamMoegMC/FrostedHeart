package com.teammoeg.chorda.asm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.function.Function;

/**
 * 成员访问器工厂。
 * 以高性能方式反射获取目标类的成员。
 *
 */
public final class AccessorFactory {

    private static final HashMap<String, Function<?, ?>> CACHE = new HashMap<>();

    private AccessorFactory() {
    }

    @SuppressWarnings("unchecked")
    public static <A, R> Function<A, R> accessor(Class<A> obj, String name, Class<R> returnType) {
        String key = obj.getName() + '#' + name;
        Function<A, R> cached = (Function<A, R>) CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        synchronized(CACHE) {
        	cached = (Function<A, R>) CACHE.get(key);
            if (cached != null) {
                return cached;
            }
	        Function<A, R> created = build(obj, name, returnType);
	        Function<?, ?> prev = CACHE.putIfAbsent(key, created);
	        return prev == null ? created : (Function<A, R>) prev;
        }
    }

    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static <A, R> Function<A, R> build(Class<A> owner, String name, Class<R> returnType) {
        Target t = resolve(owner, name);

        if (returnType != null) {
            Class<?> expected = boxed(returnType);
            Class<?> actual = boxed(t.valueType());
            if (!expected.isAssignableFrom(actual)) {
                throw new IllegalArgumentException(
                        "Invalid return type, expect " + returnType.getName()
                                + " but got " + t.valueType().getName());
            }
        }

        if (t.isStatic()) {
            throw new UnsupportedOperationException(
                    "Static method is not supported:  "
                            + t.declaringClass().getName() + "#" + name);
        }

        // 1) 进入声明成员的那个类的运行时包
        MethodHandles.Lookup lookup;
        try {
            lookup = MethodHandles.privateLookupIn(t.declaringClass(), MethodHandles.lookup());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                    "Package " + t.declaringClass().getName() + " access denied,"
                            + "--add-opens is required for jdk internal class.", e);
        }

        // 2) 解析出直接句柄 —— 访问检查在此完成
        final MethodHandle mh = findDirectHandle(lookup, t, name);
        final String errMsg="Cannot access " + t.declaringClass().getName() + "#" + name + "";
        // 3) 普通 lambda 包装。JIT 会把 mh.invoke 的调用点内联到实际成员读取。
        return (Function<A, R>) (Function<Object, Object>) arg -> {
            try {
                return mh.invoke(arg);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(errMsg, e);
            }
        };
    }

    private static MethodHandle findDirectHandle(MethodHandles.Lookup lookup, Target t, String name) {
        try {
            if (t.field != null) {
                Field f = t.field;
                return lookup.findGetter(f.getDeclaringClass(), name, f.getType());
            }
            Method m = t.method;
            return lookup.findVirtual(
                    m.getDeclaringClass(), name, MethodType.methodType(m.getReturnType()));
        } catch (NoSuchFieldException | NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException("Member not found: " + name, e);
        }
    }

    // ------------------------------------------------------------------
    // 成员解析
    // ------------------------------------------------------------------

    private static Target resolve(Class<?> owner, String name) {
        Field field = findField(owner, name);
        if (field != null) {
            return new Target(field, null);
        }
        Method method = findMethod(owner, name);
        if (method != null) {
            if (method.getParameterCount() != 0) {
                throw new IllegalArgumentException("Only no-arg method supported: " + name);
            }
            return new Target(null, method);
        }
        throw new IllegalArgumentException(
                "Member not found: " + owner.getName() + "#" + name);
    }

    private static Field findField(Class<?> c, String name) {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try {
                return k.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                // 继续沿继承链查找
            }
        }
        for (Class<?> itf : c.getInterfaces()) {
            Field f = findField(itf, name);
            if (f != null) {
                return f;
            }
        }
        return null;
    }

    private static Method findMethod(Class<?> c, String name) {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try {
                return k.getDeclaredMethod(name);
            } catch (NoSuchMethodException ignored) {
                // 继续沿继承链查找
            }
        }
        for (Class<?> itf : c.getInterfaces()) {
            Method m = findMethod(itf, name);
            if (m != null) {
                return m;
            }
        }
        return null;
    }

    private static Class<?> boxed(Class<?> c) {
        if (!c.isPrimitive()) return c;
        if (c == int.class)     return Integer.class;
        if (c == long.class)    return Long.class;
        if (c == double.class)  return Double.class;
        if (c == float.class)   return Float.class;
        if (c == boolean.class) return Boolean.class;
        if (c == byte.class)    return Byte.class;
        if (c == char.class)    return Character.class;
        if (c == short.class)   return Short.class;
        if (c == void.class)    return Void.class;
        return c;
    }

    private static final class Target {
        final Field field;
        final Method method;

        Target(Field field, Method method) {
            this.field = field;
            this.method = method;
        }

        Class<?> declaringClass() {
            return field != null ? field.getDeclaringClass() : method.getDeclaringClass();
        }

        Class<?> valueType() {
            return field != null ? field.getType() : method.getReturnType();
        }

        boolean isStatic() {
            return field != null
                    ? Modifier.isStatic(field.getModifiers())
                    : Modifier.isStatic(method.getModifiers());
        }
    }
}