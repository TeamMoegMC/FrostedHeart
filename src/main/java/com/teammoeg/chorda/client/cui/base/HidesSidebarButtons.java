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

package com.teammoeg.chorda.client.cui.base;

/**
 * 铺满窗口的主层：打开时隐藏其他模组画在屏幕边上的侧栏按钮（例如 FTB Library 的侧栏），提示和右上角的弹窗
 * （研究完成、进度）也先暂停、不显示，关上后再出，免得压在界面上。
 * <p>
 * A primary layer that fills the window: while it is open, other mods' sidebar buttons drawn at the screen's edge
 * (such as FTB Library's sidebar) are hidden, and tips and the corner toasts (research done, advancements) wait unseen
 * until it closes, so nothing sits on top of it.
 */
public interface HidesSidebarButtons {
    /** Whether the screen open now is a CUI screen whose primary layer fills the window. */
    static boolean isOpen() {
        return net.minecraft.client.Minecraft.getInstance().screen instanceof com.teammoeg.chorda.client.cui.screenadapter.CUIScreen cui
                && cui.getPrimaryLayer() instanceof HidesSidebarButtons;
    }
}
