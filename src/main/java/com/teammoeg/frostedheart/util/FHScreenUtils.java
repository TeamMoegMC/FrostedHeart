package com.teammoeg.frostedheart.util;

import com.teammoeg.frostedheart.FHMain;
import net.minecraft.util.ResourceLocation;

public class FHScreenUtils {

    public static ResourceLocation makeTextureLocation(String name) {
        return FHMain.rl("textures/gui/" + name + ".png");
    }
}
