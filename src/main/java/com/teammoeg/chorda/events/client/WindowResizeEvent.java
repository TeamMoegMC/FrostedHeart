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
