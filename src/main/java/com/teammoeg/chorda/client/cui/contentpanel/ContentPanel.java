package com.teammoeg.chorda.client.cui.contentpanel;

import com.teammoeg.chorda.client.cui.LayerScrollBar;
import com.teammoeg.chorda.client.cui.ScrollBar;
import com.teammoeg.chorda.client.cui.UIElement;
import com.teammoeg.chorda.client.cui.UILayer;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.client.ui.Colors;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ContentPanel extends UILayer {
    public ScrollBar scrollBar;
    protected List<Line<?>> lines = new ArrayList<>();

    public ContentPanel(UIElement parent) {
        super(parent);
        this.scrollBar = new LayerScrollBar(parent, true, this);
        resize();
    }

    @Override
    public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h) {
        int border = 8;
        graphics.fill(x-border, y-border, x+w+border*2, y+h+border, 0xFF444651);
        CGuiHelper.drawBox(graphics, x-border, y-border, w+border*3, h+border*2, Colors.L_BG_GRAY, true);
    }

    public void fillContent(Collection<? extends UIElement> widgets) {
        clearElement();
        for (UIElement widget : widgets) {
            if (widget instanceof Line<?> line) {
                this.lines.add(line);
            }
            add(widget);
        }
        refresh();
    }

    public void addLine(Line<?> line) {
        this.lines.add(line);
        add(line);
        refresh();
    }

    public void addLines(Collection<? extends Line<?>> lines) {
        this.lines.addAll(lines);
        lines.forEach(this::add);
        refresh();
    }

    @Override
    public void refresh() {
        resize();
        recalcContentSize();
        for (UIElement element : elements) {
            element.refresh();
        }
        alignWidgets();
        scrollBar.setValue(0);
    }

    public void resize() {
        scrollBar.setPosAndSize(getX() + getWidth()+9, -7, 6, getHeight()+14);
    }

    @Override
    public void alignWidgets() {
        align(4, false);
    }

    @Override
    public void addUIElements() {}
}
