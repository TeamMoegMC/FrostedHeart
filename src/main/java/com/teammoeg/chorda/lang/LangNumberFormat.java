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

package com.teammoeg.chorda.lang;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import java.text.NumberFormat;
import java.util.Locale;


public class LangNumberFormat {

    private NumberFormat format = NumberFormat.getNumberInstance(Locale.ROOT);
    public static LangNumberFormat numberFormat = new LangNumberFormat();

    public NumberFormat get() {
        return format;
    }
   
    public void update() {
        format = NumberFormat.getInstance(Minecraft.getInstance()
                .getLanguageManager()
                .getJavaLocale());
        format.setMaximumFractionDigits(2);
        format.setMinimumFractionDigits(0);
        format.setGroupingUsed(true);
    }

    public static String format(double d) {
        if (Mth.equal(d, 0))
            d = 0;
        return numberFormat.get()
                .format(d)
                .replace("\u00A0", " ");
    }

}
