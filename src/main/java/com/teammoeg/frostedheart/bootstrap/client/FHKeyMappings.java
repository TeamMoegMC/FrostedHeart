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

package com.teammoeg.frostedheart.bootstrap.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.util.Lazy;
import org.lwjgl.glfw.GLFW;

public class FHKeyMappings {
    public static Lazy<KeyMapping> key_skipDialog = Lazy.of(() -> new KeyMapping("key.frostedheart.skip_dialog",
        GLFW.GLFW_KEY_Z, "key.categories.frostedheart"));
    public static Lazy<KeyMapping> key_InfraredView = Lazy.of(() -> new KeyMapping("key.frostedheart.infrared_view",
            GLFW.GLFW_KEY_I, "key.categories.frostedheart"));
    public static Lazy<KeyMapping> key_openWheelMenu = Lazy.of(() -> new KeyMapping("key.frostedheart.open_wheel_menu",
            GLFW.GLFW_KEY_TAB, "key.categories.frostedheart"));
    public static Lazy<KeyMapping> key_health = Lazy.of(() -> new KeyMapping("key.frostedheart.health",
            GLFW.GLFW_KEY_H, "key.categories.frostedheart"));
    public static Lazy<KeyMapping> key_clothes = Lazy.of(() -> new KeyMapping("key.frostedheart.clothes",
            GLFW.GLFW_KEY_Y, "key.categories.frostedheart"));
    public static void init() {

    }

    public static boolean hasSDown() {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_S);
    }

}
