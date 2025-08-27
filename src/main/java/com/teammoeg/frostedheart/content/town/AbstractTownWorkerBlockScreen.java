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

package com.teammoeg.frostedheart.content.town;

import blusunrize.immersiveengineering.client.gui.IEContainerScreen;
import com.teammoeg.chorda.block.entity.CBlockEntity;
import com.teammoeg.chorda.client.widget.TabImageButton;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.chorda.menu.CBlockEntityMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractTownWorkerBlockScreen<C extends CBlockEntityMenu<? extends AbstractTownWorkerBlockEntity>> extends IEContainerScreen<C>  {


    private int activeTab = 0;
    private final List<ITabContent> tabContents = new ArrayList<>();
    private final List<AbstractButton> tabButtons = new ArrayList<>();

    public AbstractTownWorkerBlockScreen(C inventorySlotsIn, Inventory inv, Component title,ResourceLocation background) {
        super(inventorySlotsIn, inv, title, background);
        super.imageWidth = 176;
        super.imageHeight = 166;

        AbstractTownWorkerBlockEntity blockEntity = getMenu().getBlock();
        addTabContent((left,top)->{
            this.addRenderableWidget(new Label(left + 10, top + 20, Components.str(blockEntity.isWorkValid() ? "Valid working environment" : "Invalid working environment"), 0xFFFFFF));
            this.addRenderableWidget(new Label(left + 10, top + 40, Components.str(blockEntity.isStructureValid() ? "Valid structure" : "Invalid structure"), 0xFFFFFF));
        });
    }


    @Override
    protected void init() {
        super.init();
        tabButtons.clear();

        int guiLeft = leftPos;
        int guiTop = topPos;

        // Create tab button
        for (int i = 0; i < tabContents.size(); i++) {
            final int tabI = i;
            if (i<3) {
                // Left side
                int x = guiLeft - 22;
                int y = guiTop + tabI * (18 + 2) +2;
                int[] uv = getTabButtonUV(tabI);
                TabImageButton tabButtonnew = new TabImageButton(getButtonTexture(i), x, y, 22, 18, uv[0], uv[1], tabI, button -> {
//                menu.sendMessage(0, tabI);
                    activeTab = tabButtons.indexOf(button);
                    updateTabContent();
                }).bind(() -> activeTab );
                tabButtons.add(tabButtonnew);
                this.addRenderableWidget(tabButtonnew);
            }else {
                // Right side
                int x = guiLeft + 175;
                int y = guiTop + tabI * (18 + 2) +2;
                int[] uv = getTabButtonUV(tabI);
                TabImageButton tabButtonnew = new TabImageButton(getButtonTexture(i), x, y, 22, 18, uv[0], uv[1], tabI, button -> {
//                menu.sendMessage(0, tabI);
                    activeTab = tabButtons.indexOf(button);
                    updateTabContent();
                }).bind(() -> activeTab);
                tabButtons.add(tabButtonnew);
                this.addRenderableWidget(tabButtonnew);
            }
        }
        // Initialize tab content
        updateTabContent();
    }

    private void updateTabContent() {
        clearContentWidgets();

        if (activeTab > tabButtons.size()) return;

        tabContents.get(activeTab).renderTabContent(leftPos,topPos);

    }
    private void clearContentWidgets() {
        // Remove widgets
        this.renderables.removeIf(widget -> (widget instanceof TabContentComponent));
    }

    public static class Label extends AbstractWidget implements TabContentComponent{
        private final Component text;
        private final int color;

        public Label(int x, int y, Component text, int color) {
            super(x, y, 0, 0, text);
            this.text = text;
            this.color = color;
            this.width = Minecraft.getInstance().font.width(text);
            this.height = Minecraft.getInstance().font.lineHeight;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            guiGraphics.drawString(Minecraft.getInstance().font, text, getX(), getY(), color);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

        }
    }

    public ResourceLocation getButtonTexture(int tabIndex) {
        return background;
    }
    public int[] getTabButtonUV(int tabIndex) {
        if (tabIndex < 3) {
            return new int[]{180, 59};
        } else {
            return new int[]{0, 0};
        }
    }

    protected void addTabContent(ITabContent content){
        tabContents.add(content);
    }

    public interface ITabContent {
        void renderTabContent(int guiLeft,int guiTop);
    }

    public interface TabContentComponent {}

}

