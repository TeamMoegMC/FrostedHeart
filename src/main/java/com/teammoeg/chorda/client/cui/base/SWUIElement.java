package com.teammoeg.chorda.client.cui.base;

import com.teammoeg.chorda.client.RenderingHint;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 自带平滑移动的 {@link UIElement}
 */
@Getter
public abstract class SWUIElement extends UIElement {
    private float displayX = 0, displayY = 0;
    private long lastFrameTime = -1;
    @Setter
    protected float smoothStep = 0.05F;

    public SWUIElement(UIElement parent) {
        super(parent);
    }

    @Override
    public final void render(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
        try {
            updateDisplayPos(x ,y);
            graphics.pose().pushPose();
            graphics.pose().translate(displayX -(int) displayX, displayY -(int) displayY, 0);
            doRender(graphics, (int) displayX, (int) displayY, w, h, hint);
        } finally {
            graphics.pose().popPose();
            lastFrameTime = Util.getNanos();
        }
    }

    public abstract void doRender(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint);

    public void updateDisplayPos(int x, int y) {
        long now = Util.getNanos();
        float delta = (now - lastFrameTime) / 1_000_000_000.0f;
        delta = Math.min(delta, 0.1F);

        float f = 1.0f - (float)Math.exp(-delta / smoothStep);
        displayX += (x - displayX) * f;
        displayY += (y - displayY) * f;
    }


    @Override
    public void setPos(int x, int y) {
        if (isFirstFrame()) {
            forceSetPos(x, y);
        } else {
            super.setPos(x, y);
        }
    }

    /**
     * 没有平滑移动的 {@link #setPos(int, int)}
     */
    public void forceSetPos(int x, int y) {
        super.setPos(x, y);
        displayX = x;
        displayY = y;
    }

    /**
     * 此类覆写了 {@link #setPos(int, int)} 方法，
     * 第一帧会同时设置 {@code displayXY} 防止刚打开UI时元素从 0, 0 漂移到修改的坐标。
     * 如果你确实想要这个效果请使用这个方法。
     */
    public void superSetPos(int x, int y) {
        super.setPos(x, y);
    }

    /**
     * @return 元素是否是第一次被渲染
     */
    public boolean isFirstFrame() {
        return lastFrameTime == -1;
    }
}
