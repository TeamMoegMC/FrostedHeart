/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.client.ui;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.*;
import com.teammoeg.frostedresearch.knowledge.client.*;
import com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.util.FormattedCharSequence;

import java.util.*;
import java.util.function.Consumer;

/**
 * Local relation drawing. Nodes fit the reading page; no force layout or all-archive graph pass.
 */
public final class JournalRelation extends UIElement {
    private record Node(KnowledgeKey key, int x, int y, int w, int h, FormattedCharSequence text) {
    }

    public record Geometry(int columns, int cardWidth, int cardHeight, int rows, int jointY, int contentHeight) {
        public static Geometry fit(int width, int height, int inputs, int outputs) {
            int columns = inputs > 2 && width >= 210 ? 3 : Math.max(1, Math.min(2, inputs));
            int rows = (inputs + columns - 1) / columns;
            int cardHeight = Math.max(18, Math.min(26, (height - 16 - Math.max(0, rows + outputs - 1) * 4) / Math.max(1, rows + outputs)));
            int jointY = rows * (cardHeight + 4) + 2;
            return new Geometry(columns, (width - (columns - 1) * 7) / columns, cardHeight, rows, jointY, jointY + 14 + outputs * (cardHeight + 4));
        }
    }

    private KnowledgeJournalData data;
    private KnowledgeJournalData.Relation relation;
    private final List<Node> nodes = new ArrayList<>();
    private final Consumer<KnowledgeKey> open;
    private int layoutWidth = -1, layoutHeight = -1, scroll, contentHeight;
    private Language language;
    private int jointY;

    public JournalRelation(UIElement parent, Consumer<KnowledgeKey> open) {
        super(parent);
        this.open = open;
    }

    public void relation(KnowledgeJournalData data, KnowledgeJournalData.Relation relation) {
        this.data = data;
        this.relation = relation;
        layoutWidth = -1;
    }

    private void layout() {
        if (relation == null || layoutWidth == getWidth() && layoutHeight == getHeight() && language == Language.getInstance())
            return;
        layoutWidth = getWidth();
        layoutHeight = getHeight();
        language = Language.getInstance();
        nodes.clear();
        Geometry geometry = Geometry.fit(getWidth() - 4, getHeight(), relation.inputs().size(), relation.outputs().size());
        int columns = geometry.columns(), card = geometry.cardWidth(), cardHeight = geometry.cardHeight();
        for (int i = 0; i < relation.inputs().size(); i++)
            add(relation.inputs().get(i), (i % columns) * (card + 7), (i / columns) * (cardHeight + 4), card, cardHeight);
        jointY = geometry.jointY();
        int y = jointY + 14;
        for (KnowledgeKey key : relation.outputs()) {
            add(key, Math.max(0, (getWidth() - Math.min(150, getWidth() - 4)) / 2), y, Math.min(150, getWidth() - 4), cardHeight);
            y += cardHeight + 4;
        }
        contentHeight = geometry.contentHeight();
        scroll = Math.min(scroll, Math.max(0, contentHeight - getHeight()));
    }

    private void add(KnowledgeKey key, int x, int y, int w, int h) {
        nodes.add(new Node(key, x, y, w, h, Language.getInstance().getVisualOrder(getFont().substrByWidth(data.title(key), Math.max(1, w - 12)))));
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        if (!isMouseOver() || button != MouseButton.LEFT) return false;
        layout();
        for (Node node : nodes)
            if (getMouseX() >= node.x && getMouseX() < node.x + node.w && getMouseY() + scroll >= node.y && getMouseY() + scroll < node.y + node.h) {
                if (data.entry(node.key) != null) open.accept(node.key);
                return true;
            }
        return true;
    }

    @Override
    public void getTooltip(TooltipBuilder tooltip) {
        for (Node node : nodes)
            if (getMouseX() >= node.x && getMouseX() < node.x + node.w && getMouseY() + scroll >= node.y && getMouseY() + scroll < node.y + node.h)
                tooltip.accept(data.title(node.key));
    }

    @Override
    public void render(GuiGraphics g, int x, int y, int w, int h, RenderingHint hint) {
        if (relation == null) return;
        layout();
        g.enableScissor(x, y, x + w, y + h);
        y -= scroll;
        int center = x + w / 2;
        for (int i = 0; i < relation.inputs().size(); i++) {
            Node node = nodes.get(i);
            int nx = x + node.x + node.w / 2, ny = y + node.y + node.h;
            g.fill(nx, ny, nx + 1, y + jointY + 1, KnowledgeUiTheme.GOLD);
            g.fill(Math.min(nx, center), y + jointY, Math.max(nx, center) + 1, y + jointY + 1, KnowledgeUiTheme.GOLD);
        }
        g.fill(center - 2, y + jointY - 2, center + 3, y + jointY + 3, KnowledgeUiTheme.ACCENT);
        g.fill(center, y + jointY + 3, center + 1, y + Math.max(jointY + 14, contentHeight - 26), KnowledgeUiTheme.GOLD);
        for (Node node : nodes) {
            KnowledgeUiTheme.panel(g, x + node.x, y + node.y, node.w, node.h);
            g.drawString(getFont(), node.text, x + node.x + 6, y + node.y + (node.h - 9) / 2, KnowledgeUiTheme.INK, false);
        }
        g.disableScissor();
        if (contentHeight > h) {
            int thumb = Math.max(8, h * h / contentHeight), top = (h - thumb) * scroll / (contentHeight - h);
            g.fill(x + w - 2, y + scroll + top, x + w - 1, y + scroll + top + thumb, KnowledgeUiTheme.MUTED);
        }
    }

    @Override
    public boolean onMouseScrolled(double delta) {
        if (!isMouseOver()) return false;
        layout();
        scroll = net.minecraft.util.Mth.clamp(scroll - (int) Math.signum(delta) * 24, 0, Math.max(0, contentHeight - getHeight()));
        return true;
    }
}
