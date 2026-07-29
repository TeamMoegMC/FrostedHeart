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

import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.icon.CIcons;
import net.minecraft.network.chat.Component;

/**
 * 镇长印章界面的页签定义。职责与
 * {@link com.teammoeg.frostedheart.content.town.tabs.AbstractTownTab} 相同，
 * 但不绑定容器菜单——镇长印章是物品打开的无菜单客户端界面。
 * <p>
 * Tab definition for the Mayor's Seal screen. Mirrors the role of
 * AbstractTownTab, but without a container menu binding, since the seal opens
 * a client-only screen from an item.
 */
public abstract class TownManagerTab {

    protected final TownManagerScreen screen;

    protected TownManagerTab(TownManagerScreen screen) {
        this.screen = screen;
    }

    /**
     * 页签按钮上绘制的 16x16 内容图标。
     * <p>
     * The 16x16 content icon drawn on the tab button.
     *
     * @return 内容图标 / content icon
     */
    public abstract CIcons.CIcon getContentIcon();

    /**
     * 页签标题，悬停按钮时作为提示显示。
     * <p>
     * Tab title, shown as tooltip when hovering the tab button.
     *
     * @return 标题组件 / title component
     */
    public abstract Component getTitle();

    /**
     * 构建页签内容并加入内容层。内容层坐标系与屏幕框架一致，
     * 可用区域见 {@link TownManagerScreen#CONTENT_X} 等常量。
     * <p>
     * Builds tab content into the content layer. The layer shares the frame's
     * coordinate system; see the CONTENT_* constants on TownManagerScreen.
     *
     * @param layer 内容层 / content layer
     */
    public abstract void build(UILayer layer);
}
