package com.teammoeg.frostedheart.client.util;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

public class AtlasUV extends TexturedUV {
    public AtlasUV(ResourceLocation texture, int w, int h) {
        super(texture, 0, 0, w, h);
    }

    public AtlasUV(ResourceLocation texture, int x, int y, int w, int h) {
        super(texture, x, y, w, h);
    }

    //blit with texture bind and altas set add point
    public void blit(Minecraft mc, MatrixStack s, int lx, int ly, Point loc, int mx, int my, int p3, int p4) {
        mc.getTextureManager().bindTexture(texture);
        super.blit(s, lx, ly, loc, mx, my, p3, p4);
    }

    //blit with texture bind and altas set
    public void blit(Minecraft mc, MatrixStack s, int lx, int ly, int mx, int my, int p3, int p4) {
        mc.getTextureManager().bindTexture(texture);
        super.blit(s, lx, ly, mx, my, p3, p4);
    }
}
