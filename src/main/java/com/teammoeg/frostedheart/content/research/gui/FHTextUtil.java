/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.research.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.teammoeg.frostedheart.FHMain;

import dev.ftb.mods.ftblibrary.util.client.ClientTextComponentUtils;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

public class FHTextUtil {

    public static List<Component> get(List<String> orig, String type, Supplier<String> pid) {
        String s = pid.get();
        List<Component> li = new ArrayList<>();
        if (orig.isEmpty()) {
            int i = 0;
            while (true) {
                final int fi = i;
                i++;
                Component it = null;
                it = getOptional(null, type, () -> s + "." + fi);
                if (it != null)
                    li.add(it);
                else
                    return li;
            }

        }
        for (int i = 0; i < orig.size(); i++) {
            final int fi = i;
            li.add(get(orig.get(i), type, () -> s + "." + fi));
        }
        return li;
    }

    @Nonnull
    public static Component get(String orig, String type, Supplier<String> pid) {
        if (orig == null || orig.isEmpty())
            return ClientTextComponentUtils.parse("{" + type + "." + FHMain.MODID + "." + pid.get() + "}");
        if (orig.startsWith("@")) {
            if (orig.length() == 1)
                return ClientTextComponentUtils.parse("{" + type + "." + FHMain.MODID + "." + pid.get() + "}");
            return ClientTextComponentUtils.parse("{" + orig.substring(1) + "}");
        }

        return ClientTextComponentUtils.parse(orig);
    }

    @Nullable
    public static Component getOptional(String orig, String type, Supplier<String> pid) {
        if (orig == null || orig.isEmpty()) {
            String key = type + "." + FHMain.MODID + "." + pid.get();
            if (I18n.exists(key))
                return ClientTextComponentUtils.parse("{" + key + "}");
            return null;
        }
        if (orig.startsWith("@")) {
            if (orig.length() == 1)
                return ClientTextComponentUtils.parse("{" + type + "." + FHMain.MODID + "." + pid.get() + "}");
            return ClientTextComponentUtils.parse("{" + orig.substring(1) + "}");
        }

        return ClientTextComponentUtils.parse(orig);
    }

    private FHTextUtil() {
    }
}
