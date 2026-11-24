package com.teammoeg.frostedresearch.gui.drawdesk;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.frostedresearch.FRNetwork;
import com.teammoeg.frostedresearch.knowledge.client.ui.JournalButton;
import com.teammoeg.frostedresearch.knowledge.game.AirflowGame;
import com.teammoeg.frostedresearch.knowledge.game.AirflowParticles;
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
public final class AirflowDeskPanel extends DeskMinigamePanel {
    private static final int CELL = 4, WIND = 0xFF56B9D3;
    private final DrawDeskScreen desk;
    private final String run, task;
    private final Component projectTitle;
    private final AirflowGame preview = new AirflowGame();
    private final AirflowParticles gas = new AirflowParticles();
    private final ArrayDeque<AirflowGame.Stroke> pending = new ArrayDeque<>();
    private final DigCanvas canvas = new DigCanvas();
    private CompoundTag board = new CompoundTag();
    private UUID inFlight;
    private int sent, ticks;
    private JournalButton reset;

    public AirflowDeskPanel(DrawDeskLayer parent, DrawDeskScreen desk, String run, String task, Component projectTitle) {
        super(parent);
        this.desk = desk; this.run = run; this.task = task; this.projectTitle = projectTitle;
        setPosAndSize(165, 25, 218, 164);
    }

    public Component projectTitle() { return projectTitle; }
    public CompoundTag board() { return board.copy(); }
    public boolean won() { return board.getBoolean("won"); }
    public boolean failed() { return board.getBoolean("failed"); }
    public String statusKey() {
        return failed() ? (AirflowGame.chimneyOpening(board.getInt("leak")) ? "research.airflow_bypass" : "research.airflow_failed")
            : won() ? "research.airflow_won" : "research.airflow_explain";
    }
    public void start() { send(Action.START_AIRFLOW, ""); }
    public void receive(CompoundTag payload) {
        if (inFlight != null && inFlight.equals(payload.getUUID("request"))) {
            for (int i = 0; i < sent && !pending.isEmpty(); i++) pending.removeFirst();
            inFlight = null; sent = 0;
        }
        if (!payload.contains("airflow")) return;
        board = payload.getCompound("airflow").copy();
        if (payload.getString("action").equals("RESET_AIRFLOW")) gas.clear();
        preview.load(board);
        if (won() || failed()) pending.clear();
        else preview.dig(List.copyOf(pending));
    }
    private UUID send(Action action, String strokes) {
        var packet = new KnowledgeWorkbenchActionPacket(desk.getTile().getBlockPos(), action,
            List.of(), run, "", strokes, task);
        FRNetwork.INSTANCE.sendToServer(packet); return packet.requestId();
    }
    private void queue(int from, int to) {
        if (board.isEmpty() || won() || failed()) return;
        int before = preview.dugCount();
        pending.addLast(new AirflowGame.Stroke(from, to));
        preview.load(board); preview.dig(List.copyOf(pending));
        if (preview.dugCount() == before) pending.removeLast();
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
        if (!board.isEmpty()) gas.tick(preview);
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
        var back = new JournalButton(this, t("research.airflow_back"), false, desk::closeDeskGame);
        back.setPosAndSize(109, 140, 104, 20); add(back);
    }
    @Override public void alignWidgets() {}
    @Override public void render(GuiGraphics g, int x, int y, int w, int h, RenderingHint hint) {
        super.render(g, x, y, w, h, hint);
        var title = t("research.airflow_start");
        g.drawString(getFont(), title, x + (w - getFont().width(title)) / 2, y + 3, INK, false);
        String[] labels = {"airflow_inlet", "airflow_bed", "airflow_outlet", "airflow_soil"};
        int[] colors = {WIND, won() ? 0xFFE6A34D : 0xFF9C5034, WIND, 0xFFB6A06C};
        for (int i = 0; i < labels.length; i++) {
            int ly = y + 24 + i * 14;
            g.fill(x + 133, ly + 1, x + 139, ly + 7, colors[i]);
            g.drawString(getFont(), t("research." + labels[i]), x + 143, ly, INK, false);
        }
        int ly = y + 86;
        for (var line : getFont().split(t(failed() ? "research.airflow_leak" : won() ? "research.state.completed" : "research.airflow_hint"), 77)) {
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
            if (button != MouseButton.LEFT || !isMouseOver() || board.isEmpty() || won() || failed()) return false;
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
            boolean hot = won();
            for (int cell = 0; cell < AirflowGame.SIZE; cell++) {
                int cx = cell % AirflowGame.WIDTH, cy = cell / AirflowGame.WIDTH;
                int bx = x + cx * CELL, by = y + cy * CELL, type = preview.cell(cell);
                int brick = (cx + cy / 2 % 2 * 2) / 4 + cy / 2 * 13;
                int mixed = brick * 0x45D9F3B, noise = (mixed ^ mixed >>> 16) & 3;
                int color = type == AirflowGame.BRICK || type == AirflowGame.PROTECTED
                    ? (noise == 0 ? 0xFFB7A16D : noise == 1 ? 0xFFC0AB79 : 0xFFB29A65)
                    : type == AirflowGame.BED ? (hot ? 0xFFB97835 : 0xFF49332C) : 0xFFE1D2AF;
                g.fill(bx, by, bx + CELL, by + CELL, color);
                if (type == AirflowGame.BRICK || type == AirflowGame.PROTECTED) {
                    // The inlet rim uses the same bricks, with no special frame or legend.
                    if (cy % 2 == 0) g.fill(bx, by, bx + CELL, by + 1, 0xFF87754E);
                    if ((cx + cy / 2 % 2 * 2) % 4 == 0) g.fill(bx, by, bx + 1, by + CELL, 0xFF87754E);
                } else if (type == AirflowGame.BED) {
                    int pulse = (int)((Math.sin(ticks * .16 + cell * 1.7) + 1) * 13);
                    int ember = hot ? 0xFFFFB556 + (pulse << 8) : 0xFF873D22 + (pulse << 16) + (pulse / 2 << 8);
                    g.fill(bx + 1, by + 1, bx + 3, by + 3, ember);
                }
            }
            for (var particle : gas.particles()) {
                double alpha=Math.max(0,Math.min(1,getPartialTick()));
                int px = x + (int)((particle.previousX+(particle.x-particle.previousX)*alpha)*CELL);
                int py = y + (int)((particle.previousY+(particle.y-particle.previousY)*alpha)*CELL);
                g.fill(px, py, px + 1, py + 1, particle.heated ? 0xFFF0AC5A : WIND);
            }
            if (failed()) {
                int leak = board.getInt("leak"), lx = leak % AirflowGame.WIDTH, ly = leak / AirflowGame.WIDTH;
                int bx = x + lx * CELL, by = y + ly * CELL;
                g.renderOutline(bx - 2, by - 2, CELL + 4, CELL + 4, ticks % 16 < 8 ? 0xFFF2A16C : 0xFFC85A3B);
                int dx = lx == 0 ? -1 : lx == AirflowGame.WIDTH - 1 ? 1 : 0;
                int dy = ly == 0 ? -1 : ly == AirflowGame.HEIGHT - 1 ? 1 : 0;
                for (int i = 0; i < 6; i++) {
                    int distance = (ticks + i * 5) % 24 / 2;
                    int px = bx + 1 + dx * distance, py = by + 1 + dy * distance;
                    g.fill(px, py, px + 1, py + 1, WIND);
                }
            }
            int iy = y + 20 * CELL + 2, ox = x + 18 * CELL + 2;
            g.fill(x - 7, iy - 1, x + 7, iy + 1, WIND);
            g.fill(x + 1, iy - 4, x + 3, iy + 4, WIND);
            g.fill(x + 3, iy - 2, x + 5, iy + 2, WIND);
            g.fill(ox - 1, y - 5, ox + 1, y + 7, WIND);
            g.fill(ox - 2, y - 3, ox + 2, y - 1, WIND);
            g.fill(ox - 4, y - 1, ox + 4, y + 1, WIND);
            if (isMouseOver() && !won() && !failed() && !board.isEmpty()) {
                int cell = hovered();
                if (cell >= 0 && preview.cell(cell) != AirflowGame.PROTECTED) {
                    int bx = x + cell % AirflowGame.WIDTH * CELL, by = y + cell / AirflowGame.WIDTH * CELL;
                    g.renderOutline(bx - CELL, by - CELL, CELL * 3, CELL * 3, 0xB0FFF0CA);
                }
            }
        }
    }
}
