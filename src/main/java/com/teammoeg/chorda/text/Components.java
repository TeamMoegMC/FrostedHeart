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

package com.teammoeg.chorda.text;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.Collection;
import java.util.Map;

/**
 * 文本组件构建工具类，提供创建和操作Minecraft聊天组件的便捷方法。
 * 包含字面文本、可翻译文本、按键绑定的创建，以及颜色样式的应用。
 * <p>
 * Text component building utility class providing convenience methods for creating and manipulating
 * Minecraft chat components. Includes creation of literal text, translatable text, keybindings,
 * and application of color styles.
 */
public final class Components {
    private static final Component IMMUTABLE_EMPTY = Component.empty();

    /**
     * 获取一个不可变的空组件实例，用于避免创建额外对象。
     * <p>
     * Gets an immutable empty component instance to avoid creating extra objects.
     *
     * @return 不可变的空组件 / An immutable empty component
     */
    public static Component immutableEmpty() {
        return IMMUTABLE_EMPTY;
    }

    /**
     * 创建一个新的可变空组件。尽可能使用 {@link #immutableEmpty()} 以避免创建额外对象。
     * <p>
     * Creates a new mutable empty component. Use {@link #immutableEmpty()} when possible to prevent creating an extra object.
     *
     * @return 新的可变空组件 / A new mutable empty component
     */
    public static MutableComponent empty() {
        return Component.empty();
    }
    /**
     * 检查组件是否为空内容（内容为EMPTY且无兄弟组件）。
     * <p>
     * Checks if a component has EMPTY content and no siblings.
     *
     * @param title 要检查的组件 / The component to check
     * @return 如果组件为空则返回true / true if the component is empty
     */
    public static boolean isEmpty(Component title) {
    	return title.getContents()==ComponentContents.EMPTY&&title.getSiblings().isEmpty();
    }
    /**
     * 检查组件的字符串内容是否为空。
     * <p>
     * Checks if a component's string content is empty.
     *
     * @param component 要检查的可变组件 / The mutable component to check
     * @return 如果字符串内容为空则返回true / true if the string content is empty
     */
    public static boolean isContentEmpty(MutableComponent component) {
    	return component.getString(1).isEmpty();
    }
    /**
     * 创建一个字面文本组件。
     * <p>
     * Creates a literal text component.
     *
     * @param str 字面文本内容 / The literal text content
     * @return 字面文本组件 / A literal text component
     */
    public static MutableComponent literal(String str) {
        return Component.literal(str);
    }

    /**
     * 创建一个可翻译文本组件。
     * <p>
     * Creates a translatable text component.
     *
     * @param key 翻译键 / The translation key
     * @return 可翻译文本组件 / A translatable text component
     */
    public static MutableComponent translatable(String key) {
        return Component.translatable(key);
    }

    /**
     * 创建一个带参数的可翻译文本组件。
     * <p>
     * Creates a translatable text component with arguments.
     *
     * @param key 翻译键 / The translation key
     * @param args 翻译参数 / The translation arguments
     * @return 可翻译文本组件 / A translatable text component
     */
    public static MutableComponent translatable(String key, Object... args) {
        return Component.translatable(key, args);
    }

    /**
     * 创建一个按键绑定文本组件。
     * <p>
     * Creates a keybind text component.
     *
     * @param name 按键绑定名称 / The keybind name
     * @return 按键绑定组件 / A keybind component
     */
    public static MutableComponent keybind(String name) {
        return Component.keybind(name);
    }

    /**
     * 创建一个字符串文本组件，null或空字符串返回空组件。
     * <p>
     * Creates a string text component, returns empty component for null or empty strings.
     *
     * @param s 字符串内容 / The string content
     * @return 文本组件 / A text component
     */
    public static MutableComponent str(String s) {
    	if(s==null||s.isEmpty()) {
    		return Component.empty();
    	}
        return MutableComponent.create(new LiteralContents(s));
    }

    /**
     * 将集合转换为字符串文本组件，每个元素以换行分隔，使用toString方法。
     * <p>
     * Converts a collection to a string text component, listing all elements separated by new lines using toString.
     *
     * @param collection 要转换的集合 / The collection to convert
     * @return 字符串文本组件 / The string text component
     * @param <V> 集合元素类型 / The type of the collection elements
     */
    public static <V> MutableComponent str(Collection<V> collection) {

        StringBuilder sb = new StringBuilder();
        for (V v : collection) {
            sb.append(v.toString()).append("\n");
        }
        // remove the last newline if the string is not empty
        if (!sb.isEmpty()) {
            sb.setLength(sb.length() - 1);
        }
        return str(sb.toString());
    }

    /**
     * 将Map转换为字符串文本组件，每个条目以换行分隔，键值之间用冒号和空格分隔。
     * <p>
     * Converts a Map to a string text component, listing all entries separated by new lines,
     * with keys and values separated by a colon and a space.
     *
     * @param map 要转换的Map / The map to convert
     * @return 字符串文本组件 / The string text component
     * @param <K> 键的类型 / The type of the keys
     * @param <V> 值的类型 / The type of the values
     */
    public static <K, V> MutableComponent str(Map<K, V> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            sb.append(entry.getKey().toString()).append(": ").append(entry.getValue().toString()).append("\n");
        }
        return str(sb.toString());
    }

    /**
     * 获取组件的翻译键，若不是可翻译组件则返回其字符串值。
     * <p>
     * Gets the translation key of a component, or its string value if it is not a translatable component.
     *
     * @param component 要获取键的组件 / The component to get the key from
     * @return 翻译键或字符串值 / The translation key or string value
     */
    public static String getKeyOrElseStr(Component component) {
        if (component instanceof MutableComponent c && (c.getContents() instanceof TranslatableContents t)) {
            return t.getKey();
        } else {
            return component.getString();
        }
    }

    /**
     * 创建一个带指定颜色的字面文本组件。
     * <p>
     * Creates a literal text component with the specified color.
     *
     * @param text 文本内容 / The text content
     * @param color 颜色值 / The color value
     * @return 带颜色的文本组件 / A colored text component
     */
    public static MutableComponent withColor(String text, int color) {
        return withColor(Component.literal(text), color);
    }

    /**
     * 为一个不可变组件应用指定颜色。
     * <p>
     * Applies the specified color to an immutable component.
     *
     * @param text 文本组件 / The text component
     * @param color 颜色值 / The color value
     * @return 带颜色的可变文本组件 / A colored mutable text component
     */
    public static MutableComponent withColor(Component text, int color) {
        return withColor(Component.empty().append(text), color);
    }

    /**
     * 为一个可变组件应用指定颜色。
     * <p>
     * Applies the specified color to a mutable component.
     *
     * @param text 可变文本组件 / The mutable text component
     * @param color 颜色值 / The color value
     * @return 带颜色的可变文本组件 / The colored mutable text component
     */
    public static MutableComponent withColor(MutableComponent text, int color) {
        return text.withStyle(color(color));
    }

    /**
     * 创建一个带指定颜色的样式。
     * <p>
     * Creates a style with the specified color.
     *
     * @param color 颜色值 / The color value
     * @return 带颜色的样式 / A style with the specified color
     */
    public static Style color(int color) {
        return color(Style.EMPTY, color);
    }

    /**
     * 在现有样式上应用指定颜色。
     * <p>
     * Applies the specified color to an existing style.
     *
     * @param style 基础样式 / The base style
     * @param color 颜色值 / The color value
     * @return 带颜色的样式 / The style with the specified color
     */
    public static Style color(Style style, int color) {
        return style.withColor(color);
    }
}

