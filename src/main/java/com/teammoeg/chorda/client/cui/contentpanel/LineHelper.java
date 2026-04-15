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

package com.teammoeg.chorda.client.cui.contentpanel;

import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.theme.Coloring;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.List;

/**
 * 内容行工厂辅助类，提供创建各类内容行（文本、图片、物品、空白、分割线）的静态方法。
 * <p>
 * Content line factory helper providing static methods to create various line types
 * (text, image, item, space, break).
 */
public class LineHelper {
    public static TextLine text(UIElement parent, String text) {
        return text(parent, Component.literal(text));
    }

    public static TextLine text(UIElement parent, Component text) {
        return new TextLine(parent, text, Alignment.LEFT);
    }

    public static ImageLine img(UIElement parent, String imageLocation) {
        return img(parent, ResourceLocation.tryParse(imageLocation));
    }

    public static ImageLine img(UIElement parent, ResourceLocation imageLocation) {
        return new ImageLine(parent, imageLocation, Alignment.CENTER);
    }

    public static ItemRow items(UIElement parent, ItemStack... items) {
        return items(parent, List.of(items));
    }

    public static ItemRow items(UIElement parent, Collection<ItemStack> items) {
        return new ItemRow(parent, items, Alignment.CENTER);
    }

    public static EmptyLine space(UIElement parent) {
        return space(parent, 8);
    }

    public static EmptyLine space(UIElement parent, int height) {
        return new EmptyLine(parent, height);
    }

    public static BreakLine br(UIElement parent) {
        return new BreakLine(parent);
    }

    public static BreakLine br(UIElement parent, Coloring color) {
        return br(parent).color(color);
    }
}
