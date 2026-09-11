package com.teammoeg.chorda.asm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 纯 {@link MethodHandles} 实现的成员访问器工厂。
 *
 * <p>用 {@link MethodHandles#privateLookupIn} 进入声明成员的类的运行时包，
 * 解析出<b>直接</b>{@link MethodHandle}——访问检查在此一次性完成，之后不受访问控制约束。
 *
 * <p>不再使用 {@link java.lang.invoke.LambdaMetafactory}：
 * <ul>
 *   <li>传调用方的 {@code MethodHandles.lookup()} → {@code is not direct or cannot be cracked}
 *       （看不到 private 成员）；</li>
 *   <li>传 {@code privateLookupIn} 的结果 → {@code Invalid caller}
 *       （ModLauncher / 自定义类加载器下模块不匹配）。</li>
 * </ul>
 * 两头都堵，所以直接返回调用 {@code mh.invoke(arg)} 的普通 lambda。
 */
public final class AccessorFactory {

    private static final ConcurrentHashMap<String, Function<?, ?>> CACHE = new ConcurrentHashMap<>();

    private AccessorFactory() {
    }

    @SuppressWarnings("unchecked")
    public static <A, R> Function<A, R> accessor(Class<A> obj, String name, Class<R> returnType) {
        String key = obj.getName() + '#' + name;
        Function<A, R> cached = (Function<A, R>) CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        Function<A, R> created = build(obj, name, returnType);
        Function<?, ?> prev = CACHE.putIfAbsent(key, created);
        return prev == null ? created : (Function<A, R>) prev;
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
                        "返回类型不匹配: 期望 " + returnType.getName()
                                + "，实际 " + t.valueType().getName());
            }
        }

        if (t.isStatic()) {
            throw new UnsupportedOperationException(
                    "静态成员需要额外接收者参数，本实现暂不支持: "
                            + t.declaringClass().getName() + "#" + name);
        }

        // 1) 进入声明成员的那个类的运行时包
        MethodHandles.Lookup lookup;
        try {
            lookup = MethodHandles.privateLookupIn(t.declaringClass(), MethodHandles.lookup());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                    "无法进入 " + t.declaringClass().getName() + " 的运行时包。"
                            + "若目标是 JDK 内部类，请对相应模块添加 --add-opens。", e);
        }

        // 2) 解析出直接句柄 —— 访问检查在此完成
        final MethodHandle mh = findDirectHandle(lookup, t, name);

        // 3) 普通 lambda 包装。JIT 会把 mh.invoke 的调用点内联到实际成员读取。
        return (Function<A, R>) (Function<Object, Object>) arg -> {
            try {
                return mh.invoke(arg);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(
                        "访问 " + t.declaringClass().getName() + "#" + name + " 失败", e);
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
            throw new IllegalStateException("无法解析成员: " + name, e);
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
                throw new IllegalArgumentException("只支持无参方法: " + name);
            }
            return new Target(null, method);
        }
        throw new IllegalArgumentException(
                "在 " + owner.getName() + " 及其父类/接口中找不到成员: " + name);
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