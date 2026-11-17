package com.teammoeg.frostedresearch.gui.archive;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.frostedresearch.gui.drawdesk.DrawDeskScreen;
import com.teammoeg.frostedresearch.knowledge.client.KnowledgeClientState;
import com.teammoeg.frostedresearch.knowledge.client.KnowledgeJournalData;
import com.teammoeg.frostedresearch.knowledge.client.ui.JournalButton;
import com.teammoeg.frostedresearch.knowledge.client.ui.JournalList;
import com.teammoeg.frostedresearch.knowledge.client.ui.JournalText;
import com.teammoeg.frostedresearch.knowledge.client.ui.KnowledgeUiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Creative inspection of datapack projects, kept separate from legacy graph and team discoveries. */
final class CreativeProjectCatalogue extends UILayer {
    private final JournalList list;
    private final JournalText summary;
    private final JournalButton details;
    private KnowledgeJournalData data;
    private String selected;
    private String query = "";
    private long serial = -1;
    private Language language;
    private int listWidth = 142;

    CreativeProjectCatalogue(ResearchArchiveLayer parent) {
        super(parent);
        setTheme(KnowledgeUiTheme.INSTANCE);
        list = new JournalList(this, (row, ignored) -> {
            selected = (String) row.value();
            refreshSummary();
        });
        summary = new JournalText(this);
        details = new JournalButton(this, Component.translatable("gui.frostedresearch.journal.research.details"), false, () -> {
            if (available() && selected != null && getParent() instanceof ResearchArchiveLayer archive
                && archive.getParent() instanceof DrawDeskScreen desk) desk.showCreativeProject(selected);
        });
    }

    static boolean available() {
        var player = Minecraft.getInstance().player;
        return player != null && player.isCreative();
    }

    @Override
    public void addUIElements() {
        add(list);
        add(summary);
        add(details);
    }

    @Override
    public void alignWidgets() {
        resizeCatalogue(getWidth(), getHeight(), listWidth);
    }

    void resizeCatalogue(int width, int height, int listWidth) {
        this.listWidth = listWidth;
        setSize(width, height);
        list.setPosAndSize(0, 0, listWidth, height);
        int right = listWidth + 12;
        summary.setPosAndSize(right, 5, Math.max(20, width - right - 6), Math.max(20, height - 34));
        details.setPosAndSize(right, height - 23, Math.max(20, width - right - 6), 20);
    }

    void refresh(String search) {
        if (serial == KnowledgeClientState.serial() && language == Language.getInstance() && query.equals(search)) return;
        serial = KnowledgeClientState.serial();
        language = Language.getInstance();
        query = search;
        data = new KnowledgeJournalData(KnowledgeClientState.snapshot());
        String needle = search.toLowerCase(Locale.ROOT);
        var projects = data.creativeProjects.stream().filter(p -> p.id().toLowerCase(Locale.ROOT).contains(needle)
            || p.title().getString().toLowerCase(Locale.ROOT).contains(needle)).toList();
        list.rows(projects.stream().map(p -> new JournalList.Row(p.id(), p.title(), status(p), new ItemStack(Items.WRITABLE_BOOK), false)).toList());
        if (projects.stream().noneMatch(p -> p.id().equals(selected))) selected = projects.isEmpty() ? null : projects.get(0).id();
        refreshSummary();
    }

    private void refreshSummary() {
        list.focus(selected);
        var project = data == null ? null : data.creativeProjects.stream().filter(p -> p.id().equals(selected)).findFirst().orElse(null);
        details.setEnabled(project != null);
        if (project == null) {
            summary.text(List.of(Component.translatable("gui.frostedresearch.archive.new_projects.empty")));
            return;
        }
        List<Component> paragraphs = new ArrayList<>();
        paragraphs.add(project.title());
        paragraphs.add(Component.literal(project.id()));
        paragraphs.add(status(project));
        paragraphs.add(project.body());
        if (!project.outputTitles().isEmpty()) {
            paragraphs.add(Component.translatable("gui.frostedresearch.journal.graph.expected_results"));
            paragraphs.addAll(project.outputTitles());
        }
        summary.text(paragraphs);
    }

    private Component status(KnowledgeJournalData.Project project) {
        var run = data == null ? null : data.openRun(project.id());
        if (!project.completed() && run != null) return KnowledgeJournalData.runLabel(run);
        String key = project.completed() ? "gui.frostedresearch.journal.research.state.completed"
            : !project.active() ? "gui.frostedresearch.archive.new_projects.locked"
            : project.startStatus().equals("LEARNABLE") ? "gui.frostedresearch.journal.research.state.ready"
            : "gui.frostedresearch.journal.research.state.needs_knowledge";
        return Component.translatable(key);
    }

    @Override
    public void drawBackground(GuiGraphics graphics, int x, int y, int width, int height, RenderingHint hint) {
        graphics.fill(x, y, x + width, y + height, ResearchArchiveLayer.COLOR_PAPER);
    }
}
