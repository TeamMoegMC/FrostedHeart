/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.gui.knowledge;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.frostedresearch.FRNetwork;
import com.teammoeg.frostedresearch.api.ClientKnowledgeDataAPI;
import com.teammoeg.frostedresearch.blocks.DrawingDeskTileEntity;
import com.teammoeg.frostedresearch.gui.drawdesk.DrawDeskScreen;
import com.teammoeg.frostedresearch.knowledge.ActionCard;
import com.teammoeg.frostedresearch.knowledge.IdeaCandidate;
import com.teammoeg.frostedresearch.knowledge.IdeaRecord;
import com.teammoeg.frostedresearch.knowledge.KnowledgeLabProjection;
import com.teammoeg.frostedresearch.knowledge.KnowledgeProjection;
import com.teammoeg.frostedresearch.knowledge.ResearchResult;
import com.teammoeg.frostedresearch.network.FHDrawingDeskOperationPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Three-page, full-window archive for every client-safe part of team knowledge. */
public final class KnowledgeLabLayer extends UILayer {
    private static final int INK = 0xFF27231D;
    private static final int MUTED = 0xFF746B5C;
    private static final int PAPER = 0xFFF1E7CE;
    private static final int PANEL = 0xFFE2D4B5;
    private static final int DETAIL = 0xFFECE0C4;
    private static final int TEAL = 0xFF3F756D;
    private static final int RED = 0xFF974A3E;
    private static final int ROW_HEIGHT = 20;
    private static final int LIST_ROWS = 14;

    private final DrawDeskScreen screen;
    private final Runnable close;
    private final KnowledgeLabButton[] pageButtons = new KnowledgeLabButton[Page.values().length];
    private final KnowledgeLabButton[] listButtons = new KnowledgeLabButton[LIST_ROWS];
    private final KnowledgeLabButton[] actionButtons = new KnowledgeLabButton[3];
    private final KnowledgeLabButton[] candidateButtons = new KnowledgeLabButton[3];
    private final KnowledgeLabButton[] resultTypeButtons = new KnowledgeLabButton[ResearchResult.ResultType.values().length];
    private final KnowledgeLabButton closeButton;
    private final KnowledgeLabButton primaryButton;
    private final KnowledgeLabButton secondaryButton;
    private final KnowledgeLabButton pinButton;

    private KnowledgeLabLayout layout = KnowledgeLabLayout.calculate(480, 270);
    private Page page = Page.OBSERVATIONS;
    private ResearchResult.ResultType resultType = ResearchResult.ResultType.FINDING;
    private int listOffset;
    private int visibleRows = 8;
    private int selectedObservation;
    private int selectedIdea;
    private int selectedResult;
    private int inspirationRequestTicks;

    public KnowledgeLabLayer(UIElement parent, DrawDeskScreen screen, Runnable close) {
        super(parent);
        this.screen = screen;
        this.close = close;
        setScissorEnabled(false);
        for (int index = 0; index < pageButtons.length; index++) {
            Page target = Page.values()[index];
            pageButtons[index] = new KnowledgeLabButton(this, pageTitle(target), ignored -> selectPage(target)).centered();
        }
        for (int index = 0; index < listButtons.length; index++) {
            int row = index;
            listButtons[index] = new KnowledgeLabButton(this, Component.empty(), ignored -> selectListRow(row));
        }
        for (int index = 0; index < actionButtons.length; index++) {
            int action = index;
            actionButtons[index] = new KnowledgeLabButton(this, Component.empty(), ignored -> executeMethod(action));
        }
        for (int index = 0; index < candidateButtons.length; index++) {
            int candidate = index;
            candidateButtons[index] = new KnowledgeLabButton(this, Component.empty(), ignored -> sendCandidate(candidate));
        }
        for (int index = 0; index < resultTypeButtons.length; index++) {
            ResearchResult.ResultType type = ResearchResult.ResultType.values()[index];
            resultTypeButtons[index] = new KnowledgeLabButton(this, resultTypeTitle(type), ignored -> selectResultType(type)).centered();
        }
        closeButton = new KnowledgeLabButton(this, Component.empty(), ignored -> closeOrReturn()).centered();
        primaryButton = new KnowledgeLabButton(this, Component.empty(), ignored -> primaryAction()).centered();
        secondaryButton = new KnowledgeLabButton(this, Component.empty(), ignored -> secondaryAction()).centered();
        pinButton = new KnowledgeLabButton(this, Component.empty(), ignored -> toggleSelectedObservation()).centered();
    }

    @Override
    public void addUIElements() {
        for (KnowledgeLabButton button : pageButtons) add(button);
        for (KnowledgeLabButton button : listButtons) add(button);
        for (KnowledgeLabButton button : actionButtons) add(button);
        for (KnowledgeLabButton button : candidateButtons) add(button);
        for (KnowledgeLabButton button : resultTypeButtons) add(button);
        add(closeButton);
        add(primaryButton);
        add(secondaryButton);
        add(pinButton);
    }

    @Override
    public void alignWidgets() {
        resizeLab(getWidth(), getHeight());
    }

    public void resizeLab(int width, int height) {
        setSize(width, height);
        layout = KnowledgeLabLayout.calculate(width, height);
        int closeWidth = Math.max(82, Math.min(116, width / 6));
        closeButton.setPosAndSize(width - closeWidth - 8, 7, closeWidth, 19);
        int tabStart = width >= 560 ? 112 : 8;
        int tabSpace = Math.max(150, width - closeWidth - tabStart - 18);
        int tabWidth = Math.max(48, Math.min(92, (tabSpace - 8) / pageButtons.length));
        for (int index = 0; index < pageButtons.length; index++) {
            pageButtons[index].setPosAndSize(tabStart + index * (tabWidth + 4), 7, tabWidth, 19);
        }

        KnowledgeLabLayout.Bounds list = layout.list();
        visibleRows = Math.max(1, Math.min(listButtons.length, (list.height() - 36) / ROW_HEIGHT));
        for (int index = 0; index < listButtons.length; index++) {
            listButtons[index].setPosAndSize(list.x() + 7, list.y() + 27 + index * ROW_HEIGHT,
                    list.width() - 14, 18);
        }

        KnowledgeLabLayout.Bounds context = layout.context();
        int actionWidth = context.width() - 14;
        pinButton.setPosAndSize(context.x() + 7, context.y() + 86, actionWidth, 20);
        primaryButton.setPosAndSize(context.x() + 7, context.bottom() - 51, actionWidth, 20);
        secondaryButton.setPosAndSize(context.x() + 7, context.bottom() - 27, actionWidth, 20);
        for (int index = 0; index < actionButtons.length; index++) {
            actionButtons[index].setPosAndSize(context.x() + 7, context.y() + 58 + index * 24, actionWidth, 20);
        }
        for (int index = 0; index < candidateButtons.length; index++) {
            candidateButtons[index].setPosAndSize(context.x() + 7, context.y() + 58 + index * 24, actionWidth, 20);
        }
        int typeWidth = Math.max(8, (context.width() - 14 - (resultTypeButtons.length - 1) * 3)
                / resultTypeButtons.length);
        for (int index = 0; index < resultTypeButtons.length; index++) {
            resultTypeButtons[index].setPosAndSize(context.x() + 7 + index * (typeWidth + 3), context.y() + 27,
                    typeWidth, 18);
        }
        refreshWidgets();
    }

    @Override
    public void tick() {
        if (screen.getTile().getInspirationStatus() != DrawingDeskTileEntity.InspirationStatus.NONE) {
            inspirationRequestTicks = 0;
        } else if (inspirationRequestTicks > 0) {
            inspirationRequestTicks--;
        }
        refreshWidgets();
        super.tick();
    }

    private void refreshWidgets() {
        KnowledgeLabProjection projection = ClientKnowledgeDataAPI.knowledgeLabProjection();
        List<?> entries = pageEntries(projection);
        int maxOffset = Math.max(0, entries.size() - visibleRows);
        listOffset = Math.max(0, Math.min(listOffset, maxOffset));
        int selected = selectedIndex();
        if (entries.isEmpty()) setSelectedIndex(0);
        else setSelectedIndex(Math.max(0, Math.min(selected, entries.size() - 1)));

        boolean active = isInspirationActive();
        closeButton.setText(Component.translatable(active
                ? "gui.frostedresearch.knowledge.return_to_cards"
                : "gui.frostedresearch.knowledge.close"));
        for (int index = 0; index < pageButtons.length; index++) {
            pageButtons[index].setSelected(page == Page.values()[index]);
        }
        for (int index = 0; index < listButtons.length; index++) {
            int entryIndex = listOffset + index;
            KnowledgeLabButton button = listButtons[index];
            boolean show = index < visibleRows && entryIndex < entries.size();
            setShown(button, show);
            if (!show) continue;
            button.setSelected(entryIndex == selectedIndex()).setText(entryTitle(entries.get(entryIndex)));
        }

        setShown(pinButton, page == Page.OBSERVATIONS && !projection.observations().isEmpty() && !active);
        if (pinButton.isVisible()) {
            KnowledgeProjection.ObservationSummary selectedObservation = selectedObservation(projection);
            boolean pinned = selectedObservation != null
                    && screen.getTile().getPinnedEvidence().contains(selectedObservation.id());
            pinButton.setText(Component.translatable(pinned
                    ? "gui.frostedresearch.knowledge.unpin"
                    : "gui.frostedresearch.knowledge.pin"));
        }

        boolean showOrganize = page == Page.OBSERVATIONS && !active;
        boolean ready = selectedIdea(projection) != null && selectedIdea(projection).state() == IdeaRecord.State.READY;
        setShown(primaryButton, showOrganize || (page == Page.IDEAS && ready));
        primaryButton.setText(Component.translatable(page == Page.IDEAS
                ? "gui.frostedresearch.knowledge.accept"
                : "gui.frostedresearch.knowledge.organize"));
        primaryButton.setEnabled(page == Page.IDEAS ? ready : screen.getTile().getPinnedEvidence().size() >= 2);

        setShown(secondaryButton, page == Page.OBSERVATIONS && (active || !screen.getTile().getPinnedEvidence().isEmpty()));
        secondaryButton.setText(Component.translatable(active
                ? "gui.frostedresearch.knowledge.cancel_session"
                : "gui.frostedresearch.knowledge.clear"));

        List<ActionCard> actions = actionsForSelectedIdea(projection);
        for (int index = 0; index < actionButtons.length; index++) {
            boolean show = page == Page.IDEAS && index < actions.size()
                    && screen.getTile().getIdeaCandidates().isEmpty();
            setShown(actionButtons[index], show);
            if (!show) continue;
            ActionCard card = actions.get(index);
            actionButtons[index].setText(Component.literal(card.executable() ? "▶ " : "• ")
                    .append(Component.translatable(workflowKey("action", card.actionId()))));
            actionButtons[index].setEnabled(card.executable());
        }
        List<IdeaCandidate> candidates = screen.getTile().getIdeaCandidates();
        for (int index = 0; index < candidateButtons.length; index++) {
            boolean show = page == Page.IDEAS && index < candidates.size();
            setShown(candidateButtons[index], show);
            if (show) candidateButtons[index].setText(Component.translatable(
                    workflowKey("idea", candidates.get(index).ideaId())));
        }
        for (int index = 0; index < resultTypeButtons.length; index++) {
            setShown(resultTypeButtons[index], page == Page.RESULTS);
            resultTypeButtons[index].setSelected(resultType == ResearchResult.ResultType.values()[index]);
        }
    }

    @Override
    public void drawBackground(GuiGraphics graphics, int x, int y, int width, int height, RenderingHint hint) {
        graphics.fill(x, y, x + width, y + height, PAPER);
        graphics.fill(x, y + 31, x + width, y + 33, TEAL);
        if (width >= 560) graphics.drawString(getFont(), Component.translatable("gui.frostedresearch.knowledge.title"),
                x + 10, y + 12, INK, false);
        drawPanel(graphics, x, y, layout.list(), PANEL);
        drawPanel(graphics, x, y, layout.detail(), DETAIL);
        drawPanel(graphics, x, y, layout.context(), PANEL);
        graphics.drawString(getFont(), pageListTitle(), x + layout.list().x() + 8,
                y + layout.list().y() + 8, RED, false);

        KnowledgeLabProjection projection = ClientKnowledgeDataAPI.knowledgeLabProjection();
        if (page == Page.OBSERVATIONS) drawObservations(graphics, x, y, projection);
        else if (page == Page.IDEAS) drawIdeas(graphics, x, y, projection);
        else drawResults(graphics, x, y, projection);

        if (isInspirationActive()) {
            Component active = Component.translatable("gui.frostedresearch.knowledge.active_session",
                    screen.getTile().getPinnedEvidence().size());
            int bannerWidth = Math.min(layout.detail().width() - 16, getFont().width(active) + 14);
            graphics.fill(x + layout.detail().x() + 8, y + layout.detail().y() + 7,
                    x + layout.detail().x() + 8 + bannerWidth, y + layout.detail().y() + 24, 0xFFD0C29E);
            graphics.drawString(getFont(), active, x + layout.detail().x() + 14,
                    y + layout.detail().y() + 12, TEAL, false);
        }
    }

    private void drawObservations(GuiGraphics graphics, int x, int y, KnowledgeLabProjection projection) {
        KnowledgeLabLayout.Bounds detail = layout.detail();
        KnowledgeLabLayout.Bounds context = layout.context();
        graphics.drawString(getFont(), Component.translatable("gui.frostedresearch.knowledge.observation_detail"),
                x + detail.x() + 8, y + detail.y() + 31, RED, false);
        KnowledgeProjection.ObservationSummary record = selectedObservation(projection);
        if (record == null) {
            drawEmpty(graphics, x + detail.x() + 8, y + detail.y() + 50, detail.width() - 16);
        } else {
            int cursor = y + detail.y() + 50;
            cursor = drawWrappedLine(graphics, Component.translatable("gui.frostedresearch.knowledge.field.subject")
                    .append(subjectName(record)), x + detail.x() + 8, cursor, detail.width() - 16, INK);
            cursor = drawWrappedLine(graphics, Component.literal(record.dimension() + "  "
                            + record.position().getX() + ", " + record.position().getY() + ", " + record.position().getZ()),
                    x + detail.x() + 8, cursor + 3, detail.width() - 16, MUTED);
            cursor = drawWrappedLine(graphics, Component.translatable("gui.frostedresearch.knowledge.field.time",
                            record.lastObserved()), x + detail.x() + 8, cursor + 3, detail.width() - 16, MUTED);
            if (!record.stateProperties().isEmpty()) cursor = drawWrappedLine(graphics,
                    Component.literal(joinState(record)), x + detail.x() + 8, cursor + 5, detail.width() - 16, MUTED);
            if (!record.contextFacts().isEmpty()) cursor = drawWrappedLine(graphics,
                    Component.translatable("gui.frostedresearch.knowledge.field.context")
                            .append(Component.literal(joinContext(record))),
                    x + detail.x() + 8, cursor + 5, detail.width() - 16, MUTED);
            if (!record.publicFacets().isEmpty()) cursor = drawWrappedLine(graphics,
                    Component.translatable("gui.frostedresearch.knowledge.field.facets")
                            .append(Component.literal(joinIds(record.publicFacets()))),
                    x + detail.x() + 8, cursor + 5, detail.width() - 16, MUTED);
            if (!record.channels().isEmpty()) drawWrappedLine(graphics,
                    Component.translatable("gui.frostedresearch.knowledge.field.channels")
                            .append(Component.literal(joinIds(record.channels()))),
                    x + detail.x() + 8, cursor + 5, detail.width() - 16, MUTED);
        }
        graphics.drawString(getFont(), Component.translatable("gui.frostedresearch.knowledge.board"),
                x + context.x() + 8, y + context.y() + 8, RED, false);
        drawPinnedRecords(graphics, x + context.x() + 8, y + context.y() + 28,
                context.width() - 16, projection.observations());
        Component feedback = inspirationFeedback(screen.getTile().getInspirationStatus());
        if (inspirationRequestTicks > 0) feedback = Component.translatable(
                "gui.frostedresearch.knowledge.feedback.checking");
        if (feedback != null) graphics.drawWordWrap(getFont(), feedback, x + context.x() + 8,
                y + context.bottom() - 74, context.width() - 16, MUTED);
    }

    private void drawIdeas(GuiGraphics graphics, int x, int y, KnowledgeLabProjection projection) {
        KnowledgeLabLayout.Bounds detail = layout.detail();
        KnowledgeLabLayout.Bounds context = layout.context();
        graphics.drawString(getFont(), Component.translatable("gui.frostedresearch.knowledge.idea_workspace"),
                x + detail.x() + 8, y + detail.y() + 31, RED, false);
        KnowledgeProjection.IdeaSummary idea = selectedIdea(projection);
        if (idea == null) {
            drawEmpty(graphics, x + detail.x() + 8, y + detail.y() + 50, detail.width() - 16);
        } else {
            int cursor = y + detail.y() + 50;
            cursor = drawWrappedLine(graphics, Component.translatable(workflowKey("idea", idea.ideaId())),
                    x + detail.x() + 8, cursor, detail.width() - 16, TEAL);
            cursor = drawWrappedLine(graphics, Component.translatable("gui.frostedresearch.knowledge.idea.state",
                            stateTitle(idea.state())), x + detail.x() + 8, cursor + 5, detail.width() - 16,
                    idea.state() == IdeaRecord.State.ORPHAN ? RED : MUTED);
            cursor = drawWrappedLine(graphics, Component.translatable("gui.frostedresearch.knowledge.idea.counts",
                            idea.sourceCount(), idea.evidenceCount()), x + detail.x() + 8, cursor + 3,
                    detail.width() - 16, MUTED);
            List<KnowledgeProjection.ComparisonSummary> artifacts = projection.artifacts().stream()
                    .filter(value -> value.topicId().equals(idea.topicId())).toList();
            if (!artifacts.isEmpty()) {
                cursor += 9;
                graphics.drawString(getFont(), Component.translatable("gui.frostedresearch.knowledge.artifacts"),
                        x + detail.x() + 8, cursor, RED, false);
                cursor += 14;
                for (KnowledgeProjection.ComparisonSummary artifact : artifacts) {
                    cursor = drawWrappedLine(graphics, Component.literal("• ").append(Component.translatable(
                                    "gui.frostedresearch.knowledge.comparison."
                                            + artifact.outcome().name().toLowerCase())),
                            x + detail.x() + 8, cursor, detail.width() - 16, MUTED);
                }
            }
        }
        graphics.drawString(getFont(), Component.translatable("gui.frostedresearch.knowledge.next_steps"),
                x + context.x() + 8, y + context.y() + 8, RED, false);
        if (screen.getTile().getIdeaCandidates().size() > 1) graphics.drawWordWrap(getFont(),
                Component.translatable("gui.frostedresearch.knowledge.choose_idea"),
                x + context.x() + 8, y + context.y() + 31, context.width() - 16, MUTED);
    }

    private void drawResults(GuiGraphics graphics, int x, int y, KnowledgeLabProjection projection) {
        KnowledgeLabLayout.Bounds detail = layout.detail();
        KnowledgeLabLayout.Bounds context = layout.context();
        graphics.drawString(getFont(), Component.translatable("gui.frostedresearch.knowledge.result_detail"),
                x + detail.x() + 8, y + detail.y() + 31, RED, false);
        KnowledgeLabProjection.ResultSummary result = selectedResult(projection);
        if (result == null) {
            drawEmpty(graphics, x + detail.x() + 8, y + detail.y() + 50, detail.width() - 16);
        } else {
            int cursor = y + detail.y() + 50;
            cursor = drawWrappedLine(graphics, Component.literal(result.id().toString()),
                    x + detail.x() + 8, cursor, detail.width() - 16, TEAL);
            cursor = drawWrappedLine(graphics, Component.translatable("gui.frostedresearch.knowledge.result.type")
                            .append(resultTypeTitle(result.type())), x + detail.x() + 8, cursor + 5,
                    detail.width() - 16, MUTED);
            if (result.orphan()) cursor = drawWrappedLine(graphics,
                    Component.translatable("gui.frostedresearch.knowledge.result.orphan"),
                    x + detail.x() + 8, cursor + 5, detail.width() - 16, RED);
            if (result.topicId().isPresent()) cursor = drawWrappedLine(graphics,
                    Component.translatable("gui.frostedresearch.knowledge.result.topic")
                            .append(Component.literal(result.topicId().get().toString())),
                    x + detail.x() + 8, cursor + 5, detail.width() - 16, MUTED);
            if (!result.targets().isEmpty()) {
                cursor += 8;
                graphics.drawString(getFont(), Component.translatable("gui.frostedresearch.knowledge.result.targets"),
                        x + detail.x() + 8, cursor, RED, false);
                cursor += 14;
                for (ResourceLocation target : result.targets()) {
                    cursor = drawWrappedLine(graphics, Component.literal("• " + target),
                            x + detail.x() + 8, cursor, detail.width() - 16, MUTED);
                }
            }
        }
        graphics.drawString(getFont(), Component.translatable("gui.frostedresearch.knowledge.result_categories"),
                x + context.x() + 8, y + context.y() + 8, RED, false);
        int cursor = y + context.y() + 57;
        for (ResearchResult.ResultType type : ResearchResult.ResultType.values()) {
            long count = projection.results().stream().filter(value -> value.type() == type).count();
            graphics.drawString(getFont(), resultTypeTitle(type).copy().append(Component.literal("  " + count)),
                    x + context.x() + 8, cursor, type == resultType ? TEAL : MUTED, false);
            cursor += 14;
        }
    }

    private void drawPinnedRecords(GuiGraphics graphics, int x, int y, int width,
            List<KnowledgeProjection.ObservationSummary> records) {
        List<UUID> pins = screen.getTile().getPinnedEvidence();
        for (int index = 0; index < 5; index++) {
            int rowY = y + index * 13;
            KnowledgeProjection.ObservationSummary record = index < pins.size() ? find(records, pins.get(index)) : null;
            Component line = record == null ? Component.literal("○ —")
                    : Component.literal("● ").append(subjectName(record));
            graphics.drawString(getFont(), getFont().plainSubstrByWidth(line.getString(), width), x, rowY,
                    record == null ? MUTED : TEAL, false);
        }
    }

    private void drawEmpty(GuiGraphics graphics, int x, int y, int width) {
        graphics.drawWordWrap(getFont(), Component.translatable("gui.frostedresearch.knowledge.empty"),
                x, y, width, MUTED);
    }

    private int drawWrappedLine(GuiGraphics graphics, Component line, int x, int y, int width, int color) {
        graphics.drawWordWrap(getFont(), line, x, y, width, color);
        return y + Math.max(getFont().lineHeight, getFont().split(line, width).size() * getFont().lineHeight);
    }

    private static void drawPanel(GuiGraphics graphics, int originX, int originY,
            KnowledgeLabLayout.Bounds bounds, int color) {
        graphics.fill(originX + bounds.x(), originY + bounds.y(), originX + bounds.right(),
                originY + bounds.bottom(), color);
    }

    private void selectPage(Page replacement) {
        page = replacement;
        listOffset = 0;
        refreshWidgets();
    }

    private void selectResultType(ResearchResult.ResultType replacement) {
        resultType = replacement;
        selectedResult = 0;
        listOffset = 0;
        refreshWidgets();
    }

    private void selectListRow(int row) {
        setSelectedIndex(listOffset + row);
        refreshWidgets();
    }

    private void toggleSelectedObservation() {
        KnowledgeProjection.ObservationSummary record = selectedObservation(
                ClientKnowledgeDataAPI.knowledgeLabProjection());
        if (record == null || isInspirationActive()) return;
        FRNetwork.INSTANCE.sendToServer(new FHDrawingDeskOperationPacket(
                screen.getTile().getBlockPos(), 5, record.id()));
    }

    private void primaryAction() {
        if (page == Page.IDEAS) send(9);
        else requestInspiration();
    }

    private void secondaryAction() {
        send(6);
    }

    private void closeOrReturn() {
        if (isInspirationActive()) screen.showInspirationGame();
        else close.run();
    }

    private void requestInspiration() {
        if (screen.getTile().getPinnedEvidence().size() < 2) return;
        inspirationRequestTicks = 40;
        send(4);
    }

    private void send(int operation) {
        FRNetwork.INSTANCE.sendToServer(new FHDrawingDeskOperationPacket(screen.getTile().getBlockPos(), operation));
    }

    private void sendCandidate(int candidateIndex) {
        FRNetwork.INSTANCE.sendToServer(new FHDrawingDeskOperationPacket(
                screen.getTile().getBlockPos(), 7, candidateIndex));
    }

    private void executeMethod(int actionIndex) {
        List<ActionCard> actions = actionsForSelectedIdea(ClientKnowledgeDataAPI.knowledgeLabProjection());
        if (actionIndex < 0 || actionIndex >= actions.size()) return;
        ActionCard action = actions.get(actionIndex);
        if (!action.executable()) return;
        FRNetwork.INSTANCE.sendToServer(new FHDrawingDeskOperationPacket(
                screen.getTile().getBlockPos(), action.topicId(), action.protocolId()));
    }

    @Override
    public boolean onMouseScrolled(double scroll) {
        if (layout.list().contains(getMouseX(), getMouseY())) {
            int size = pageEntries(ClientKnowledgeDataAPI.knowledgeLabProjection()).size();
            int maxOffset = Math.max(0, size - visibleRows);
            listOffset = Math.max(0, Math.min(maxOffset,
                    listOffset - (int) Math.signum(scroll) * Math.max(1, visibleRows / 2)));
            refreshWidgets();
            return true;
        }
        return super.onMouseScrolled(scroll);
    }

    private List<?> pageEntries(KnowledgeLabProjection projection) {
        return switch (page) {
            case OBSERVATIONS -> observations(projection);
            case IDEAS -> ideas(projection);
            case RESULTS -> results(projection);
        };
    }

    private List<KnowledgeProjection.ObservationSummary> observations(KnowledgeLabProjection projection) {
        return projection.observations().stream()
                .sorted(Comparator.comparingLong(KnowledgeProjection.ObservationSummary::lastObserved).reversed()
                        .thenComparing(value -> value.id().toString())).toList();
    }

    private List<KnowledgeProjection.IdeaSummary> ideas(KnowledgeLabProjection projection) {
        return projection.ideas().stream().sorted(Comparator.comparing(value -> value.ideaId().toString())).toList();
    }

    private List<KnowledgeLabProjection.ResultSummary> results(KnowledgeLabProjection projection) {
        return projection.results().stream().filter(value -> value.type() == resultType)
                .sorted(Comparator.comparing(value -> value.id().toString())).toList();
    }

    private Component entryTitle(Object entry) {
        if (entry instanceof KnowledgeProjection.ObservationSummary observation) {
            return subjectName(observation).copy().append(Component.literal("  @ "
                    + observation.position().getX() + "," + observation.position().getY() + ","
                    + observation.position().getZ()));
        }
        if (entry instanceof KnowledgeProjection.IdeaSummary idea) {
            return Component.translatable(workflowKey("idea", idea.ideaId()));
        }
        if (entry instanceof KnowledgeLabProjection.ResultSummary result) {
            return Component.literal(result.orphan() ? "⚠ " + result.id() : result.id().toString());
        }
        return Component.empty();
    }

    private int selectedIndex() {
        return switch (page) {
            case OBSERVATIONS -> selectedObservation;
            case IDEAS -> selectedIdea;
            case RESULTS -> selectedResult;
        };
    }

    private void setSelectedIndex(int index) {
        switch (page) {
            case OBSERVATIONS -> selectedObservation = index;
            case IDEAS -> selectedIdea = index;
            case RESULTS -> selectedResult = index;
        }
    }

    private KnowledgeProjection.ObservationSummary selectedObservation(KnowledgeLabProjection projection) {
        List<KnowledgeProjection.ObservationSummary> values = observations(projection);
        return values.isEmpty() ? null : values.get(Math.min(selectedObservation, values.size() - 1));
    }

    private KnowledgeProjection.IdeaSummary selectedIdea(KnowledgeLabProjection projection) {
        List<KnowledgeProjection.IdeaSummary> values = ideas(projection);
        return values.isEmpty() ? null : values.get(Math.min(selectedIdea, values.size() - 1));
    }

    private KnowledgeLabProjection.ResultSummary selectedResult(KnowledgeLabProjection projection) {
        List<KnowledgeLabProjection.ResultSummary> values = results(projection);
        return values.isEmpty() ? null : values.get(Math.min(selectedResult, values.size() - 1));
    }

    private List<ActionCard> actionsForSelectedIdea(KnowledgeLabProjection projection) {
        KnowledgeProjection.IdeaSummary idea = selectedIdea(projection);
        if (idea == null) return List.of();
        return ClientKnowledgeDataAPI.knowledgeProjection().actions().stream()
                .filter(action -> action.topicId().equals(idea.topicId())).limit(3).toList();
    }

    private boolean isInspirationActive() {
        return screen.getTile().getGamePurpose() == DrawingDeskTileEntity.GamePurpose.V2_INSPIRATION;
    }

    private Component subjectName(KnowledgeProjection.ObservationSummary record) {
        net.minecraft.world.level.block.Block block = ForgeRegistries.BLOCKS.getValue(record.subject());
        Component name = block == null ? Component.literal(record.subject().toString()) : block.getName();
        if (record.annotations().isEmpty()) return name;
        return name.copy().append(Component.literal(" · ")).append(Component.translatable(
                workflowKey("annotation", record.annotations().get(0))));
    }

    private static KnowledgeProjection.ObservationSummary find(
            List<KnowledgeProjection.ObservationSummary> records, UUID id) {
        return records.stream().filter(record -> record.id().equals(id)).findFirst().orElse(null);
    }

    private static String joinState(KnowledgeProjection.ObservationSummary record) {
        return record.stateProperties().entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue())
                .sorted().reduce((left, right) -> left + ", " + right).orElse("");
    }

    private static String joinContext(KnowledgeProjection.ObservationSummary record) {
        return record.contextFacts().entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue()).sorted()
                .reduce((left, right) -> left + ", " + right).orElse("");
    }

    private static String joinIds(Set<ResourceLocation> values) {
        return values.stream().map(ResourceLocation::toString).sorted()
                .reduce((left, right) -> left + ", " + right).orElse("");
    }

    private static Component inspirationFeedback(DrawingDeskTileEntity.InspirationStatus status) {
        return switch (status) {
            case NO_CANDIDATE -> Component.translatable("gui.frostedresearch.knowledge.feedback.no_candidate");
            case NEED_PAPER -> Component.translatable("gui.frostedresearch.knowledge.feedback.need_paper");
            case NEED_INK -> Component.translatable("gui.frostedresearch.knowledge.feedback.need_ink");
            default -> null;
        };
    }

    private static Component pageTitle(Page page) {
        return Component.translatable("gui.frostedresearch.knowledge.page." + page.key);
    }

    private Component pageListTitle() {
        return Component.translatable("gui.frostedresearch.knowledge.list." + page.key,
                pageEntries(ClientKnowledgeDataAPI.knowledgeLabProjection()).size());
    }

    private static Component resultTypeTitle(ResearchResult.ResultType type) {
        return Component.translatable("gui.frostedresearch.knowledge.result_type." + type.token());
    }

    private static Component stateTitle(IdeaRecord.State state) {
        return Component.translatable("gui.frostedresearch.knowledge.idea_state." + state.name().toLowerCase());
    }

    private static void setShown(UIElement element, boolean shown) {
        element.setVisible(shown);
        element.setEnabled(shown);
    }

    private static String workflowKey(String kind, ResourceLocation id) {
        return "gui.frostedresearch.knowledge." + kind + "." + id.getNamespace() + "."
                + id.getPath().replace('/', '.');
    }

    private enum Page {
        OBSERVATIONS("observations"),
        IDEAS("ideas"),
        RESULTS("results");

        private final String key;

        Page(String key) {
            this.key = key;
        }
    }
}
