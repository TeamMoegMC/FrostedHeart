package com.teammoeg.frostedresearch.gui.drawdesk;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.frostedresearch.FRNetwork;
import com.teammoeg.frostedresearch.knowledge.client.ui.JournalButton;
import com.teammoeg.frostedresearch.knowledge.game.FoldlineGame;
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

public final class FoldlineDeskPanel extends DeskMinigamePanel {
    private static final int CELL = 17;
    private static final int[] COLORS = {0xFF99471D, 0xFF147381, 0xFF78449A};
    private final DrawDeskScreen desk;
    private final String run, task;
    private final Component title;
    private final ArrayDeque<String> undo = new ArrayDeque<>();
    private UUID pending;
    private FoldlineGame game;
    private String mirrors = "";
    private boolean done, unavailable;
    private int focus = -1;
    private JournalButton stamp, revert, reset, reroll;

    public FoldlineDeskPanel(DrawDeskLayer parent, DrawDeskScreen desk, String run, String task, Component title) {
        super(parent);
        this.desk = desk; this.run = run; this.task = task; this.title = title;
        setPosAndSize(165, 25, 213, 163);
    }
    public Component projectTitle() { return title; }
    public String statusKey() { return "research.foldline_controls"; }
    public void start() { send(Action.START_FOLDLINE, ""); }
    private void send(Action action, String value) {
        if (pending != null) return;
        String previous = game == null ? "" : game.seed + "/" + mirrors;
        var packet = new KnowledgeWorkbenchActionPacket(desk.getTile().getBlockPos(), action, List.of(), run, previous, value, task);
        pending = packet.requestId();
        FRNetwork.INSTANCE.sendToServer(packet);
    }
    public void receive(CompoundTag payload) {
        if (!payload.hasUUID("request") || !payload.getUUID("request").equals(pending)) return;
        pending = null;
        var paper = payload.getCompound("foldline");
        unavailable = !paper.contains("seed");
        if (unavailable) { game = null; return; }
        if (game == null || game.seed != paper.getLong("seed") || game.level != paper.getInt("level")) {
            game = new FoldlineGame(paper.getInt("level"), paper.getLong("seed"));
            focus = -1;
            undo.clear();
        }
        mirrors = paper.getString("mirrors");
        done = paper.getBoolean("done");
    }
    private void edit(String next) {
        if (pending != null || done || next.equals(mirrors)) return;
        undo.push(mirrors);
        send(Action.EDIT_FOLDLINE, next);
    }
    private JournalButton button(String key, int x, int y, int width, Runnable action) {
        var b = new JournalButton(this, t("research." + key), false, action);
        b.setPosAndSize(x, y, width, 16); add(b);
        return b;
    }
    @Override public void addUIElements() {
        var grid = new Grid();
        grid.setPosAndSize(2, 15, CELL * 7, CELL * 7); add(grid);
        revert = button("foldline_undo", 129, 64, 79, () -> {
            if (pending == null && !undo.isEmpty()) send(Action.EDIT_FOLDLINE, undo.pop());
        });
        reset = button("foldline_reset", 129, 82, 79, () -> edit(game.draft));
        reroll = button("foldline_reroll", 129, 100, 79, () -> send(Action.REROLL_FOLDLINE, ""));
        stamp = button("foldline_stamp", 2, 142, 100, () -> send(Action.STAMP_FOLDLINE, ""));
        button("airflow_back", 108, 142, 100, desk::closeDeskGame);
    }
    @Override public void alignWidgets() {}
    @Override public void tick() {
        super.tick();
        if (stamp == null) return;
        boolean edit = game != null && !done && pending == null;
        stamp.setEnabled(edit && game.solved(mirrors)); revert.setEnabled(edit && !undo.isEmpty());
        reroll.setEnabled(edit); reset.setEnabled(edit && !mirrors.equals(game.draft));
    }
    @Override public void render(GuiGraphics g, int x, int y, int w, int h, RenderingHint rh) {
        super.render(g,x,y,w,h,rh);
        g.drawString(getFont(), t("research.foldline_start"), x+4,y+1, INK,false);
        if (game == null) {
            g.drawString(getFont(), t(unavailable ? "research.unchanged" : "research.foldline_loading"), x+4,y+24,MUTED,false);
            return;
        }
        int used = FoldlineGame.used(mirrors);
        g.drawString(getFont(), t("research.foldline_budget", used, game.budget), x+130,y+4,used>game.budget ? COLORS[0] : INK,false);
        for (int i=0;i<game.entries.length;i++) {
            var ok = game.trace(mirrors,i,null)==game.exits[i];
            g.drawString(getFont(), t(ok ? "research.foldline_connected" : "research.foldline_open", String.valueOf((char)('A'+i))), x+130,y+19+i*12,COLORS[i],false);
        }
        if (done) g.drawString(getFont(), t("research.foldline_won"), x+3,y+132,INK,false);
    }
    private final class Grid extends UIElement {
        Grid() { super(FoldlineDeskPanel.this); }
        @Override public boolean onMousePressed(MouseButton button) {
            if (!isMouseOver() || game==null || (button!=MouseButton.LEFT && button!=MouseButton.RIGHT)) return false;
            int cell=(int)(getMouseX()/CELL)+7*(int)(getMouseY()/CELL);
            for(int i=0;i<game.entries.length;i++) if(cell==game.entries[i] || cell==game.exits[i]) {
                focus=focus==i ? -1 : i; return true;
            }
            if(done || pending!=null) return true;
            for(int i=0;i<game.pads.length;i++) if(cell==game.pads[i]) {
                char[] next=mirrors.toCharArray();
                next[i]=button==MouseButton.RIGHT ? '0' : (char)('0'+(next[i]-'0'+1)%3);
                edit(new String(next)); break;
            }
            return true;
        }
        private int cx(int x,int cell) { return x+CELL*(cell%7)+CELL/2; }
        private int cy(int y,int cell) { return y+CELL*(cell/7)+CELL/2; }
        @Override public void render(GuiGraphics g,int x,int y,int w,int h,RenderingHint rh) {
            if(game==null) return;
            g.fill(x,y,x+w,y+h,0xFFECE0C3);
            for(int i=1;i<=5;i++) {
                int d=i*CELL+CELL/2;
                g.hLine(x+CELL,x+w-CELL,y+d,0xFFD3C8AE);
                g.vLine(x+d,y+CELL,y+h-CELL,0xFFD3C8AE);
            }
            for(int ray=0;ray<game.entries.length;ray++) {
                if(focus>=0 && focus!=ray) continue;
                int[] previous={game.entries[ray]}; int color=COLORS[ray];
                game.trace(mirrors,ray,p->{
                    int a=previous[0];
                    if(p!=a) {
                        if(p/7==a/7) g.hLine(cx(x,a),cx(x,p),cy(y,p),color);
                        else g.vLine(cx(x,p),cy(y,a),cy(y,p),color);
                    }
                    previous[0]=p;
                });
            }
            for(int i=0;i<game.pads.length;i++) {
                int xx=cx(x,game.pads[i]), yy=cy(y,game.pads[i]);
                g.renderOutline(xx-6,yy-6,13,13,0xFF9D927C);
                char mirror=mirrors.charAt(i);
                if(mirror!='0') for(int j=-4;j<=4;j++) {
                    int dy=mirror=='1' ? -j : j; g.fill(xx+j,yy+dy,xx+j+1,yy+dy+2,INK);
                }
            }
            for(int i=0;i<game.entries.length;i++) {
                port(g,x,y,game.entries[i],i,true);port(g,x,y,game.exits[i],i,false);
            }
        }
        private void port(GuiGraphics g,int x,int y,int cell,int ray,boolean source) {
            int xx=cx(x,cell), yy=cy(y,cell), color=COLORS[ray];
            if(source) g.fill(xx-6,yy-6,xx+7,yy+7,color);
            else g.renderOutline(xx-6,yy-6,13,13,color);
            g.drawString(getFont(),String.valueOf((char)('A'+ray)),xx-3,yy-4,source ? 0xFFFFF1D3 : color,false);
        }
    }
}
