package com.teammoeg.frostedresearch.gui.drawdesk;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.frostedresearch.FRNetwork;
import com.teammoeg.frostedresearch.knowledge.client.ui.JournalButton;
import com.teammoeg.frostedresearch.knowledge.game.AirflowGame;
import com.teammoeg.frostedresearch.knowledge.network.KnowledgeWorkbenchActionPacket;
import com.teammoeg.frostedresearch.knowledge.network.KnowledgeWorkbenchActionPacket.Action;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;
import java.util.List;
import java.util.UUID;

import static com.teammoeg.frostedresearch.knowledge.client.KnowledgePresentation.t;
import static com.teammoeg.frostedresearch.knowledge.client.ui.KnowledgeUiTheme.*;

/** Excavation and continuous wind, hosted on the drawing desk's parchment. */
public final class AirflowDeskPanel extends UILayer {
    private static final int CELL = 4, WIND = 0xFF56B9D3;
    private final DrawDeskScreen desk;
    private final String run, task;
    private final Component projectTitle;
    private final AirflowGame preview = new AirflowGame();
    private final ArrayDeque<AirflowGame.Stroke> pending = new ArrayDeque<>();
    private final DigCanvas canvas = new DigCanvas();
    private CompoundTag board = new CompoundTag();
    private UUID inFlight;
    private int sent, ticks, wonAt;
    private JournalButton reset;

    public AirflowDeskPanel(DrawDeskLayer parent, DrawDeskScreen desk, String run, String task, Component projectTitle) {
        super(parent);
        this.desk = desk; this.run = run; this.task = task; this.projectTitle = projectTitle;
        setPosAndSize(165, 25, 218, 164);
    }

    public Component projectTitle() { return projectTitle; }
    public CompoundTag board() { return board.copy(); }
    public boolean won() { return board.getBoolean("won"); }
    public void start() { send(Action.START_AIRFLOW, ""); }
    public void receive(CompoundTag payload) {
        if (inFlight != null && inFlight.equals(payload.getUUID("request"))) {
            for (int i = 0; i < sent; i++) pending.removeFirst();
            inFlight = null; sent = 0;
        }
        if (!payload.contains("airflow")) return;
        boolean wasWon = won(); board = payload.getCompound("airflow").copy();
        if (!wasWon && won()) wonAt = ticks;
        preview.load(board);
        if (won()) pending.clear();
        else for (var stroke : pending) preview.dig(stroke.from(), stroke.to());
    }
    private UUID send(Action action, String strokes) {
        var packet = new KnowledgeWorkbenchActionPacket(desk.getTile().getBlockPos(), action,
            List.of(), run, "", strokes, task);
        FRNetwork.INSTANCE.sendToServer(packet); return packet.requestId();
    }
    private void queue(int from, int to) {
        if (board.isEmpty() || won() || !preview.dig(from, to)) return;
        pending.addLast(new AirflowGame.Stroke(from, to));
    }
    public void flushPending() {
        int skip = inFlight == null ? 0 : sent, count = 0;
        var encoded = new StringBuilder();
        for (var stroke : pending) {
            if (skip-- > 0) continue;
            encoded.append(stroke.from()).append(':').append(stroke.to()).append(';');
            if (++count == 32) { send(Action.DIG_AIRFLOW, encoded.toString()); encoded.setLength(0); count = 0; }
        }
        if (!encoded.isEmpty()) send(Action.DIG_AIRFLOW, encoded.toString());
    }
    @Override public void tick() {
        super.tick(); ticks++;
        if (inFlight == null && !pending.isEmpty() && ticks % 2 == 0) {
            var encoded = new StringBuilder(); sent = 0;
            for (var stroke : pending) {
                if (sent++ == 32) { sent = 32; break; }
                encoded.append(stroke.from()).append(':').append(stroke.to()).append(';');
            }
            inFlight = send(Action.DIG_AIRFLOW, encoded.toString());
        }
        if (reset != null) reset.setEnabled(!board.isEmpty() && !won() && inFlight == null && pending.isEmpty());
    }

    @Override public void addUIElements() {
        canvas.setPosAndSize(10, 21, AirflowGame.WIDTH * CELL, AirflowGame.HEIGHT * CELL); add(canvas);
        reset = new JournalButton(this, t("research.airflow_reset"), false, () -> {
            canvas.last = -1; send(Action.RESET_AIRFLOW, "");
        });
        reset.setPosAndSize(1, 140, 104, 20); add(reset);
        var back = new JournalButton(this, t("research.airflow_back"), false, desk::closeAirflow);
        back.setPosAndSize(109, 140, 104, 20); add(back);
    }
    @Override public void alignWidgets() {}
    @Override public void render(GuiGraphics g, int x, int y, int w, int h, RenderingHint hint) {
        super.render(g, x, y, w, h, hint);
        var title = t("research.airflow_start");
        g.drawString(getFont(), title, x + (w - getFont().width(title)) / 2, y + 3, INK, false);
        String[] labels = {"airflow_inlet", "airflow_bed", "airflow_outlet", "airflow_soil", "airflow_wall"};
        int[] colors = {WIND, won() ? 0xFFE6A34D : 0xFF68443A, WIND, 0xFFA57450, 0xFF77766B};
        for (int i = 0; i < labels.length; i++) {
            int ly = y + 24 + i * 14;
            g.fill(x + 133, ly + 1, x + 139, ly + 7, colors[i]);
            g.drawString(getFont(), t("research." + labels[i]), x + 143, ly, INK, false);
        }
        int ly = y + 99;
        for (var line : getFont().split(t(won() ? "research.state.completed" : "research.airflow_hint"), 77)) {
            g.drawString(getFont(), line, x + 133, ly, MUTED, false); ly += 10;
        }
    }

    private final class DigCanvas extends UIElement {
        private int last = -1;
        private boolean dragging;
        private double dragX, dragY;
        private DigCanvas() { super(AirflowDeskPanel.this); }
        private int hovered() {
            int x = (int)Math.floor(getMouseX() / CELL), y = (int)Math.floor(getMouseY() / CELL);
            return x < 0 || x >= AirflowGame.WIDTH || y < 0 || y >= AirflowGame.HEIGHT ? -1 : y * AirflowGame.WIDTH + x;
        }
        private void dig() {
            int cx = (int)Math.floor(dragX / CELL), cy = (int)Math.floor(dragY / CELL);
            int current = cx < 0 || cx >= AirflowGame.WIDTH || cy < 0 || cy >= AirflowGame.HEIGHT ? -1 : cy * AirflowGame.WIDTH + cx;
            if (current < 0) { last = -1; return; }
            queue(last < 0 ? current : last, current); last = current;
        }
        @Override public boolean onMousePressed(MouseButton button) {
            if (button != MouseButton.LEFT || !isMouseOver() || board.isEmpty() || won()) return false;
            dragging = true; last = -1; dragX = getMouseX(); dragY = getMouseY(); dig(); return true;
        }
        @Override public boolean onMouseDragged(MouseButton button, double dx, double dy) {
            if (button != MouseButton.LEFT || !dragging) return false;
            dragX += dx; dragY += dy;
            dig(); return true;
        }
        @Override public void onMouseReleased(MouseButton button) {
            if (button == MouseButton.LEFT) { dragging = false; last = -1; }
        }
        @Override public void render(GuiGraphics g, int x, int y, int w, int h, RenderingHint hint) {
            g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF6A5947);
            int[] path = board.getIntArray("path");
            int head = won() ? Math.min(path.length, Math.max(0, ticks - wonAt) * 2) : 0;
            boolean hot = won() && path.length == 0;
            for (int i = 0; i < head; i++) if (AirflowGame.terrain(path[i]) == AirflowGame.BED) { hot = true; break; }
            for (int cell = 0; cell < AirflowGame.SIZE; cell++) {
                int bx = x + cell % AirflowGame.WIDTH * CELL, by = y + cell / AirflowGame.WIDTH * CELL;
                int type = preview.cell(cell);
                int mixed = cell * 0x45D9F3B, noise = (mixed ^ mixed >>> 16) & 7;
                int color = type == AirflowGame.SOIL ? (noise < 2 ? 0xFF9F704E : noise < 4 ? 0xFFB3845B : 0xFFAA7953)
                    : type == AirflowGame.WALL ? 0xFF77766B : type == AirflowGame.BED ? (hot ? 0xFFD28A3F : 0xFF68443A) : 0xFFE1D2AF;
                g.fill(bx, by, bx + CELL, by + CELL, color);
                if (type == AirflowGame.WALL) {
                    if (cell / AirflowGame.WIDTH % 2 == 0) g.fill(bx, by, bx + CELL, by + 1, 0xFF55564F);
                    if ((cell % AirflowGame.WIDTH + cell / AirflowGame.WIDTH / 2) % 3 == 0) g.fill(bx, by, bx + 1, by + CELL, 0xFF55564F);
                } else if (type == AirflowGame.BED) {
                    g.fill(bx + 1, by + 1, bx + 3, by + 3, hot ? 0xFFFFD078 : 0xFF372E2A);
                } else if (type == AirflowGame.SOIL && noise == 0) g.fill(bx + 1, by + 1, bx + 2, by + 2, 0xFFC19469);
            }
            // Particles follow the verified route, not dead-end branches or filled cavities.
            boolean passedBed = false;
            for (int i = 0; i < head; i++) {
                int cell = path[i]; passedBed |= AirflowGame.terrain(cell) == AirflowGame.BED;
                if ((i - ticks / 2) % 7 != 0) continue;
                int px = x + cell % AirflowGame.WIDTH * CELL + 1, py = y + cell / AirflowGame.WIDTH * CELL + 1;
                g.fill(px, py, px + 2, py + 2, passedBed ? 0xFFE6AD62 : WIND);
            }
            int iy = y + 20 * CELL + 2, ox = x + 18 * CELL + 2;
            g.fill(x - 7, iy - 1, x + 7, iy + 1, WIND);
            g.fill(x + 1, iy - 4, x + 3, iy + 4, WIND);
            g.fill(x + 3, iy - 2, x + 5, iy + 2, WIND);
            g.fill(ox - 1, y - 5, ox + 1, y + 7, WIND);
            g.fill(ox - 2, y - 3, ox + 2, y - 1, WIND);
            g.fill(ox - 4, y - 1, ox + 4, y + 1, WIND);
            if (!won()) {
                int px = x + (ticks / 2 % 10);
                g.fill(px, iy - 1, px + 2, iy + 1, 0xFFC4EBEC);
            }
            if (isMouseOver() && !won() && !board.isEmpty()) {
                int cell = hovered();
                if (cell >= 0 && preview.cell(cell) != AirflowGame.WALL) {
                    int bx = x + cell % AirflowGame.WIDTH * CELL, by = y + cell / AirflowGame.WIDTH * CELL;
                    g.renderOutline(bx - CELL, by - CELL, CELL * 3, CELL * 3, 0xB0FFF0CA);
                }
            }
        }
    }
}
