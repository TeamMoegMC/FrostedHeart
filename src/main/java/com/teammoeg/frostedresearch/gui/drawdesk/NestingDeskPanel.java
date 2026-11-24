package com.teammoeg.frostedresearch.gui.drawdesk;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.frostedresearch.FRNetwork;
import com.teammoeg.frostedresearch.knowledge.client.ui.JournalButton;
import com.teammoeg.frostedresearch.knowledge.game.NestingGame;
import com.teammoeg.frostedresearch.knowledge.network.KnowledgeWorkbenchActionPacket;
import com.teammoeg.frostedresearch.knowledge.network.KnowledgeWorkbenchActionPacket.Action;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.List;
import java.util.UUID;
import static com.teammoeg.frostedresearch.knowledge.client.KnowledgePresentation.t;
import static com.teammoeg.frostedresearch.knowledge.client.ui.KnowledgeUiTheme.*;

public final class NestingDeskPanel extends DeskMinigamePanel {
    private static final int CELL = 17, BOARD_X = 2, BOARD_Y = 5, TRAY_X = 111, SLOT_W = 47, SLOT_H = 30;
    private static final int[] COLORS = {0xFFBB7953,0xFF6899A4,0xFFC2A35C,0xFF82976A,0xFFA087A0,0xFF699A92};
    private final DrawDeskScreen desk;
    private final String run, task;
    private final Component title;
    private final boolean fill;
    private final ArrayDeque<String> history = new ArrayDeque<>();
    private UUID pending;
    private Long seed;
    private NestingGame game;
    private String layout = "", expectedEdit;
    private boolean done, dragging, unavailable;
    private int selected = -1, rotation, anchor;
    private JournalButton rotate, undo, shuffle, stamp;

    NestingDeskPanel(DrawDeskLayer parent, DrawDeskScreen desk, String run, String task, Component title, boolean fill) {
        super(parent);
        this.desk=desk; this.run=run; this.task=task; this.title=title; this.fill=fill;
        setPosAndSize(165,25,213,163);
    }
    public Component projectTitle() { return title; }
    public String statusKey() { return "research.nesting_controls"; }
    public void start() { send(Action.START_NESTING,""); }
    private boolean editable() { return game!=null && !done && pending==null; }

    private void send(Action action,String value) {
        if(pending!=null) return;
        var packet=new KnowledgeWorkbenchActionPacket(desk.getTile().getBlockPos(),action,List.of(),run,
                seed==null?"":seed+"/"+layout,value,task);
        pending=packet.requestId(); FRNetwork.INSTANCE.sendToServer(packet);
    }
    public void receive(CompoundTag response) {
        if(!response.hasUUID("request") || !response.getUUID("request").equals(pending)) return;
        pending=null;
        var paper=response.getCompound("nesting");
        unavailable=!paper.contains("seed") || !(fill?NestingGame.FILL:NestingGame.PACK).toString().equals(paper.getString("game"));
        if(unavailable) {game=null;selected=-1;return;}
        if(seed==null || seed!=paper.getLong("seed")) {
            seed=paper.getLong("seed");game=new NestingGame(fill,seed);selected=-1;history.clear();
        }
        layout=paper.getString("pieces");
        if(expectedEdit!=null && !expectedEdit.equals(layout)) history.clear();
        expectedEdit=null;done=paper.getBoolean("done");
    }
    private void edit(String next) {
        if(!editable() || !game.valid(next)) return;
        selected=-1;dragging=false;
        if(next.equals(layout)) return;
        history.push(layout);expectedEdit=next;send(Action.EDIT_NESTING,next);
    }
    private JournalButton button(String key,int x,int y,int w,Runnable callback) {
        var b=new JournalButton(this,t("research."+key),false,callback);
        b.setPosAndSize(x,y,w,17);add(b);return b;
    }
    @Override public void addUIElements() {
        var work=new WorkArea();work.setPosAndSize(0,16,209,93);add(work);
        rotate=button("nesting_rotate",1,115,66,this::rotatePiece);
        undo=button("foldline_undo",71,115,66,()->{
            if(editable() && !history.isEmpty()){selected=-1;expectedEdit=history.pop();send(Action.EDIT_NESTING,expectedEdit);}
        });
        shuffle=button("foldline_reroll",141,115,66,()->send(Action.REROLL_NESTING,""));
        stamp=button("foldline_stamp",1,140,101,()->send(Action.STAMP_NESTING,""));
        button("airflow_back",106,140,101,desk::closeDeskGame);
    }
    private void rotatePiece() { if(selected>=0 && editable()) rotation=(rotation+1)%4; }
    @Override public boolean onKeyPressed(int key,int scan,int mods) {
        if(key==GLFW.GLFW_KEY_R && selected>=0 && editable()){rotatePiece();return true;}
        return super.onKeyPressed(key,scan,mods);
    }
    @Override public void alignWidgets() {}
    @Override public void tick() {
        super.tick();if(stamp==null)return;
        shuffle.setEnabled(editable());rotate.setEnabled(editable()&&selected>=0);
        undo.setEnabled(editable()&&!history.isEmpty());stamp.setEnabled(editable()&&game.solved(layout));
    }
    @Override public void render(GuiGraphics g,int x,int y,int w,int h,RenderingHint rh) {
        super.render(g,x,y,w,h,rh);
        g.drawString(getFont(),t(fill?"research.nesting_fill_start":"research.nesting_pack_start"),x+2,y+2,INK,false);
        if(game==null){g.drawString(getFont(),t(unavailable?"research.unchanged":"research.foldline_loading"),x+4,y+32,MUTED,false);return;}
        int placed=0;
        for(int i=0;i<game.shapes.length;i++) if(NestingGame.at(layout,i)!=0)placed++;
        Component progress=fill?t("research.nesting_cells",Integer.bitCount(game.occupied(layout)),Integer.bitCount(game.groove)):
                t("research.nesting_parts",placed,game.shapes.length);
        g.drawString(getFont(),done?t("research.nesting_done"):progress,x+112,y+2,INK,false);
    }

    private final class WorkArea extends UIElement {
        private int startX,startY;
        WorkArea(){super(NestingDeskPanel.this);}
        private int boardCell(int mx,int my) {
            int x=mx-BOARD_X,y=my-BOARD_Y;
            if(x<0||y<0||x>=6*CELL||y>=5*CELL)return -1;
            return x/CELL+6*(y/CELL);
        }
        private int slot(int x,int y) {
            if(x<TRAY_X||x>=TRAY_X+2*SLOT_W||y<0||y>=3*SLOT_H)return -1;
            int i=(x-TRAY_X)/SLOT_W+2*(y/SLOT_H);
            return i<game.shapes.length?i:-1;
        }
        private int occupant(int cell) {
            if(cell<0)return -1;
            for(int i=0;i<game.shapes.length;i++)
                if((game.mask(i,NestingGame.at(layout,i)) & (1<<cell))!=0)return i;
            return -1;
        }
        private int aimed(int cell) {
            int offset=game.shapes[selected][rotation][anchor];
            return NestingGame.placement(cell%6-offset%6,cell/6-offset/6,rotation);
        }
        private void drop(int cell) {
            if(cell<0 || selected<0)return;
            int p=aimed(cell);
            if(p>0)edit(NestingGame.change(layout,selected,p));
        }
        @Override public boolean onMousePressed(MouseButton button) {
            if(!isMouseOver()||!editable())return false;
            startX=(int)Math.floor(getMouseX());startY=(int)Math.floor(getMouseY());dragging=false;
            int cell=boardCell(startX,startY),inside=occupant(cell),rack=slot(startX,startY);
            if(button==MouseButton.RIGHT) {
                if(inside>=0)edit(NestingGame.change(layout,inside,0));
                else selected=-1;
                return true;
            }
            if(button!=MouseButton.LEFT)return false;
            if(rack>=0 || (selected<0&&inside>=0)){
                selected=rack>=0?rack:inside;
                int p=NestingGame.at(layout,selected);
                rotation=p==0?0:NestingGame.rotation(p);anchor=0;
                if(rack<0){
                    int[] shape=game.shapes[selected][rotation];
                    while(shape[anchor]+NestingGame.origin(p)!=cell)anchor++;
                }
                dragging=true;return true;
            }
            if(selected>=0&&cell>=0){drop(cell);return true;}
            return false;
        }
        @Override public void onMouseReleased(MouseButton button) {
            if(button!=MouseButton.LEFT||!dragging)return;
            dragging=false;
            int x=(int)Math.floor(getMouseX()),y=(int)Math.floor(getMouseY());
            if(!editable()||selected<0||Math.abs(x-startX)+Math.abs(y-startY)<3)return;
            if(x>=TRAY_X && isMouseOver() && NestingGame.at(layout,selected)!=0)edit(NestingGame.change(layout,selected,0));
            else drop(boardCell(x,y));
        }
        @Override public void render(GuiGraphics g,int x,int y,int w,int h,RenderingHint rh) {
            if(game==null)return;
            int bx=x+BOARD_X,by=y+BOARD_Y;
            g.fill(bx-1,by-1,bx+6*CELL+1,by+5*CELL+1,0xFFDFD2B0);
            for(int c=0;c<30;c++)if((game.groove & (1<<c))!=0) {
                int xx=bx+c%6*CELL,yy=by+c/6*CELL;
                g.fill(xx,yy,xx+CELL,yy+CELL,0xFF788776);g.fill(xx+1,yy+1,xx+CELL,yy+CELL,0xFFB7C2AB);
            }
            for(int i=0;i<game.shapes.length;i++){
                int pos=NestingGame.at(layout,i), rot=pos==0?0:NestingGame.rotation(pos);
                if(pos!=0){
                    int o=NestingGame.origin(pos);
                    shape(g,game.shapes[i][rot],bx+o%6*CELL,by+o/6*CELL,CELL,selected==i?0xFFB2AE97:COLORS[i]);
                }
                int sx=x+TRAY_X+(i%2)*SLOT_W,sy=y+(i/2)*SLOT_H;
                g.renderOutline(sx,sy,SLOT_W-2,SLOT_H-2,selected==i?0xFF98552C:0xFFA59E86);
                int[] figure=game.shapes[i][selected==i?rotation:rot];
                int width=1,height=1;
                for(int c:figure){width=Math.max(width,c%6+1);height=Math.max(height,c/6+1);}
                int size=Math.min(7,24/height);
                shape(g,figure,sx+(SLOT_W-2-width*size)/2,sy+(SLOT_H-2-height*size)/2,size,pos==0?COLORS[i]:0xFFBDB59F);
            }
            int cell=boardCell((int)Math.floor(getMouseX()),(int)Math.floor(getMouseY()));
            if(selected>=0&&cell>=0&&editable()){
                int p=aimed(cell);
                boolean fits=p>0&&game.valid(NestingGame.change(layout,selected,p));
                int[] offsets=game.shapes[selected][rotation];int a=offsets[anchor];
                for(int c:offsets){
                    int xx=cell%6-a%6+c%6,yy=cell/6-a/6+c/6;
                    if(xx>=0&&xx<6&&yy>=0&&yy<5)g.renderOutline(bx+xx*CELL+1,by+yy*CELL+1,CELL-2,CELL-2,fits?0xFF266D45:0xFFAF3A28);
                }
            }
        }
        private void shape(GuiGraphics g,int[] offsets,int x,int y,int size,int color) {
            for(int c:offsets){
                int xx=x+c%6*size,yy=y+c/6*size;
                g.fill(xx,yy,xx+size,yy+size,color);
                g.fill(xx+1,yy+1,xx+size-1,yy+2,0x44FFFFFF);
                g.fill(xx,yy+size-1,xx+size,yy+size,0x5540362A);
            }
        }
    }
}
