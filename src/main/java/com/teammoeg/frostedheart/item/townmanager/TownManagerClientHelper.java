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

import com.teammoeg.chorda.client.cui.screenadapter.CUIScreenWrapper;
import com.teammoeg.frostedheart.content.town.resource.VirtualResourceType;

/**
 * 镇长印章界面的客户端打开入口。界面无容器菜单，
 * 通过 {@link CUIScreenWrapper} 将 CUI 主层包装为原版 Screen 打开。
 * <p>
 * Client-side entry point for the Mayor's Seal screen. The screen has no
 * container menu, so the CUI primary layer is wrapped into a vanilla Screen
 * via CUIScreenWrapper.
 */
public class TownManagerClientHelper {

    public static void openScreen() {
        CUIScreenWrapper.open(new TownManagerScreen());
    }

    public static void openEvents() {
        CUIScreenWrapper.open(new TownManagerScreen(TownManagerScreen.EVENTS_TAB));
    }

    public static void openTransportCapacity() {
        CUIScreenWrapper.open(new TownManagerScreen(
                TownManagerScreen.VIRTUAL_RESOURCES_TAB,
                VirtualResourceType.TRANSPORT_CAPACITY));
    }
}
