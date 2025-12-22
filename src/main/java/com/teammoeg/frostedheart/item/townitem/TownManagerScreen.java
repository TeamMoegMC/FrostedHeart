package com.teammoeg.frostedheart.item.townitem;

import com.teammoeg.chorda.client.widget.ITabContent;
import com.teammoeg.chorda.client.widget.TabElementWidget;
import com.teammoeg.chorda.client.widget.TabImageButton;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.frostedheart.content.town.AbstractTownWorkerBlockScreen;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.util.client.FHClientUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

import static com.teammoeg.frostedheart.content.town.AbstractTownWorkerBlockScreen.getTabButtonUV;

public class TownManagerScreen extends Screen {
    TeamTown town;
    public int leftPos, topPos, imageWidth, imageHeight;
    public static final ResourceLocation TEXTURE = FHClientUtils.makeGuiTextureLocation("town_manage_screen");
    public List<TabImageButton> tabButtons = new ArrayList<>();
    public List<ITabContent > tabContents = new ArrayList<>();
    int activeTab = 0;
    public TownManagerScreen(Component pTitle) {
        super(pTitle);
        this.tabContents.add((left, top) -> {
            this.addRenderableWidget(new TabElementWidget<>(new AbstractTownWorkerBlockScreen.Label(left + 10, top + 20, Components.str("test there is Tab0"), 0xFFFFFF)));
        });
        this.tabContents.add((left, top) -> {
            this.addRenderableWidget(new TabElementWidget<>(new AbstractTownWorkerBlockScreen.Label(left + 10, top + 20, Components.str("test there is Tab1"), 0xFFFFFF)));
        });
    }

    @Override
    protected void init() {
        super.init();
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        this.town = TeamTown.fromLocal();
        tabButtons.clear();


        int guiLeft = leftPos;
        int guiTop = topPos;

        // Create tab button
        for (int i = 0; i < tabContents.size(); i++) {
            final int tabI = i;
            /*if (i<3) {*/
            // Left side
            int x = guiLeft - 22;
            int y = guiTop + tabI * (18 + 2) +2;
            int[] uv = getTabButtonUV(tabI);
            TabImageButton tabButtonNew = new TabImageButton(TEXTURE, x, y, 22, 18, uv[0], uv[1], tabI, button -> {
                activeTab = tabButtons.indexOf(button);
                updateTabContent();
            }).bind(() -> activeTab );
            tabButtons.add(tabButtonNew);
            this.addRenderableWidget(tabButtonNew);
        }
        updateTabContent();
    }

    public void updateTabContent(){
        this.renderables.removeIf(widget -> widget instanceof TabElementWidget<?>);
        tabContents.get(activeTab).addRenderableWidgets(leftPos,topPos);
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        pGuiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }
}
