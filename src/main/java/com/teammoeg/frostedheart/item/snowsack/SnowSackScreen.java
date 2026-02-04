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

package com.teammoeg.frostedheart.item.snowsack;

import com.teammoeg.frostedheart.FHNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

//不建议开发时参考此类
@OnlyIn(Dist.CLIENT)
public class SnowSackScreen extends AbstractContainerScreen<SnowSackMenu> implements MenuAccess<SnowSackMenu> {
    // GUI贴图位置: textures/gui/snow_sack.png
    // 贴图尺寸: 176x166 (标准容器大小)
    // 雪量条背景位置: (176, 0) 尺寸: 12x52
    // 雪量条填充位置: (188, 0) 尺寸: 12x52 (从底部向上绘制)
    // 贴图还没有：private static final ResourceLocation SNOW_SACK_TEXTURE = new ResourceLocation(FHMain.MODID, "textures/gui/snow_sack.png");
    
    private Button autoPickupButton;

    public SnowSackScreen(SnowSackMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }
    
    @Override
    protected void init() {
        super.init();
        
        // 添加自动拾取切换按钮，调整位置避免与物品栏重叠
        this.autoPickupButton = this.addRenderableWidget(Button.builder(
                this.getAutoPickupText(), 
                (button) -> {
                    this.menu.toggleAutoPickup();
                    button.setMessage(this.getAutoPickupText());
                    FHNetwork.INSTANCE.sendToServer(new ToggleAutoPickupMessage());
                })
                .pos(this.leftPos + 8, this.topPos + 70)
                .size(80, 16)
                .build());
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
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
        int maxSnowAmount = this.menu.getMaxSnowAmount(); // 最大雪量
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
        Component snowText = Component.literal(snowAmount + "/" + this.menu.getMaxSnowAmount());
        graphics.drawString(this.font, snowText, (this.imageWidth - this.font.width(snowText)) / 2, 20, 0xFFFFFFFF, false);
        
        // 绘制自动拾取状态文本
        //Component autoPickupText = this.getAutoPickupText();
        //graphics.drawString(this.font, autoPickupText, 8, 60, 0xFFFFFFFF, false);
    }
    
    private Component getAutoPickupText() {
        boolean autoPickup = this.menu.isAutoPickupEnabled();
        return Component.translatable("gui.frostedheart.snow_sack.auto_pickup", 
            autoPickup ? Component.translatable("gui.frostedheart.snow_sack.enabled") : Component.translatable("gui.frostedheart.snow_sack.disabled"));
    }
    
    @Override
    public void containerTick() {
        super.containerTick();
        // 定期更新按钮文本以确保显示正确状态
        if (this.autoPickupButton != null) {
            this.autoPickupButton.setMessage(this.getAutoPickupText());
        }
    }
}