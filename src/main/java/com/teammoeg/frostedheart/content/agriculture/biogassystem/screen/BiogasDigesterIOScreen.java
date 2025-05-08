package com.teammoeg.frostedheart.content.agriculture.biogassystem.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class BiogasDigesterIOScreen extends AbstractContainerScreen<BiogasDigesterIOMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(FHMain.MODID,
            "textures/gui/biogas_digester_io_gui.png");

    public BiogasDigesterIOScreen(BiogasDigesterIOMenu handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }
    boolean b;
    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
    }
    @Override
    protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f,1f,1f,1f);
        RenderSystem.setShaderTexture(0,TEXTURE);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        context.blit(TEXTURE, x, y, 0, 0, imageWidth,imageHeight);

        renderProgressArrow(context, x, y);
        if (menu.isChecked()){
            context.blit(TEXTURE,x+123,y+60,176,0,25,12);
        }
        b = mouseX >= x + 108 && mouseX <= x + 121 && mouseY >= y + 60 && mouseY <= y + 71;
    }

    private void renderProgressArrow(GuiGraphics context, int x, int y) {
        if (menu.isCrafting()){
            context.blit(TEXTURE, x + 64, y + 36, 0, 166, menu.getScaledProgress(), 18);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics pGuiGraphics, int pX, int pY) {
        if (b){
            pGuiGraphics.renderTooltip(font,Component.literal(menu.getGasValue() + " L").withStyle(ChatFormatting.WHITE),pX,pY);
        }
        super.renderTooltip(pGuiGraphics, pX, pY);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        renderTooltip(context, mouseX, mouseY);
    }
}
