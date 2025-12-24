package com.teammoeg.frostedheart.item.townmanager;

import com.teammoeg.frostedheart.util.client.FHClientUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class TownManagerScreen extends Screen {
    public static ResourceLocation BACKGROUND_TEXTURE = FHClientUtils.makeGuiTextureLocation("town_manage_screen");

    public int imageWidth, imageHeight, leftPos, topPos;

    protected TownManagerScreen(Component pTitle) {
        super(pTitle);
    }

    @Override
    protected void init() {
        super.init();
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        renderBackground(pGuiGraphics);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }

    public void renderBackground(GuiGraphics pGuiGraphics){
        pGuiGraphics.blit(BACKGROUND_TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

}
