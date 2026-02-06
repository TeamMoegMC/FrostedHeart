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

package com.teammoeg.chorda.client.cui.category;

import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.text.Components;
import com.teammoeg.frostedheart.content.archive.ArchiveCategory;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class CategoryHelper {

    public static Entry findRelative(Entry entry, int offset) {
        if (entry == null) return null;
        if (!(entry.getParent() instanceof Category) || offset == 0) return entry;

        List<Entry> allEntries = new ArrayList<>();
        collectAllEntries(entry.getParent().root, allEntries);
        if (allEntries.isEmpty()) return entry;

        int currentIndex = -1;
        for (int i = 0; i < allEntries.size(); i++) {
            if (allEntries.get(i) == entry) {
                currentIndex = i;
                break;
            }
        }
        if (currentIndex == -1) return entry;

        int size = allEntries.size();
        int targetIndex = (currentIndex + offset) % size;
        if (targetIndex < 0) {
            targetIndex += size;
        }
        return allEntries.get(targetIndex);
    }

    public static Entry next(Entry entry) {
        return findRelative(entry, 1);
    }

    public static Entry prev(Entry entry) {
        return findRelative(entry, -1);
    }

    public static void collectAllEntries(Category root, List<Entry> entries) {
        if (root == null || entries == null) return;
        for (UIElement element : root.getElements()) {
            if (element instanceof Entry entry) {
                entries.add(entry);
            } else if (element instanceof Category category) {
                collectAllEntries(category, entries);
            }
        }
    }

    public static String getRawTitle(UIElement widget) {
        return widget == null ? "" : Components.getKeyOrElseStr(widget.getTitle());
    }

    public static String path(Entry entry) {
        if (entry == null) return "/";
        StringBuilder path = new StringBuilder(getRawTitle(entry)).append("/");
        UIElement parent = entry.getParent();
        while (!(parent instanceof ArchiveCategory)) {
            path.insert(0, getRawTitle(parent) + "/");
            parent = parent.getParent();
        }
        return path.toString();
    }

    public static Component translatePath(String path) {
        var c = Component.empty();
        if (path != null && !path.isBlank()) {
            for (String s : path.split("/")) {
                if (I18n.exists(s)) {
                    c.append(Component.translatable(s));
                } else {
                    c.append(s);
                }
                c.append("/");
            }
        }
        return c;
    }
}
