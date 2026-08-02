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

package com.teammoeg.frostedheart.item.townmanager;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.PrimaryLayer;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.cui.widgets.TabImageButtonElement;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.dataholders.team.CClientTeamDataManager;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.TeamTownData;
import com.teammoeg.frostedheart.content.town.event.ITownDataUpdateListener;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 镇长印章的城镇管理界面。基于 Chorda CUI 框架（{@link PrimaryLayer}）实现，
 * 外观与城镇建筑方块界面一致：townworkerblock.png 176x222 框架加左侧页签。
 * 界面为纯客户端只读视图，数据来自每 tick 同步的客户端城镇快照。
 * 由 {@link TownManagerClientHelper#openScreen()} 通过 CUIScreenWrapper 打开。
 * <p>
 * Town management screen of the Mayor's Seal. Built on the Chorda CUI
 * framework (PrimaryLayer) and visually consistent with town building GUIs:
 * the 176x222 townworkerblock frame with left-side tabs. It is a client-only
 * read-only view backed by the town snapshot synced from the server.
 */
public class TownManagerScreen extends PrimaryLayer implements ITownDataUpdateListener {

    private static final CIcons.CTextureIcon ALL = CIcons
            .getIcon(new ResourceLocation(FHMain.MODID, "textures/gui/town_manage_screen.png"));
    private static final CIcons.CTextureIcon BACKGROUND =
            ALL.withUV(0, 0, 176, 222, 256, 256);
    private static final CIcons.CTextureIcon ACTIVE_TAB =
            ALL.withUV(180, 59, 22, 18, 256, 256);
    private static final CIcons.CTextureIcon INACTIVE_TAB =
            ALL.withUV(202, 59, 22, 18, 256, 256);

    public static final int FRAME_WIDTH = 176;
    public static final int FRAME_HEIGHT = 222;
    /**
     * 内容区左上角与尺寸。本界面没有玩家背包，内容可使用整个框架内边距。
     * <p>
     * Content area origin and size. This screen has no player inventory, so
     * the content may occupy the whole inner frame.
     */
    public static final int CONTENT_X = 8;
    public static final int CONTENT_Y = 6;
    public static final int CONTENT_WIDTH = 160;
    public static final int CONTENT_HEIGHT = 204;

    private int activeTab = 0;
    private final List<TownManagerTab> tabs = new ArrayList<>();
    private final UILayer contentLayer;

    public TownManagerScreen() {
        super();
        tabs.add(new TownOverviewTab(this));
        tabs.add(new TownResidentsTab(this));
        tabs.add(new TownBuildingsTab(this));
        tabs.add(new TownStatisticsTab(this));
        this.contentLayer = new UILayer(this) {
            @Override
            public void addUIElements() {
                if (activeTab >= 0 && activeTab < tabs.size()) {
                    tabs.get(activeTab).build(this);
                }
            }

            @Override
            public void alignWidgets() {
            }
        };
    }

    /**
     * 获取客户端缓存的城镇数据。数据由服务端定期全量同步，可能尚未到达。
     * <p>
     * Gets the client-cached town data. Synced from the server periodically;
     * may be null if no sync has arrived yet.
     *
     * @return 城镇数据，可能为 null / town data, or null
     */
    @Nullable
    public TeamTownData getTownData() {
        return CClientTeamDataManager.INSTANCE.getInstance().getData(FHSpecialDataTypes.TOWN_DATA);
    }

    /**
     * 获取以当前客户端快照为数据的城镇门面。
     * <p>
     * Gets a town facade over the current client snapshot.
     *
     * @return 城镇门面，可能为 null / town facade, or null
     */
    @Nullable
    public TeamTown getTown() {
        TeamTownData data = getTownData();
        return data == null ? null : data.createTeamTown();
    }

    /**
     * 切换到指定页签并重建内容。
     * <p>
     * Selects a tab and rebuilds the content.
     *
     * @param index 页签序号 / tab index
     */
    public void selectTab(int index) {
        if (index < 0 || index >= tabs.size() || index == activeTab) return;
        activeTab = index;
        contentLayer.refresh();
    }

    @Override
    public void addChildUIElements() {
        contentLayer.setPos(0, 0);
        contentLayer.setSize(FRAME_WIDTH, FRAME_HEIGHT);
        this.add(contentLayer);
        for (int i = 0; i < tabs.size(); i++) {
            this.add(createTabButton(i, tabs.get(i)));
        }
    }

    private TabImageButtonElement createTabButton(int index, TownManagerTab tab) {
        TabImageButtonElement button = new TabImageButtonElement(
                this, -22, 2 + index * 20, 22, 18, index, ACTIVE_TAB, INACTIVE_TAB) {
            @Override
            public void render(GuiGraphics graphics, int renderX, int renderY, int width, int height, RenderingHint hint) {
                super.render(graphics, renderX, renderY, width, height, hint);
                tab.getContentIcon().draw(graphics, renderX + 3, renderY + 1, 16, 16);
            }

            @Override
            public void onClicked(MouseButton button) {
                selectTab(index);
            }
        };
        button.setTitle(tab.getTitle());
        button.bind(() -> activeTab);
        return button;
    }

    @Override
    public boolean onInit() {
        this.setSize(FRAME_WIDTH, FRAME_HEIGHT);
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
     * 城镇数据变化时无需重建内容：所有面板（TownOverviewTab / TownResidentsPanel /
     * TownBuildingsPanel / TownStatisticsPanel）在 render() 阶段通过 Supplier 从
     * 客户端城镇快照实时取值，每帧都会渲染最新数据。若在此处调用
     * {@code contentLayer.refresh()}，clearElement() 会销毁全部子元素并以默认状态
     * 重建，导致滚动位置、选中的建筑/居民等瞬时 UI 状态被重置（例如建筑详情
     * 切回第一座建筑、名单滚动回到顶端）。
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

    @Override
    public void drawBackground(GuiGraphics graphics, int x, int y, int width, int height, RenderingHint hint) {
        BACKGROUND.draw(graphics, x, y, FRAME_WIDTH, FRAME_HEIGHT);
    }

    @Override
    public void setSizeToContentSize() {
        // 固定框架尺寸，不随内容变化 / fixed frame size, never fit to content
    }
}
