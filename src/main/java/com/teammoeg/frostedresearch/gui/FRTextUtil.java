/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedresearch.gui;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.teammoeg.chorda.client.StringTextComponentParser;
import com.teammoeg.frostedresearch.FRMain;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

public class FRTextUtil {

    private FRTextUtil() {
    }

    public static List<Component> get(List<String> orig, String type, String pid) {
        List<Component> li = new ArrayList<>();
        if (orig.isEmpty()) {
            int i = 0;
            while (true) {
                final int fi = i;
                i++;
                Component it = null;
                it = getOptional(null, type, pid + "." + fi);
                if (it != null)
                    li.add(it);
                else
                    return li;
            }

        }
        for (int i = 0; i < orig.size(); i++) {
            final int fi = i;
            li.add(get(orig.get(i), type,  pid + "." + fi));
        }
        return li;
    }

    @Nonnull
    public static Component get(String orig, String type,String pid) {
        if (orig == null || orig.isEmpty())
            return StringTextComponentParser.parse("{" + type + "." + FRMain.MODID + "." + pid+ "}");
        if (orig.startsWith("@")) {
            if (orig.length() == 1)
                return StringTextComponentParser.parse("{" + type + "." + FRMain.MODID + "." + pid + "}");
            return StringTextComponentParser.parse("{" + orig.substring(1) + "}");
        }

        return StringTextComponentParser.parse(orig);
    }

    @Nullable
    public static Component getOptional(String orig, String type, String pid) {
        if (orig == null || orig.isEmpty()) {
            String key = type + "." + FRMain.MODID + "." + pid;
            if (I18n.exists(key))
                return StringTextComponentParser.parse("{" + key + "}");
            return null;
        }
        if (orig.startsWith("@")) {
            if (orig.length() == 1)
                return StringTextComponentParser.parse("{" + type + "." + FRMain.MODID + "." + pid + "}");
            return StringTextComponentParser.parse("{" + orig.substring(1) + "}");
        }

        return StringTextComponentParser.parse(orig);
    }
}
