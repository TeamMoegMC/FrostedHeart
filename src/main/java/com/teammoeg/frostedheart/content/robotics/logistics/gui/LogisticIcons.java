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

package com.teammoeg.frostedheart.content.robotics.logistics.gui;

import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CTextureIcon;
import com.teammoeg.frostedheart.util.client.FHClientUtils;

public class LogisticIcons {
    public static final CTextureIcon BOT_DOCK=CIcons.getIcon(FHClientUtils.makeGuiTextureLocation("bot_dock"));
    public static final CTextureIcon DOCK_FILTER=CIcons.getIcon(FHClientUtils.makeGuiTextureLocation("bot_dock_filter"));
    public static final CTextureIcon INV_STATUS=BOT_DOCK.withUV(0, 0, 176, 25, 256, 256);
    public static final CTextureIcon INV_CHEST=BOT_DOCK.withUV(0, 25, 176, 59, 256, 256);
    public static final CTextureIcon INV_FILTER=BOT_DOCK.withUV(0, 84, 176, 31, 256, 256);
    public static final CTextureIcon INV_BACK=BOT_DOCK.withUV(0, 115, 176, 84, 256, 256);
    public static final CTextureIcon STATUS_GREEN=BOT_DOCK.withUV(176, 0, 10, 10, 256, 256);
    public static final CTextureIcon STATUS_BLUE=BOT_DOCK.withUV(186, 0, 10, 10, 256, 256);
    public static final CTextureIcon STATUS_YELLOW=BOT_DOCK.withUV(196, 0, 10, 10, 256, 256);
    public static final CTextureIcon STATUS_RED=BOT_DOCK.withUV(206, 0, 10, 10, 256, 256);
    public static final CTextureIcon[] STATUS_LIGHTS=new CTextureIcon[] {STATUS_RED,STATUS_YELLOW,STATUS_GREEN,STATUS_BLUE};
    
    public static final CTextureIcon FILTER_BACK=DOCK_FILTER.withUV(0, 0, 108, 50, 256, 256);
    public static final CTextureIcon BUTTON_BACK_ON=DOCK_FILTER.withUV(108, 0, 12, 12, 256, 256);
    public static final CTextureIcon BUTTON_CHECK=DOCK_FILTER.withUV(108, 13, 8, 8, 256, 256);
	public LogisticIcons() {
	}

}
