/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.client.ui;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.frostedresearch.knowledge.client.KnowledgeGraphModel;
import com.teammoeg.frostedresearch.knowledge.client.KnowledgeJournalData;
import com.teammoeg.frostedresearch.knowledge.client.KnowledgePresentation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.teammoeg.frostedresearch.knowledge.client.KnowledgePresentation.t;
import static com.teammoeg.frostedresearch.knowledge.client.ui.KnowledgeUiTheme.*;

/**
 * A second paper above the graph; reading never changes the underlying camera or page.
 */
public final class KnowledgeNodeSheet extends UILayer {
    private final Runnable close;
    private final JournalText text;
    private final JournalRelation relation;
    private KnowledgeJournalData data;
    private KnowledgeGraphModel.Node node;
    private boolean details;
    private FormattedCharSequence heading = FormattedCharSequence.EMPTY;

    public KnowledgeNodeSheet(UIElement parent, Runnable close, Consumer<String> open) {
        super(parent);
        this.close = close;
        text = new JournalText(this);
        relation = new JournalRelation(this, key -> open.accept(KnowledgeGraphModel.knowledgeId(key)));
        setScissorEnabled(false);
        setZIndex(400);
        setVisible(false);
        setEnabled(false);
    }

    public String nodeId() {
        return node == null ? "" : node.id();
    }

    public void show(KnowledgeJournalData data, KnowledgeGraphModel.Node node) {
        if (this.node == null || !this.node.id().equals(node.id())) details = false;
        this.data = data;
        this.node = node;
        setVisible(true);
        setEnabled(true);
        refresh();
    }

    @Override
    public void addUIElements() {
        if (node == null) return;
        int width = getWidth(), height = getHeight();
        heading = Language.getInstance().getVisualOrder(getFont().substrByWidth(node.title(), width - 70));
        JournalButton dismiss = new JournalButton(this, t("graph.close"), true, close);
        dismiss.setPosAndSize(width - 37, 11, 23, 18);
        add(dismiss);

        if (node.kind() == KnowledgeGraphModel.Kind.LINK && node.link() != null) {
            relation.setPosAndSize(21, 65, width - 42, height - 82);
            relation.relation(data, node.link());
            add(relation);
            return;
        }

        List<Component> paragraphs = new ArrayList<>();
        var entry = node.entry();
        if (entry != null) {
            if (!entry.body().getString().isBlank()) paragraphs.add(entry.body());
            paragraphs.addAll(entry.summary());
            if (details) {
                paragraphs.addAll(entry.details());
                paragraphs.add(KnowledgePresentation.source(entry.source()));
            }
            if (!entry.active()) paragraphs.add(KnowledgePresentation.status(entry.status()));
            JournalButton expand = new JournalButton(this, t(details ? "details.hide" : "details.show"), true,
                    () -> {
                        details = !details;
                        refresh();
                    });
            expand.setPosAndSize(21, height - 31, width - 42, 18);
            add(expand);
        } else if (node.kind() == KnowledgeGraphModel.Kind.PROJECT) {
            paragraphs.add(t(node.completed() ? "graph.research_done" : "graph.research_waiting"));
            if (node.project() != null) {
                paragraphs.add(t("graph.related_ideas"));
                for (var idea : node.project().ideas()) paragraphs.add(data.title(idea));
                paragraphs.add(t("graph.expected_results"));
                paragraphs.addAll(node.project().outputTitles());
            } else if (node.link() != null) {
                paragraphs.add(t("graph.expected_results"));
                for (var result : node.link().outputs()) paragraphs.add(data.title(result));
            }
        } else {
            paragraphs.add(t("graph.old_record"));
        }
        text.setPosAndSize(23, 64, width - 46, height - (entry == null ? 82 : 104));
        text.text(paragraphs);
        add(text);
    }

    @Override
    public void alignWidgets() {
    }

    @Override
    public void drawBackground(GuiGraphics g, int x, int y, int width, int height, RenderingHint hint) {
        g.fill(x - getX(), y - getY(), x - getX() + parent.getWidth(), y - getY() + parent.getHeight(), 0xA0111713);
        drawPaper(g, x, y, width, height);
        if (node == null) return;
        Component kind = node.kind() == KnowledgeGraphModel.Kind.PROJECT ? t("graph.project")
                : node.kind() == KnowledgeGraphModel.Kind.LINK ? t("graph.connection") : KnowledgePresentation.kind(node.kind().name());
        g.drawString(getFont(), kind, x + 23, y + 15, MUTED, false);
        g.renderItem(node.icon(), x + 23, y + 33);
        g.drawString(getFont(), heading, x + 46, y + 36, INK, false);
        rule(g, x + 23, y + 57, width - 46);
    }
}
