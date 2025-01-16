package com.teammoeg.chorda;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;

public class CUtils {
    public static boolean isDown(int key) {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), key);
    }
}
