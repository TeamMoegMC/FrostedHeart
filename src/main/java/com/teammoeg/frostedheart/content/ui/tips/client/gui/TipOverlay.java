package com.teammoeg.frostedheart.content.ui.tips.client.gui;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.cui.base.PrimaryLayer;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.editor.EditorFieldsDialog;
import com.teammoeg.chorda.client.cui.screenadapter.CUIScreenWrapper;
import com.teammoeg.chorda.math.Rect;
import com.teammoeg.frostedheart.content.ui.tips.Tip;
import com.teammoeg.frostedheart.content.ui.tips.TipQueueModel;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class TipOverlay extends PrimaryLayer {
    public static final TipOverlay INSTANCE = new TipOverlay();
    @Getter
    protected static final List<Tip> QUEUE = new ArrayList<>();
    @Getter
    final TipLayer tipLayer;

    public static void add(Tip tip) {
        replaceQueue(TipQueueModel.enqueue(QUEUE, tip, Tip::id));
    }

    public static void replaceQueue(List<Tip> tips) {
        QUEUE.clear();
        QUEUE.addAll(tips);
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

    private static boolean isTownEventTip(Tip tip) {
        return tip != null && tip.id().startsWith("/town/events/");
    }

    @Override
    public void refresh() {
        addUIElements();
        recalcContentSize();
        for (UIElement element : elements) {
            element.refresh();
        }
        alignWidgets();
        setSizeToContentSize();
    }

    private TipOverlay() {
        var theme = new TipTheme();
        setTheme(theme);
        this.tipLayer = new TipLayer(this);
        theme.tipLayer = this.tipLayer;
        tipLayer.setPosAndSize(0, 0, 100, 200);
        addUIElements();
    }

    @Override
    public void tick() {
        if (!FHConfig.CLIENT.enableTip.get()) {
            QUEUE.clear();
            tipLayer.clearImmediately();
            return;
        }
        if (!FHConfig.CLIENT.enableTownEventTips.get()) {
            boolean currentIsTownEvent = isTownEventTip(tipLayer.getTip());
            QUEUE.removeIf(TipOverlay::isTownEventTip);
            if (currentIsTownEvent) tipLayer.clearImmediately();
        }
        super.tick();
        if (ClientUtils.getMc().screen instanceof CUIScreenWrapper cui) {
            if (cui.getPrimaryLayer().getDialog() instanceof EditorFieldsDialog<?> efd) {
                if (efd.getValue() instanceof Tip tip) {
                    if (!tipLayer.tip.equals(tip)) {
                        tipLayer.tip = tip;
                        tipLayer.display = tip.display();
                        tipLayer.state = TipLayer.State.DONE;
                        refresh();
                    }
                    tipLayer.setEditing(true);
                }
            }
        } else {
            tipLayer.setEditing(false);
        }

        if (tipLayer.state == TipLayer.State.IDLE && !QUEUE.isEmpty()) {
            nextTip();
        }
    }

    @Override
    public void addChildUIElements() {
        clearElement();
        add(tipLayer);
    }

    @Override
    public void getTooltip(TooltipBuilder list) {
        super.getTooltip(list);
        list.translateZ(800);
    }

    @Override
    public boolean isVisible() {
        return isEnabled();
    }

    @Override
    public boolean isEnabled() {
        return tipLayer.isEnabled();
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
