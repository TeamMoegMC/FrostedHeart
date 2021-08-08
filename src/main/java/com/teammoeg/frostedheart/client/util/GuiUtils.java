package com.teammoeg.frostedheart.client.util;

import com.teammoeg.frostedheart.FHMain;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;

public class GuiUtils {

    public static ResourceLocation makeTextureLocation(String name) {
        return FHMain.rl("textures/gui/" + name + ".png");
    }

    public static TranslationTextComponent translateGui(String name) {
        return new TranslationTextComponent(FHMain.MODID + ".gui." + name);
    }
}
