package com.teammoeg.frostedheart.content.utility.snowsack;

import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SnowSackScreen extends AbstractContainerScreen<SnowSackMenu> {
    // GUI贴图位置: textures/gui/snow_sack.png
    // 贴图尺寸: 176x166 (标准容器大小)
    // 雪量条背景位置: (176, 0) 尺寸: 12x52
    // 雪量条填充位置: (188, 0) 尺寸: 12x52 (从底部向上绘制)
    // 贴图还没有：private static final ResourceLocation SNOW_SACK_TEXTURE = new ResourceLocation(FHMain.MODID, "textures/gui/snow_sack.png");

    public SnowSackScreen(SnowSackMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int x, int y) {
        // 使用纯色背景替代贴图
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        
        // 绘制纯色背景 (深灰色背景)
        graphics.fill(i, j, i + this.imageWidth, j + this.imageHeight, 0xFF404040);
        
        // 绘制标题栏背景 (稍浅的灰色)
        graphics.fill(i, j, i + this.imageWidth, j + 15, 0xFF606060);
        
        // 渲染雪的数量条背景 (深色背景)
        int snowBarHeight = 52;
        graphics.fill(i + 150, j + 16, i + 150 + 12, j + 16 + snowBarHeight, 0xFF202020);
        
        // 渲染雪的数量条
        int snowAmount = this.menu.getSnowAmount();
        int maxSnowAmount = 1024; // 最大雪量
        int filledHeight = (int) (((float) snowAmount / maxSnowAmount) * snowBarHeight);
        
        // 绘制雪量条填充（从底部向上绘制，使用蓝色表示雪量）
        graphics.fill(i + 150, j + 16 + snowBarHeight - filledHeight, i + 150 + 12, j + 16 + snowBarHeight, 0xFF87CEEB);
        
        // 移除了对不存在贴图的引用
        // RenderSystem.setShader(GameRenderer::getPositionTexShader);
        // RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        // RenderSystem.setShaderTexture(0, SNOW_SACK_TEXTURE);
        // graphics.blit(SNOW_SACK_TEXTURE, i, j, 0, 0, this.imageWidth, this.imageHeight);
        // graphics.blit(SNOW_SACK_TEXTURE, i + 150, j + 16, 176, 0, 12, snowBarHeight);
        // graphics.blit(SNOW_SACK_TEXTURE, i + 150, j + 16 + snowBarHeight - filledHeight, 188, snowBarHeight - filledHeight, 12, filledHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int x, int y) {
        // 绘制标题
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFFFFFF, false);
        
        // 绘制雪的数量
        int snowAmount = this.menu.getSnowAmount();
        Component snowText = Component.literal(snowAmount + "/1024");
        graphics.drawString(this.font, snowText, (this.imageWidth - this.font.width(snowText)) / 2, 20, 0xFFFFFFFF, false);
        
        // 绘制自动拾取状态
        boolean autoPickup = this.menu.isAutoPickupEnabled();
        Component autoPickupText = Component.translatable("gui.frostedheart.snow_sack.auto_pickup", 
            autoPickup ? Component.translatable("gui.frostedheart.snow_sack.enabled") : Component.translatable("gui.frostedheart.snow_sack.disabled"));
        graphics.drawString(this.font, autoPickupText, (this.imageWidth - this.font.width(autoPickupText)) / 2, 70, 0xFFFFFFFF, false);
    }
}