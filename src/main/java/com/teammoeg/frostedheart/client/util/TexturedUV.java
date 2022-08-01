package com.teammoeg.frostedheart.client.util;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.util.ResourceLocation;

public class TexturedUV extends UV {
    ResourceLocation texture;

    public TexturedUV(ResourceLocation texture, int x, int y, int w, int h) {
        super(x, y, w, h);
        this.texture = texture;
    }

    public TexturedUV(ResourceLocation texture, UV uv) {
        super(uv);
        this.texture = texture;
    }

    //blit with texture bind at IngameGui add point
    public void blit(Minecraft mc, MatrixStack s, int lx, int ly, Point loc) {
        mc.getTextureManager().bindTexture(texture);
        super.blit(mc.ingameGUI, s, lx, ly, loc);
    }

    //blit with texture bind at IngameGui
    public void blit(Minecraft mc, MatrixStack s, int lx, int ly) {
        mc.getTextureManager().bindTexture(texture);
        super.blit(mc.ingameGUI, s, lx, ly);
    }

    //blit with texture bind add point
    public void blit(Minecraft mc, AbstractGui gui, MatrixStack s, int lx, int ly, Point loc) {
        mc.getTextureManager().bindTexture(texture);
        super.blit(gui, s, lx, ly, loc);
    }

    //blit with texture bind
    public void blit(Minecraft mc, AbstractGui gui, MatrixStack s, int lx, int ly) {
        mc.getTextureManager().bindTexture(texture);
        super.blit(gui, s, lx, ly);
    }
}
