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

public class BiogasDigesterControllerScreen extends AbstractContainerScreen<BiogasDigesterControllerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(FHMain.MODID,
            "textures/gui/biogas_digester_controller_gui.png");

    public BiogasDigesterControllerScreen(BiogasDigesterControllerMenu handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }
    boolean b1;
    boolean b2;
    boolean b3;
    boolean b4;
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

        context.drawString(font,String.valueOf(menu.getSize()),x+70,y+22,0xffffff,true);
        context.drawString(font,String.valueOf(menu.getGasValue()),x+70,y+38,0x00ff00,true);
        context.drawString(font,String.valueOf(menu.getSize() * 1000),x+70,y+54,0xff0000,true);
        b1 = mouseX >= x + 52 && mouseY >= y + 19 && mouseX <= x + 65 && mouseY <= y + 33;
        b2 = mouseX >= x + 52 && mouseY >= y + 35 && mouseX <= x + 65 && mouseY <= y + 49;
        b3 = mouseX >= x + 52 && mouseY >= y + 51 && mouseX <= x + 65 && mouseY <= y + 65;

        if (menu.getChecked()==1){
            context.blit(TEXTURE,x+9,y+58,176,0,25,12);
            b4 = false;
        } else {
            b4 = mouseX >= x + 9 && mouseY >= y + 58 && mouseX <= x + 33 && mouseY <= y + 69;
        }
        if (mouseX >= x + 161 && mouseX <= x + 171 && mouseY >= y + 5 && mouseY <= y + 15){
            context.blit(TEXTURE,x+161,y+5,194,13,11,11);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics context, int mouseX, int mouseY) {
        if (b1){
            context.renderTooltip(font,Component.translatable("gui.frostedheart.biogas_digester_controller_menu.size").withStyle(ChatFormatting.WHITE),mouseX,mouseY);
        }
        if (b2){
            context.renderTooltip(font,Component.translatable("gui.frostedheart.biogas_digester_controller_menu.gas_value").withStyle(ChatFormatting.GREEN),mouseX,mouseY);
        }
        if (b3){
            context.renderTooltip(font,Component.translatable("gui.frostedheart.biogas_digester_controller_menu.max_gas_value").withStyle(ChatFormatting.RED),mouseX,mouseY);
        }
        if (b4){
            context.renderTooltip(font,Component.translatable("gui.frostedheart.biogas_digester_controller_menu.unavailable").withStyle(ChatFormatting.RED),mouseX,mouseY);
        }
        super.renderTooltip(context, mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        renderTooltip(context, mouseX, mouseY);
    }
}
