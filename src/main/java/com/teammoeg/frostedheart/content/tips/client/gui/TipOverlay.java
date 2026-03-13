package com.teammoeg.frostedheart.content.tips.client.gui;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.cui.base.PrimaryLayer;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.math.Rect;
import com.teammoeg.frostedheart.content.tips.Tip;
import lombok.Getter;
import net.minecraftforge.common.util.Size2i;

import java.util.ArrayList;
import java.util.List;

public class TipOverlay extends PrimaryLayer {
    public static final TipOverlay INSTANCE = new TipOverlay();
    @Getter
    protected static final List<Tip> QUEUE = new ArrayList<>();
    @Getter
    final TipLayer tipLayer;

    public static void add(Tip tip) {
        if (!QUEUE.contains(tip)) {
            QUEUE.add(tip);
        }
    }

    public static Tip getCurrent() {
        return INSTANCE.tipLayer.getTip();
    }

    /**
     * 切换下一个
     */
    public static void nextTip() {
        QUEUE.remove(INSTANCE.tipLayer.lastTip);
        INSTANCE.tipLayer.lastTip = null;
        if (!QUEUE.isEmpty()) {
            INSTANCE.tipLayer.setTip(QUEUE.get(0));
        }
    }

    public static void removeCurrent() {
        INSTANCE.tipLayer.state = TipLayer.State.FADING_OUT;
    }

    @Override
    public void refresh() {
        addUIElements();
        recalcContentSize();
        for (UIElement element : elements) {
            element.refresh();
        }
        alignWidgets();
    }

    private TipOverlay() {
        var theme = new TipTheme();
        setTheme(theme);
        this.tipLayer = new TipLayer(this);
        theme.tipLayer = this.tipLayer;
        tipLayer.setPosAndSize(0, 0, 100, 200);
        addUIElements();
    }

    Size2i lastScreenSize;
    @Override
    public void tick() {
        super.tick();
        var newSize = new Size2i(ClientUtils.screenWidth(), ClientUtils.screenHeight());
        if (!newSize.equals(lastScreenSize)) {
            refresh();
        }
        lastScreenSize = newSize;

        if (tipLayer.state == TipLayer.State.IDLE && !QUEUE.isEmpty()) {
            nextTip();
        }
    }

    @Override
    public void addUIElements() {
        clearElement();
        super.addUIElements();
        add(tipLayer);
    }

    @Override
    public void getTooltip(TooltipBuilder list) {
        super.getTooltip(list);
        list.translateZ(800);
    }

    @Override
    public boolean shouldRenderGradient() {
        return false;
    }

    @Override
    public Rect getBounds() {
        return tipLayer.getBounds();
    }
}
