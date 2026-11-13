package com.teammoeg.frostedresearch.gui.archive;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.frostedresearch.gui.TechIcons;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.locale.Language;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.IntConsumer;

/** Shared presentation for the legacy workspace and datapack project details. */
public final class ResearchProjectFrame extends UIElement {
    public static final int HEADER_HEIGHT = 35;
    public static final int TAB_HEIGHT = 20;
    public static final int CONTENT_TOP = HEADER_HEIGHT + TAB_HEIGHT;
    private final Component title, status;
    private final List<Component> tabs;
    private final int selected;
    private final IntConsumer selectTab;
    private final Runnable close;
    private final CIcons.CIcon icon = CIcons.getIcon(Items.WRITABLE_BOOK);

    public ResearchProjectFrame(UIElement parent, Component title, Component status, List<Component> tabs,
                                int selected, IntConsumer selectTab, Runnable close) {
        super(parent);
        this.title = title;
        this.status = status;
        this.tabs = List.copyOf(tabs);
        this.selected = selected;
        this.selectTab = selectTab;
        this.close = close;
    }

    public static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        TechIcons.DIALOG.draw(graphics, x, y, width, height);
    }

    public static void drawTab(GuiGraphics graphics, Font font, Component title,
                               int x, int y, int width, boolean selected) {
        graphics.fill(x, y, x + width, y + TAB_HEIGHT,
                selected ? ResearchArchiveLayer.COLOR_TEAL : ResearchArchiveLayer.COLOR_PAPER_DARK);
        var fitted = font.substrByWidth(title, Math.max(1, width - 8));
        graphics.drawString(font, Language.getInstance().getVisualOrder(fitted), x + (width - font.width(fitted)) / 2, y + 6,
                selected ? 0xFFF8F1DE : ResearchArchiveLayer.COLOR_INK, false);
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height, RenderingHint hint) {
        drawPanel(graphics, x, y, width, height);
        icon.draw(graphics, x + 9, y + 9, 18, 18);
        graphics.drawString(getFont(), Language.getInstance().getVisualOrder(getFont().substrByWidth(title, width - 70)), x + 33, y + 9,
                ResearchArchiveLayer.COLOR_INK, false);
        graphics.drawString(getFont(), Language.getInstance().getVisualOrder(getFont().substrByWidth(status, width - 70)), x + 33, y + 20,
                ResearchArchiveLayer.COLOR_MUTED_INK, false);
        graphics.fill(x + width - 26, y + 8, x + width - 7, y + 27, ResearchArchiveLayer.COLOR_PAPER_DARK);
        graphics.drawString(getFont(), "x", x + width - 19, y + 14, ResearchArchiveLayer.COLOR_INK, false);
        for (int i = 0; i < tabs.size(); i++) {
            int left = i * width / tabs.size(), right = (i + 1) * width / tabs.size();
            drawTab(graphics, getFont(), tabs.get(i), x + left, y + HEADER_HEIGHT, right - left, i == selected);
        }
        graphics.fill(x + 5, y + height - 55, x + width - 5, y + height - 6, ResearchArchiveLayer.COLOR_PANEL_DARK);
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        if (!isMouseOver() || button != MouseButton.LEFT) return false;
        int x = (int) getMouseX(), y = (int) getMouseY();
        if (y >= 8 && y <= 27 && x >= getWidth() - 26 && x <= getWidth() - 7) {
            close.run();
            return true;
        }
        if (y >= HEADER_HEIGHT && y < CONTENT_TOP && !tabs.isEmpty()) {
            selectTab.accept(Math.min(tabs.size() - 1, x * tabs.size() / Math.max(1, getWidth())));
            return true;
        }
        return false;
    }
}
