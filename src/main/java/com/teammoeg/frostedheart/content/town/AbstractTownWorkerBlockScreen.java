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
            int btnX = -22;
            int btnY = 2 + i * 20;
            TabImageButtonElement btn = getTabButton(btnX, btnY, tabI);
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
        return menu;
    }

    public TabImageButtonElement getTabButton(int x, int y, int tabI) {
        return new TabImageButtonElement(this, x, y, 22, 18, tabI, CIcons.nop(), CIcons.nop()){
            @Override
            public void onClicked(MouseButton button) {
                selectTab(tabI);
            }

        };
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


    protected void addTabContent(ITabContent content){
        tabContents.add(content);
    }

    public interface ITabContent {
        void renderTabContent(UILayer layer);
    }


}

