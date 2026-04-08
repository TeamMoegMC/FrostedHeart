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
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.text.Components;
import com.teammoeg.frostedheart.content.archive.ArchiveCategory;
import com.teammoeg.frostedheart.content.tips.TipHelper;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 分类辅助工具类。提供条目导航、路径查找和路径翻译等功能。
 * <p>
 * Category helper utility class. Provides entry navigation, path lookup and path translation.
 */
public class CategoryHelper {

    /**
     * 查找相对于给定条目偏移量位置的条目，支持循环。
     * <p>
     * Finds the entry at a relative offset from the given entry, with wrapping.
     *
     * @param entry  起始条目 / the starting entry
     * @param offset 偏移量 / the offset
     * @return 目标条目 / the target entry
     */
    public static Entry findRelative(Entry entry, int offset) {
        if (entry == null) return null;
        if (!(entry.getParent() instanceof Category) || offset == 0) return entry;

        List<Entry> allEntries = new ArrayList<>();
        collectAllEntries(entry.getParent().getRoot(), allEntries);
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

    /**
     * 获取下一个条目。
     * <p>
     * Gets the next entry.
     *
     * @param entry 当前条目 / the current entry
     * @return 下一个条目 / the next entry
     */
    public static Entry next(Entry entry) {
        return findRelative(entry, 1);
    }

    /**
     * 获取上一个条目。
     * <p>
     * Gets the previous entry.
     *
     * @param entry 当前条目 / the current entry
     * @return 上一个条目 / the previous entry
     */
    public static Entry prev(Entry entry) {
        return findRelative(entry, -1);
    }

    /**
     * 递归收集分类树下的所有条目。
     * <p>
     * Recursively collects all entries under the category tree.
     *
     * @param root    根分类 / the root category
     * @param entries 用于存放结果的列表 / the list to store results
     */
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

    /**
     * 获取UI元素的原始标题文本。
     * <p>
     * Gets the raw title text of a UI element.
     *
     * @param widget UI元素 / the UI element
     * @return 原始标题字符串 / the raw title string
     */
    public static String getRawTitle(UIElement widget) {
        if (widget == null) {
            return "";
        } else if (widget instanceof Entry entry) {
            return entry.getIdentifier() + "$$" + Components.getKeyOrElseStr(entry.getTitle());
        } else {
            return Components.getKeyOrElseStr(widget.getTitle());
        }
    }

    /**
     * 构建条目从根到当前位置的完整路径字符串。
     * <p>
     * Builds the full path string from root to the given entry.
     *
     * @param entry 目标条目 / the target entry
     * @return 路径字符串 / the path string
     */
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

    /**
     * 将路径字符串中的翻译键转换为本地化文本组件。
     * <p>
     * Translates translation keys in a path string into localized text components.
     *
     * @param path 路径字符串 / the path string
     * @return 翻译后的文本组件 / the translated text component
     */
    public static Component translatePath(String path) {
        var c = Component.empty();
        if (path != null && !path.isBlank()) {
            for (String s : path.split("/")) {
                if (s.contains("$$")) {
                    var s2 = s.split("\\$\\$", -1);
                    s = s2.length >= 2 ? s2[1] : s2[0];
                }
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

    public static Category toCategory(UILayer parent, UILayer layer) {
        var main = new Category(parent, layer.getTitle());

        for (UIElement ele : layer.getElements()) {
            if (ele instanceof UILayer l) {
                main.add(toCategory(main, l));
            } else {
                main.add(new Entry(main, ele.getTitle()) {
                    @Override
                    public String getIdentifier() {
                        return TipHelper.randomString();
                    }
                });
            }
        }

        return main;
    }
}
