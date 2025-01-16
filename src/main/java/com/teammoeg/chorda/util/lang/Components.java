package com.teammoeg.chorda.util.lang;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.Collection;
import java.util.Map;

public final class Components {
    private static final Component IMMUTABLE_EMPTY = Component.empty();

    public static Component immutableEmpty() {
        return IMMUTABLE_EMPTY;
    }

    /** Use {@link #immutableEmpty()} when possible to prevent creating an extra object. */
    public static MutableComponent empty() {
        return Component.empty();
    }

    public static MutableComponent literal(String str) {
        return Component.literal(str);
    }

    public static MutableComponent translatable(String key) {
        return Component.translatable(key);
    }

    public static MutableComponent translatable(String key, Object... args) {
        return Component.translatable(key, args);
    }

    public static MutableComponent keybind(String name) {
        return Component.keybind(name);
    }

    public static MutableComponent str(String s) {
    	if(s==null||s.isEmpty()) {
    		return Component.empty();
    	}
        return MutableComponent.create(new LiteralContents(s));
    }

    /**
     * Convert a collection to a string text component
     * <p></p>
     * Lists all elements in the collection, separated by new lines
     * Uses the toString method of each element
     * @param collection the collection
     * @return the string text component
     * @param <V> the type of the collection
     */
    public static <V> MutableComponent str(Collection<V> collection) {

        StringBuilder sb = new StringBuilder();
        for (V v : collection) {
            sb.append(v.toString()).append("\n");
        }
        // remove the last newline if the string is not empty
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return str(sb.toString());
    }

    /**
     * Convert a Map to a string text component
     * <p></p>
     * Lists all elements in the map, separated by new lines
     * For each entry, use the toString method of the key and value,
     * separated by a colon and a space
     * @param map the map
     * @return the string text component
     * @param <K> the type of the keys
     * @param <V> the type of the values
     */
    public static <K, V> MutableComponent str(Map<K, V> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            sb.append(entry.getKey().toString()).append(": ").append(entry.getValue().toString()).append("\n");
        }
        return str(sb.toString());
    }

    public static String getKeyOrElseStr(Component component) {
        if (component instanceof MutableComponent c && (c.getContents() instanceof TranslatableContents t)) {
            return t.getKey();
        } else {
            return component.getString();
        }
    }

    public static MutableComponent translateOrElseStr(String string) {
        return I18n.exists(string) ? Component.translatable(string) : str(string);
    }
}

