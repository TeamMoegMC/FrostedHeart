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

package com.teammoeg.chorda.events.client;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraftforge.eventbus.api.Event;

/**
 * 窗口大小改变事件，在 {@link Minecraft#resizeDisplay} 末尾触发。
 */
@Getter
public class WindowResizeEvent extends Event {

    private final Minecraft minecraft;
    private final int width;
    private final int height;

    public WindowResizeEvent(Minecraft minecraft, int width, int height) {
        this.minecraft = minecraft;
        this.width = width;
        this.height = height;
    }
}
