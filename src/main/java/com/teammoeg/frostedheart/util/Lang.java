/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.teammoeg.frostedheart.FHMain;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public class Lang {
    public static TranslationTextComponent translate(String key, Object... args) {
        return createTranslationTextComponent(key, args);
    }

    public static TranslationTextComponent createTranslationTextComponent(String key, Object... args) {
        return new TranslationTextComponent(FHMain.MODID + "." + key, args);
    }

    public static void sendStatus(PlayerEntity player, String key, Object... args) {
        player.sendStatusMessage(createTranslationTextComponent(key, args), true);
    }

    public static List<ITextComponent> translatedOptions(String prefix, String... keys) {
        List<ITextComponent> result = new ArrayList<>(keys.length);
        for (String key : keys)
            result.add(translate(prefix + "." + key));

        return result;
    }

    public static String asId(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
