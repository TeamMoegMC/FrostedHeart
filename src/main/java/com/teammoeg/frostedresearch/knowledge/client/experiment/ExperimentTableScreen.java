/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.client.experiment;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MenuPrimaryLayer;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.frostedresearch.FRNetwork;
import com.teammoeg.frostedresearch.gui.drawdesk.DrawDeskIcons;
import com.teammoeg.frostedresearch.knowledge.client.KnowledgeClientState;
import com.teammoeg.frostedresearch.knowledge.client.KnowledgeJournalData;
import com.teammoeg.frostedresearch.knowledge.client.KnowledgePresentation;
import com.teammoeg.frostedresearch.knowledge.client.ui.JournalButton;
import com.teammoeg.frostedresearch.knowledge.client.ui.JournalText;
import com.teammoeg.frostedresearch.knowledge.client.ui.KnowledgeUiTheme;
import com.teammoeg.frostedresearch.knowledge.experiment.ExperimentReport;
import com.teammoeg.frostedresearch.knowledge.experiment.ExperimentTableActionPacket;
import com.teammoeg.frostedresearch.knowledge.experiment.ExperimentTableActionPacket.Action;
import com.teammoeg.frostedresearch.knowledge.experiment.ExperimentTableBlockEntity;
import com.teammoeg.frostedresearch.knowledge.experiment.ExperimentTableMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 实验台界面：选择队伍进行中研究的一项实验，查看条件清单，开始或停止。所有判断都在服务端。
 * Experiment table screen: choose an experiment of the team's open research, read its checklist, start or stop it.
 * The server judges everything; this screen only shows the table's synchronized state.
 */
public final class ExperimentTableScreen extends MenuPrimaryLayer<ExperimentTableMenu> {
    private static final CIcons.CTextureIcon PANEL = DrawDeskIcons.ALL.withUV(0, 0, 140, 203, 512, 512);
    private static final int WIDTH = 440, HEIGHT = 210, RIGHT = 149;
    /** Colours of the desk panel's plain column and of its inventory cells. */
    private static final int COLUMN = 0xFF8F683A, COLUMN_EDGE = 0xFFAB8454, CELL_EDGE = 0xFF573B1F, CELL = 0xFF694929;

    private record Option(String run, String requirement, Component title) {
    }

    private final JournalText text;
    private List<Option> options = List.of();
    private int index;
    private long serial = -1;
    private int signature;

    public ExperimentTableScreen(ExperimentTableMenu menu) {
        super(menu);
        setTheme(KnowledgeUiTheme.INSTANCE);
        text = new JournalText(this);
    }

    static Component t(String key, Object... args) {
        return Component.translatable("gui.frostedresearch.experiment." + key, args);
    }

    @Override
    public boolean onInit() {
        setSize(WIDTH, HEIGHT);
        return super.onInit();
    }

    private ExperimentTableBlockEntity table() {
        return menu.getBlock();
    }

    /**
     * Experiments the team still needs, from the visible snapshot; the table's own target is kept selected.
     */
    private void reloadOptions() {
        serial = KnowledgeClientState.serial();
        var data = new KnowledgeJournalData(KnowledgeClientState.snapshot());
        List<Option> list = new ArrayList<>();
        for (var run : data.runs)
            if (run.open())
                for (var requirement : run.requirements())
                    if (requirement.experiment() && !requirement.satisfied())
                        list.add(new Option(run.id(), requirement.id(), t("option", run.title(), requirement.title())));
        options = List.copyOf(list);
        var table = table();
        for (int i = 0; i < options.size(); i++)
            if (table.run() != null && options.get(i).run().equals(table.run().toString()) && options.get(i).requirement().equals(table.requirement()))
                index = i;
        index = options.isEmpty() ? 0 : Math.floorMod(index, options.size());
    }

    private boolean chosen(Option option) {
        var table = table();
        return option != null && table.run() != null && option.run().equals(table.run().toString()) && option.requirement().equals(table.requirement());
    }

    private void send(Action action, Option option) {
        FRNetwork.INSTANCE.sendToServer(new ExperimentTableActionPacket(table().getBlockPos(), action,
                option == null ? "" : option.run(), option == null ? "" : option.requirement()));
    }

    private JournalButton button(int x, int y, int width, int height, Component label, boolean quiet, Runnable action) {
        JournalButton button = new JournalButton(this, label, quiet, action);
        button.setPosAndSize(x, y, width, height);
        add(button);
        return button;
    }

    @Override
    public void addChildUIElements() {
        if (serial != KnowledgeClientState.serial()) reloadOptions();
        var table = table();
        Option option = options.isEmpty() ? null : options.get(index);
        int width = WIDTH - RIGHT - 14;
        if (options.size() > 1) {
            button(RIGHT, 30, 20, 18, Component.literal("‹"), true, () -> {
                index = Math.floorMod(index - 1, options.size());
                refresh();
            });
            button(WIDTH - 34, 30, 20, 18, Component.literal("›"), true, () -> {
                index = Math.floorMod(index + 1, options.size());
                refresh();
            });
        }
        button(RIGHT + 24, 30, width - 48, 18, option == null ? t("none") : option.title(), true, () -> {
        }).setEnabled(false);

        List<Component> lines = new ArrayList<>();
        if (option == null) lines.add(t("empty"));
        else if (!chosen(option)) lines.add(t(table.run() == null ? "choose" : "switch"));
        else {
            lines.add(switch (table.status()) {
                case RUNNING -> t("status.running", seconds(table.progress()), seconds(table.duration()));
                case PAUSED -> t("status.paused", seconds(table.progress()), seconds(table.duration()));
                case IDLE -> t("status.idle", seconds(table.duration()));
            });
            for (ExperimentReport.Line line : table.report().lines()) lines.add(line(line));
        }
        if (!table.message().isBlank()) lines.add(t("message." + table.message()).copy().withStyle(s -> s.withColor(KnowledgeUiTheme.ACCENT)));
        text.setPosAndSize(RIGHT, 54, width, 122);
        text.color(KnowledgeUiTheme.INK);
        text.text(lines);
        add(text);

        int third = (width - 8) / 3;
        if (option != null && !chosen(option)) {
            button(RIGHT, 182, width, 19, t("select"), false, () -> send(Action.SELECT, option))
                    .tooltip(t(table.status() == ExperimentTableBlockEntity.Status.IDLE ? "select.help" : "select.stops"));
            return;
        }
        boolean idle = table.status() == ExperimentTableBlockEntity.Status.IDLE;
        button(RIGHT, 182, third, 19, t("check"), true, () -> send(Action.CHECK, option)).setEnabled(option != null);
        button(RIGHT + third + 4, 182, third, 19, t("start"), false, () -> send(Action.START, option))
                .tooltip(t("start.help")).setEnabled(option != null && idle && table.report().ok());
        button(RIGHT + 2 * third + 8, 182, width - 2 * third - 8, 19, t("stop"), true, () -> send(Action.STOP, option))
                .tooltip(t("stop.help")).setEnabled(!idle);
    }

    private static String seconds(int ticks) {
        return Integer.toString(Math.max(0, ticks) / 20);
    }

    private static Component line(ExperimentReport.Line line) {
        Object[] args = line.args().stream().map(arg -> arg.translate() ? Component.translatable(arg.value())
                : (Object) KnowledgePresentation.text(arg.value()).copy()).toArray();
        Component content = t(line.key(), args);
        return t(line.ok() ? "line.ok" : "line.fail", content).copy()
                .withStyle(s -> s.withColor(line.ok() ? KnowledgeUiTheme.INK : KnowledgeUiTheme.RED));
    }

    @Override
    public void alignWidgets() {
    }

    @Override
    public void tick() {
        var table = table();
        int now = Objects.hash(table.run(), table.requirement(), table.status(), table.progress(), table.message(), table.report(),
                KnowledgeClientState.serial(), Language.getInstance());
        if (now != signature) {
            signature = now;
            refresh();
        }
        super.tick();
    }

    @Override
    public void drawBackground(GuiGraphics g, int x, int y, int w, int h, RenderingHint hint) {
        KnowledgeUiTheme.drawPaper(g, x, y, w, h);
        PANEL.draw(g, x, y, 140, 203);
        // The panel's right column holds the desk's examine, paper and pen slots; this table has none of them.
        g.fill(x + 111, y + 92, x + 133, y + 194, COLUMN);
        g.fill(x + 111, y + 92, x + 112, y + 194, COLUMN_EDGE);
        g.drawString(getFont(), t("materials"), x + 21, y + 20, KnowledgeUiTheme.MUTED, false);
        for (int[] slot : ExperimentTableMenu.MATERIAL_POSITIONS) {
            g.fill(x + slot[0] - 1, y + slot[1] - 1, x + slot[0] + 17, y + slot[1] + 17, CELL_EDGE);
            g.fill(x + slot[0], y + slot[1], x + slot[0] + 16, y + slot[1] + 16, CELL);
        }
        g.drawString(getFont(), Component.translatable("block.frostedresearch.experiment_table"), x + RIGHT, y + 14, KnowledgeUiTheme.INK, false);
        KnowledgeUiTheme.rule(g, x + RIGHT, y + 25, w - RIGHT - 14);
    }
}
