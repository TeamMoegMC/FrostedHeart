/*
 * Copyright (c) 2026 TeamMoeg
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

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MenuPrimaryLayer;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.cui.widgets.TabImageButtonElement;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.menu.CBlockEntityMenu;
import com.teammoeg.frostedheart.content.town.block.AbstractTownBuildingBlockEntity;
import com.teammoeg.frostedheart.content.town.event.ITownDataUpdateListener;
import com.teammoeg.frostedheart.content.town.tabs.AbstractTownTab;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractTownWorkerBlockScreen<C extends CBlockEntityMenu<? extends AbstractTownBuildingBlockEntity>> extends MenuPrimaryLayer<C> implements ITownDataUpdateListener {


    private int activeTab = 0;
    private final List<AbstractTownTab<C>> tabs = new ArrayList<>();
    private final List<TabImageButtonElement> tabButtons = new ArrayList<>();
    private UILayer contentLayer;

    public AbstractTownWorkerBlockScreen(C inventorySlotsIn) {
        super(inventorySlotsIn);

        //初始化内容容器
        if (this.contentLayer == null) {
            this.contentLayer = new UILayer(this) {
                @Override public void addUIElements() {
                    if (activeTab >= 0 && activeTab < tabs.size()) {
                        tabs.get(activeTab).build(this);
                    }
                }
                @Override public void alignWidgets() {}
            };
        }
        initTabs();
    }

    @Override
    public void addChildUIElements() {
        this.contentLayer.setSize(176, 222);
        this.add(this.contentLayer);


        //初始化 Tab 按钮
        this.tabButtons.clear();
        for (int i = 0; i < tabs.size(); i++) {
            final int tabI = i;
            AbstractTownTab<C> tab = tabs.get(i);
            int btnX = -22;
            int btnY = 2 + i * 20;
            TabImageButtonElement btn = getTabButton(
                    btnX,
                    btnY,
                    tabI,
                    tab.getIcon(),
                    tab.getActiveIcon(),
                    tab.getContentIcon(),
                    tab.getTitle());
            btn.bind(() -> activeTab);
            this.tabButtons.add(btn);
            this.add(btn); // 添加到主 Screen
        }


    }
    public void selectTab(int index) {
        if (index < 0 || index >= tabs.size()) return;
        if (this.activeTab == index) return;
        this.activeTab = index;

        updateTabContent();
    }

    protected void updateTabContent() {
        if (contentLayer != null) {
            contentLayer.refresh();
        }
    }

    public C getCBEMenu() {
        return menu;
    }

    public TabImageButtonElement getTabButton(
            int x,
            int y,
            int tabI,
            CIcons.CIcon inactiveBackground,
            CIcons.CIcon activeBackground,
            CIcons.CIcon contentIcon,
            Component title
    ) {
        TabImageButtonElement button = new TabImageButtonElement(
                this,
                x,
                y,
                22,
                18,
                tabI,
                activeBackground,
                inactiveBackground
        ) {
            @Override
            public void render(
                    GuiGraphics graphics,
                    int renderX,
                    int renderY,
                    int width,
                    int height,
                    RenderingHint hint
            ) {
                super.render(graphics, renderX, renderY, width, height, hint);
                contentIcon.draw(graphics, renderX + 3, renderY + 1, 16, 16);
            }

            @Override
            public void onClicked(MouseButton button) {
                selectTab(tabI);
            }

        };
        button.setTitle(title);
        return button;
    }
    @Override
    public boolean onInit() {
        int sw = 176;
        int sh = 222;
        this.setSize(sw, sh);
        // 打开界面时注册为城镇数据监听器，增量/全量同步到达即刷新本界面。
        TeamTownData.addClientListener(this);
        return super.onInit();
    }

    @Override
    public void onClosed() {
        // 关闭界面时移除监听器，避免悬挂引用与无谓刷新。
        TeamTownData.removeClientListener(this);
        super.onClosed();
    }

    /**
     * 城镇数据变化时无需重建内容：所有面板（BuildingInfoElement / TownInfoPanel /
     * VirtualItemGridElement / TownWorkforcePanel 等）在 render() 阶段通过 Supplier
     * 从客户端城镇快照实时取值，每帧都会渲染最新数据。若在此处调用
     * {@code contentLayer.refresh()}，clearElement() 会销毁全部子元素并以默认状态
     * 重建，导致滚动位置、选中的建筑/居民等瞬时 UI 状态被重置（例如滚动条回到
     * 顶端、建筑详情切回第一座建筑）。
     * <p>
     * No rebuild is needed when town data changes: every panel reads the current
     * client snapshot via a Supplier during render(), so content stays fresh each
     * frame. Calling refresh() here would clearElement() and rebuild all children
     * with default state, resetting transient UI state such as scroll offsets and
     * the selected building/resident.
     */
    @Override
    public void onBuildingsChanged() {
    }

    @Override
    public void onResidentsChanged() {
    }

    @Override
    public void onResourcesChanged() {
    }

    protected void initTabs() {}

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

    protected void addTab(AbstractTownTab<C> tab){
        tabs.add(tab);
    }

}
