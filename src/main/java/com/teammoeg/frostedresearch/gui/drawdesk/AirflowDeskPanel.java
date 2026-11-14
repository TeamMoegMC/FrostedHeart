package com.teammoeg.frostedresearch.gui.drawdesk;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.frostedresearch.FRNetwork;
import com.teammoeg.frostedresearch.knowledge.client.ui.JournalButton;
import com.teammoeg.frostedresearch.knowledge.network.KnowledgeWorkbenchActionPacket;
import com.teammoeg.frostedresearch.knowledge.network.KnowledgeWorkbenchActionPacket.Action;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.List;

import static com.teammoeg.frostedresearch.knowledge.client.KnowledgePresentation.t;
import static com.teammoeg.frostedresearch.knowledge.client.ui.KnowledgeUiTheme.*;

/** The server-owned airflow board occupies the same desk area as the legacy matching game. */
public final class AirflowDeskPanel extends UILayer {
    private final DrawDeskScreen desk;
    private final String run, task;
    private final Component projectTitle;
    private CompoundTag board = new CompoundTag();

    public AirflowDeskPanel(DrawDeskLayer parent, DrawDeskScreen desk, String run, String task, Component projectTitle) {
        super(parent);
        this.desk = desk;
        this.run = run;
        this.task = task;
        this.projectTitle = projectTitle;
        setPosAndSize(165, 25, 218, 164);
    }

    public Component projectTitle() { return projectTitle; }
    public CompoundTag board() { return board.copy(); }
    public boolean won() { return board.getBoolean("won"); }
    public void start() { send(Action.START_AIRFLOW, ""); }
    public void receive(CompoundTag payload) {
        if (payload.contains("airflow")) board = payload.getCompound("airflow").copy();
    }
    private void send(Action action, String cell) {
        FRNetwork.INSTANCE.sendToServer(new KnowledgeWorkbenchActionPacket(desk.getTile().getBlockPos(), action,
            List.of(), run, cell, "", task));
    }

    @Override
    public void addUIElements() {
        int[] masks = board.getIntArray("masks");
        int left = 61, top = 23;
        for (int cell = 0; cell < 9; cell++) {
            final int selected = cell;
            var button = new JournalButton(this, Component.empty(), false, () -> send(Action.ROTATE_AIRFLOW, Integer.toString(selected)));
            button.setPosAndSize(left + cell % 3 * 32, top + cell / 3 * 32, 30, 30);
            button.setEnabled(masks.length == 9 && masks[cell] != 0 && !won());
            add(button);
        }
        UIElement pipes = new UIElement(this) {
            @Override
            public void render(GuiGraphics g, int x, int y, int w, int h, RenderingHint hint) {
                int color = won() ? ACCENT : INK;
                for (int cell = 0; cell < 9; cell++) {
                    int mask = masks.length == 9 ? masks[cell] : 0;
                    int bx = x + 12 + cell % 3 * 32, by = y + cell / 3 * 32, cx = bx + 15, cy = by + 15;
                    if (mask == 0) { g.fill(bx + 8, by + 8, bx + 22, by + 22, MUTED); continue; }
                    g.fill(cx - 2, cy - 2, cx + 2, cy + 2, color);
                    if ((mask & 1) != 0) g.fill(cx - 2, by, cx + 2, cy, color);
                    if ((mask & 2) != 0) g.fill(cx, cy - 2, bx + 30, cy + 2, color);
                    if ((mask & 4) != 0) g.fill(cx - 2, cy, cx + 2, by + 30, color);
                    if ((mask & 8) != 0) g.fill(bx, cy - 2, cx, cy + 2, color);
                }
                int mid = y + 47;
                for (int ax : new int[]{x, x + 108}) {
                    g.fill(ax, mid - 2, ax + 10, mid + 2, color);
                    g.fill(ax + 6, mid - 5, ax + 8, mid + 5, color);
                    g.fill(ax + 8, mid - 3, ax + 10, mid + 3, color);
                    g.fill(ax + 10, mid - 1, ax + 12, mid + 1, color);
                }
            }
        };
        pipes.setPosAndSize(left - 12, top, 120, 94);
        pipes.setEnabled(false);
        add(pipes);
        var reset = new JournalButton(this, t("research.airflow_reset"), false, () -> send(Action.RESET_AIRFLOW, ""));
        reset.setPosAndSize(1, 140, 104, 20);
        reset.setEnabled(masks.length == 9 && !won());
        add(reset);
        var back = new JournalButton(this, t("research.airflow_back"), false, desk::closeAirflow);
        back.setPosAndSize(109, 140, 104, 20);
        add(back);
    }

    @Override public void alignWidgets() {}
    @Override
    public void render(GuiGraphics g, int x, int y, int w, int h, RenderingHint hint) {
        super.render(g, x, y, w, h, hint);
        var title = t("research.airflow_start");
        g.drawString(getFont(), title, x + (w - getFont().width(title)) / 2, y + 3, INK, false);
    }
}
