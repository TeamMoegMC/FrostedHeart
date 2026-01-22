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

import com.teammoeg.chorda.client.cui.MenuPrimaryLayer;
import com.teammoeg.chorda.client.cui.MouseButton;
import com.teammoeg.chorda.client.cui.TabImageButtonElement;
import com.teammoeg.chorda.client.cui.UILayer;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.menu.CBlockEntityMenu;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractTownWorkerBlockScreen<C extends CBlockEntityMenu<? extends AbstractTownWorkerBlockEntity>> extends MenuPrimaryLayer<C> {


    private int activeTab = 0;
    private final List<ITabContent> tabContents = new ArrayList<>();
    private final List<TabImageButtonElement> tabButtons = new ArrayList<>();
    private UILayer contentLayer;

    public AbstractTownWorkerBlockScreen(C inventorySlotsIn) {
        super(inventorySlotsIn);

    }
/*    @Override
    public boolean onInit() {
        tabButtons.clear();


//        this.setSize(176, 222);
        int guiLeft = 0*//*getContentX()*//*;
        int guiTop = 0*//*getContentY()*//*;
        int leftPos = (this.width - 176) / 2;
        int topPos = (this.height - 222) / 2;
//        super.onI nit();
//        this.setPos(leftPos, topPos);
//        this.setPos(0, 0);
//        this.setSize(176, 222);



        // Create tab button
*//*        for (int i = 0; i < tabContents.size(); i++) {
            final int tabI = i;
            *//**//*if (i<3) {*//**//*
                // Left side
                int x = guiLeft - 22;
                int y = guiTop + tabI * (18 + 2) +2;
                int[] uv = getTabButtonUV(tabI);
            TabImageButtonElement tabButtonNew = new TabImageButtonElement(this, x, y, 22, 18, uv[0], uv[1], tabI,CIcon){
//                menu.sendMessage(0, tabI);
                @Override
                public void onClicked(MouseButton mouseButton) {
                    activeTab = tabButtons.indexOf(mouseButton);
                    updateTabContent();
                }
            };

            tabButtons.add(tabButtonNew);

//                this.addRenderableWidget(tabButtonNew);
            *//**//*}else {
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
            }*//**//*
        }*//*
        // Initialize tab content

        updateTabContent();
        return true;
    }*/

    @Override
    public void addUIElements() {

        //初始化内容容器
        if (this.contentLayer == null) {
            this.contentLayer = new UILayer(this) {
                @Override public void addUIElements() {
                        if (activeTab >= 0 && activeTab < tabContents.size()) {
                            tabContents.get(activeTab).renderTabContent(this);
                    }
                }
                @Override public void alignWidgets() {}
            };
        }
        this.contentLayer.setSize(176, 222);
        this.add(this.contentLayer);

        //初始化 Tab 按钮
        this.tabButtons.clear();
        for (int i = 0; i < tabContents.size(); i++) {
            final int tabI = i;
            int[] uv = getTabButtonUV(tabI);
            int btnX = -22;
            int btnY = 2 + i * 20;
            TabImageButtonElement btn = new TabImageButtonElement(this, btnX, btnY, 22, 18, tabI, getButtonIcon(0), getButtonIcon(1)){
                @Override
                public void onClicked(MouseButton button) {
                    selectTab(tabI);
                }

            };
            btn.bind(() -> activeTab);
            this.tabButtons.add(btn);
            this.add(btn); // 添加到主 Screen
        }


    }
    public void selectTab(int index) {
        if (index < 0 || index >= tabContents.size()) return;
        this.activeTab = index;

        updateTabContent();
    }

    private void updateTabContent() {
        if (contentLayer != null) {

            contentLayer.refresh();
        }
    }

    public CBlockEntityMenu getCMenu() {
        return container;
    }

    public int[] getTabButtonUV(int tabIndex) {
        /*if (tabIndex < 3) {*/
            return new int[]{180, 59};
        /*} else {
            return new int[]{0, 0};
        }*/
    }

    @Override
    public boolean onInit() {
        int sw = 176;
        int sh = 222;
        this.setSize(sw, sh);
        return super.onInit();
    }
    @Override
    public void setSizeToContentSize() {
    }
    @Override
    public int getContentHeight() {
        return 222;
    }

    public int getHeight() {
        return 222;
    }

    public CIcons.CIcon getButtonIcon(int i) {
        return CIcons.nop();
    }

    protected void addTabContent(ITabContent content){
        tabContents.add(content);
    }

    public interface ITabContent {
        void renderTabContent(UILayer layer);
    }

    public interface TabContentComponent {}

}

