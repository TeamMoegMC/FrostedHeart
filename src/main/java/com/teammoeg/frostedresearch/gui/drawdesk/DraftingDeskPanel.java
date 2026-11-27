package com.teammoeg.frostedresearch.gui.drawdesk;

import com.google.gson.JsonParser;
import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.frostedresearch.FRNetwork;
import com.teammoeg.frostedresearch.knowledge.game.DraftingSheet;
import com.teammoeg.frostedresearch.knowledge.network.KnowledgeWorkbenchActionPacket;
import com.teammoeg.frostedresearch.knowledge.network.KnowledgeWorkbenchActionPacket.Action;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.*;
import static com.teammoeg.frostedresearch.knowledge.game.DraftingGeometry.*;

public final class DraftingDeskPanel extends DeskMinigamePanel {
    private enum Tool { CIRCLE, FIXED, SPAN, SEGMENT, LINE, RAY, ANGLE, BOX, POINT, DISTANCE, ERASE, REMOVE }
    private static final Tool[] TOOLS=Tool.values();
    private static final int CX=96,CY=99,HALF=82;
    private static final int LEFT=191, COLUMNS=3, COLUMN_STEP=60, BUTTON_WIDTH=58, BUTTON_HEIGHT=12;
    private static final int TOOL_TOP=20, TOOL_ROW_STEP=14, TOOL_ROWS=(TOOLS.length+COLUMNS-1)/COLUMNS;
    private static final int VALUE_TOP=77, VALUE_HEIGHT=14, VALUE_BUTTON_WIDTH=28;
    private static final int VALUE_LEFT=LEFT+29, VALUE_RIGHT=LEFT+149;
    private static final int EDIT_TOP=97, REVIEW_TOP=112, NOTE_TOP=132, BACK_TOP=174;
    private final DrawDeskScreen desk;
    private final String run, task;
    private final Component title;
    private DraftingSheet sheet;
    private List<Curve> ink=List.of(), gaps=List.of(), green=List.of(), auxiliary=List.of();
    private List<P> snapPoints=List.of();
    private final Deque<List<Curve>> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
    private final List<P> anchors=new ArrayList<>();
    private Tool tool=Tool.CIRCLE;
    private double radius=24, degrees=45;
    private int hint=-1,revision;
    private long remaining=-1;
    private boolean done,checked,showAux=true;
    private String feedback="";
    private UUID pending;

    public DraftingDeskPanel(DrawDeskLayer parent,DrawDeskScreen desk,String run,String task,Component title) {
        super(parent); this.desk=desk; this.run=run; this.task=task; this.title=title;
        setPosAndSize(3,2,381,198);
    }
    private static String tr(String key) { return Component.translatable("drafting.frostedresearch."+key).getString(); }
    public Component projectTitle() { return title; }
    public String statusKey() { return "research.drafting_start"; }
    public void start() { send(Action.OPEN_DRAFT,""); }
    private void send(Action action,String value) {
        if(pending!=null) return;
        var packet=new KnowledgeWorkbenchActionPacket(desk.getTile().getBlockPos(),action,List.of(),run,
            Integer.toString(revision),value,task);
        pending=packet.requestId(); FRNetwork.INSTANCE.sendToServer(packet);
    }
    public void receive(CompoundTag reply) {
        if(!reply.hasUUID("request") || !reply.getUUID("request").equals(pending)) return;
        pending=null;
        var paper=reply.getCompound("draft");
        if(!paper.contains("ink")) { sheet=null; feedback=tr("unavailable"); return; }
        if(paper.contains("sheet")) sheet=DraftingSheet.read(JsonParser.parseString(paper.getString("sheet")).getAsJsonObject());
        ink=DraftingSheet.curves(JsonParser.parseString(paper.getString("ink")).getAsJsonArray());
        revision=paper.getInt("revision"); done=paper.getBoolean("done"); remaining=paper.getLong("remaining");
        rebuild();
    }
    private List<Curve> axes() {
        double e=sheet.extent();
        return List.of(Curve.line(new P(-e,0),new P(e,0)),Curve.line(new P(0,-e),new P(0,e)));
    }
    private void rebuild() {
        gaps=sheet.gaps(ink);
        List<Curve> good=new ArrayList<>(),extra=new ArrayList<>(), guides=new ArrayList<>(axes());
        Set<P> points=new LinkedHashSet<>(sheet.datums());
        for (Curve target : sheet.targets()) {
            points.add(target.at(0));
            points.add(target.at(1));
            for (Curve axis : guides) points.addAll(intersections(target, axis));
        }
        for(Curve c:ink) {
            points.add(c.a()); points.add(c.at(0)); points.add(c.at(1));
            for(Curve g:guides) points.addAll(intersections(c,g));
            guides.add(c);
            for(Curve piece:split(c,sheet.targets(),false))
                (covered(piece,sheet.targets())?good:extra).add(piece);
        }
        snapPoints=List.copyOf(points); green=good; auxiliary=extra;
    }
    @Override public void tick() {
        super.tick();
        if(remaining>0) remaining--;
        if(remaining==0 && !done && pending==null && sheet!=null) send(Action.CHECK_DRAFT,"");
    }
    @Override public void addUIElements() {}
    @Override public void alignWidgets() {}
    private double scale() { return HALF/sheet.extent(); }
    private P mouse() { return new P((getMouseX()-CX)/scale(),(getMouseY()-CY)/scale()); }
    private P nearest(P raw,List<P> points,double tolerance) {
        P best=null;
        for(P p:points) if(raw.dist(p)<=tolerance) { best=p; tolerance=raw.dist(p); }
        return best;
    }
    private P locate(P raw) {
        double tol=3.1/scale();
        P p=nearest(raw,snapPoints,tol);
        if(p!=null) return p;
        if(hint>=0) {
            p=nearest(raw,sheet.hints().get(hint).points(),tol);
            if(p!=null) return p;
        }
        List<P> projections=new ArrayList<>();
        for(Curve c:ink) projections.add(c.nearest(raw));
        for(Curve c:axes()) projections.add(c.nearest(raw));
        p=nearest(raw,projections,tol*.7);
        if(p!=null) return p;
        projections.clear();
        for(Curve c:sheet.targets()) {
            projections.add(c.at(0)); projections.add(c.at(1)); projections.add(c.nearest(raw));
        }
        p=nearest(raw,projections,tol*.55);
        return p!=null?p:raw;
    }
    private void update(List<Curve> next) {
        String value=DraftingSheet.encode(next);
        if(value.length()>32767) { feedback=tr("full"); return; }
        checked=false;
        send(Action.EDIT_DRAFT,value);
    }
    private void edit(List<Curve> next) {
        if(ink.equals(next)) return;
        undo.push(ink); redo.clear(); update(next);
    }
    // Clip the infinite ruler at the square paper boundary.
    private Curve ruled(P a,P b) {
        if(tool==Tool.SEGMENT) return Curve.line(a,b);
        P d=b.minus(a); double lo=tool==Tool.RAY?0:Double.NEGATIVE_INFINITY,hi=Double.POSITIVE_INFINITY;
        for(int axis=0;axis<2;axis++) {
            double origin=axis==0?a.x():a.y(), step=axis==0?d.x():d.y();
            if(Math.abs(step)<1e-10) continue;
            double t1=(-sheet.extent()-origin)/step,t2=(sheet.extent()-origin)/step;
            lo=Math.max(lo,Math.min(t1,t2)); hi=Math.min(hi,Math.max(t1,t2));
        }
        return Curve.line(a.plus(d.times(lo)),a.plus(d.times(hi)));
    }
    private void erase(P p) {
        List<Curve> boundaries=new ArrayList<>(ink); boundaries.addAll(sheet.targets()); boundaries.addAll(axes());
        int index=-1; Curve hit=null; List<Curve> pieces=List.of(); double best=3/scale();
        for(int i=0;i<ink.size();i++) {
            Curve c=ink.get(i);
            if(c.nearest(p).dist(p)>best) continue;
            var parts=tool==Tool.REMOVE?List.of(c):split(c,boundaries,true);
            for(Curve part:parts) {
                double dist=part.nearest(p).dist(p);
                if(dist<=best) { best=dist;index=i;hit=part;pieces=parts; }
            }
        }
        if(index<0) return;
        List<Curve> next=new ArrayList<>(ink); next.remove(index);
        for(Curve part:pieces) if(part!=hit) next.add(part);
        edit(next);
    }
    private void drawAt(P raw) {
        if(tool==Tool.ERASE || tool==Tool.REMOVE) { erase(raw); return; }
        P p=locate(raw);
        List<Curve> next=new ArrayList<>(ink);
        if(tool==Tool.FIXED) next.add(Curve.arc(p,radius,0,360));
        else if(tool==Tool.POINT) next.add(Curve.line(p,p));
        else if(anchors.isEmpty()) { anchors.add(p); return; }
        else {
            P a=anchors.get(0);
            if(p.dist(a)<EPS) return;
            switch(tool) {
                case CIRCLE -> {
                    radius = a.dist(p);
                    next.add(Curve.arc(a, radius, 0, 360));
                }
                case SEGMENT,LINE,RAY -> next.add(ruled(a,p));
                case DISTANCE -> feedback=String.format(Locale.ROOT,"%.2f mm",p.dist(a));
                case SPAN -> { radius=p.dist(a); tool=Tool.FIXED; }
                case ANGLE -> {
                    double angle=Math.atan2(p.y()-a.y(),p.x()-a.x())+Math.toRadians(degrees);
                    P end=a.plus(new P(Math.cos(angle),Math.sin(angle)).times(a.dist(p)));
                    next.add(Curve.line(a,end));
                }
                case BOX -> {
                    P b=new P(a.x(),p.y()), d=new P(p.x(),a.y());
                    next.addAll(List.of(Curve.line(a,b),Curve.line(b,p),Curve.line(p,d),Curve.line(d,a)));
                }
                default -> {}
            }
        }
        anchors.clear(); edit(next);
    }
    private void adjust(int delta) {
        if(tool==Tool.ANGLE) degrees=(degrees+delta)%360;
        else radius=Math.max(.1,Math.min(sheet.extent()*2,radius+delta));
    }
    @Override public boolean onMouseScrolled(double scroll) {
        if(sheet==null || done || !isMouseOver()) return false;
        if (tool != Tool.ANGLE && tool != Tool.FIXED) return true;
        adjust(scroll>0?1:-1); return true;
    }
    @Override public boolean onMousePressed(MouseButton button) {
        if(!isMouseOver()) return false;
        if(button==MouseButton.RIGHT) { anchors.clear(); feedback=""; return true; }
        if(button!=MouseButton.LEFT) return false;
        int x=(int)getMouseX(), y=(int)getMouseY();
        if(controlColumn(x,y,BACK_TOP)==2) { desk.closeDeskGame(); return true; }
        if(sheet==null || done || pending!=null) return true;
        if(Math.abs(x-CX)<=HALF && Math.abs(y-CY)<=HALF) { drawAt(mouse()); return true; }
        if(y>=TOOL_TOP && y<TOOL_TOP+TOOL_ROWS*TOOL_ROW_STEP) {
            int row=(y-TOOL_TOP)/TOOL_ROW_STEP;
            int column=controlColumn(x,y,TOOL_TOP+row*TOOL_ROW_STEP);
            int index=row*COLUMNS+column;
            if(column>=0 && index<TOOLS.length) {
                tool=TOOLS[index];
                anchors.clear();
                feedback="";
            }
            return true;
        }
        if((tool==Tool.FIXED || tool==Tool.ANGLE) && y>=VALUE_TOP && y<VALUE_TOP+VALUE_HEIGHT) {
            if(x>=VALUE_LEFT && x<VALUE_LEFT+VALUE_BUTTON_WIDTH) adjust(-1);
            if(x>=VALUE_RIGHT-VALUE_BUTTON_WIDTH && x<VALUE_RIGHT) adjust(1);
            return true;
        }
        switch(controlColumn(x,y,EDIT_TOP)) {
            case 0 -> { if(!undo.isEmpty()) { redo.push(ink); update(undo.pop()); } }
            case 1 -> { if(!redo.isEmpty()) { undo.push(ink); update(redo.pop()); } }
            case 2 -> showAux=!showAux;
        }
        switch(controlColumn(x,y,REVIEW_TOP)) {
            case 0 -> { if(!sheet.hints().isEmpty()) { hint=(hint+1)%sheet.hints().size(); feedback=""; } }
            case 1 -> { checked=true; send(Action.CHECK_DRAFT,""); }
            case 2 -> send(Action.TIMER_DRAFT,"");
        }
        return true;
    }
    private static int controlColumn(int x,int y,int top) {
        int offset=x-LEFT;
        if(y<top || y>=top+BUTTON_HEIGHT || offset<0 || offset>=COLUMNS*COLUMN_STEP
            || offset%COLUMN_STEP>=BUTTON_WIDTH) return -1;
        return offset/COLUMN_STEP;
    }
    private int px(double x) { return CX+(int)Math.round(x*scale()); }
    private int py(double y) { return CY+(int)Math.round(y*scale()); }
    private void plot(GuiGraphics g,int x,int y,Curve c,int color,boolean rainbow) {
        int steps=Math.max(1,(int)Math.ceil(c.length()*scale()*1.5));
        for(int i=0;i<=steps;i++) {
            P p=c.at((double)i/steps); int xx=px(p.x()),yy=py(p.y());
            if(Math.abs(xx-CX)>HALF || Math.abs(yy-CY)>HALF) continue;
            int tint=rainbow?java.awt.Color.HSBtoRGB((System.currentTimeMillis()%4000)/4000f+(xx+yy)/110f,.8f,.82f):color;
            g.fill(x+xx,y+yy,x+xx+1,y+yy+1,tint);
        }
    }
    private void centeredText(GuiGraphics g,String text,int x,int y,int width,int color) {
        g.drawString(getFont(),text,x+(width-getFont().width(text))/2,y,color,false);
    }
    private void drawTool(GuiGraphics g,int x,int y,Tool entry) {
        int column=entry.ordinal()%COLUMNS;
        int top=TOOL_TOP+entry.ordinal()/COLUMNS*TOOL_ROW_STEP;
        int left=x+LEFT+column*COLUMN_STEP;
        boolean selected=entry==tool;
        boolean hovered=controlColumn((int)getMouseX(),(int)getMouseY(),top)==column;
        int background=selected?0xff245966:hovered?0xffe5d8be:0xffd7cbb5;
        g.fill(left,y+top,left+BUTTON_WIDTH,y+top+BUTTON_HEIGHT,background);
        g.fill(left,y+top+BUTTON_HEIGHT-1,left+BUTTON_WIDTH,y+top+BUTTON_HEIGHT,selected?0xff73a7ae:0xffa99b81);
        centeredText(g,tr(entry.name().toLowerCase(Locale.ROOT)),left,y+top+2,BUTTON_WIDTH,selected?0xffffffff:0xff393026);
    }
    private void drawValueControl(GuiGraphics g,int x,int y) {
        int left=x+VALUE_LEFT, right=x+VALUE_RIGHT, top=y+VALUE_TOP;
        g.fill(left,top,right,top+VALUE_HEIGHT,0xfff4eddb);
        g.fill(left+1,top+1,left+VALUE_BUTTON_WIDTH,top+VALUE_HEIGHT-1,0xffd5c09b);
        g.fill(right-VALUE_BUTTON_WIDTH,top+1,right-1,top+VALUE_HEIGHT-1,0xffd5c09b);
        g.renderOutline(left,top,right-left,VALUE_HEIGHT,0xff8a785b);
        g.vLine(left+VALUE_BUTTON_WIDTH,top+1,top+VALUE_HEIGHT-2,0xffad9b7d);
        g.vLine(right-VALUE_BUTTON_WIDTH-1,top+1,top+VALUE_HEIGHT-2,0xffad9b7d);
        String value=tool==Tool.ANGLE?String.format(Locale.ROOT,"%.1f°",degrees):String.format(Locale.ROOT,"R%.1f",radius);
        centeredText(g,"−",left,top+3,VALUE_BUTTON_WIDTH,0xff393026);
        centeredText(g,value,left,top+3,right-left,0xff393026);
        centeredText(g,"+",right-VALUE_BUTTON_WIDTH,top+3,VALUE_BUTTON_WIDTH,0xff393026);
    }
    private void drawAction(GuiGraphics g,int x,int y,int top,int column,String text,boolean active) {
        int left=x+LEFT+column*COLUMN_STEP;
        boolean hovered=controlColumn((int)getMouseX(),(int)getMouseY(),top)==column;
        int background=hovered?0xffebddc2:active?0xffd8e3d4:0xffdbcdb2;
        g.fill(left,y+top,left+BUTTON_WIDTH,y+top+BUTTON_HEIGHT,background);
        g.renderOutline(left,y+top,BUTTON_WIDTH,BUTTON_HEIGHT,active?0xff477577:0xff928062);
        centeredText(g,text,left,y+top+2,BUTTON_WIDTH,active?0xff234f54:0xff49382b);
    }
    @Override public void render(GuiGraphics g,int x,int y,int w,int h,RenderingHint rh) {
        DrawDeskIcons.DIALOG.draw(g,x,y,w,h);
        drawAction(g,x,y,BACK_TOP,2,tr("back"),false);
        if(sheet==null) { g.drawString(getFont(),feedback,x+9,y+8,0xff453a31,false); return; }
        g.drawString(getFont(),Component.translatable(sheet.title()),x+14,y+7,0xff342d27,false);
        g.drawString(getFont(),sheet.dimensions(),x+LEFT,y+7,0xff493c30,false);
        g.renderOutline(x+CX-HALF-1,y+CY-HALF-1,2*HALF+3,2*HALF+3,0xff937951);
        g.fill(x+CX-HALF,y+CY-HALF,x+CX+HALF+1,y+CY+HALF+1,0xfffaf6e9);
        g.drawManaged(() -> drawGeometry(g,x,y));
        drawControls(g,x,y);
    }
    private void drawGeometry(GuiGraphics g,int x,int y) {
        for(Curve c:axes()) plot(g,x,y,c,0xffc8bdad,false);
        for(Curve c:sheet.targets()) plot(g,x,y,c,0xff9b9486,false);
        if(showAux) for(Curve c:auxiliary) plot(g,x,y,c,0x7752647a,false);
        for(Curve c:green) plot(g,x,y,c,0xff168153,done);
        if(checked && !done) for(Curve c:gaps) {
            plot(g,x,y,c,0xffc74835,false);
            if(c.length()*scale()<9) plot(g,x,y,Curve.arc(c.at(.5),4/scale(),0,360),0xffc74835,false);
        }
        for(P p:sheet.datums()) mark(g,x,y,p,0xffbd852c);
        if(hint>=0) for(P p:sheet.hints().get(hint).points()) mark(g,x,y,p,0xffad683a);
        if(!done && Math.abs(getMouseX()-CX)<=HALF && Math.abs(getMouseY()-CY)<=HALF) {
            P hover=locate(mouse());
            if(tool==Tool.CIRCLE && !anchors.isEmpty()) {
                P center = anchors.get(0);
                plot(g,x,y,Curve.arc(center,center.dist(hover),0,360),0xff7898a4,false);
            }
            if(tool==Tool.FIXED) plot(g,x,y,Curve.arc(hover,radius,0,360),0x665c86a1,false);
            if(!anchors.isEmpty()) plot(g,x,y,Curve.line(anchors.get(0),hover),0xff7898a4,false);
            mark(g,x,y,hover,0xff32718a);
        }
    }
    private void drawControls(GuiGraphics g,int x,int y) {
        for(Tool entry:TOOLS) drawTool(g,x,y,entry);
        if(tool==Tool.FIXED || tool==Tool.ANGLE) drawValueControl(g,x,y);
        g.fill(x+LEFT,y+EDIT_TOP-3,x+LEFT+2*COLUMN_STEP+BUTTON_WIDTH,y+EDIT_TOP-2,0xffa89a80);
        drawAction(g,x,y,EDIT_TOP,0,tr("undo"),false);
        drawAction(g,x,y,EDIT_TOP,1,tr("redo"),false);
        drawAction(g,x,y,EDIT_TOP,2,tr("aux"),showAux);
        drawAction(g,x,y,REVIEW_TOP,0,tr("hint"),hint>=0);
        drawAction(g,x,y,REVIEW_TOP,1,tr("check"),checked);
        drawAction(g,x,y,REVIEW_TOP,2,remaining>=0?tr("cancel")+(remaining+19)/20:tr("auto")+sheet.seconds()+"s",remaining>=0);
        String note=done?tr("won"):!feedback.isEmpty()?feedback:checked && !gaps.isEmpty()?tr("gaps"):
            tool==Tool.CIRCLE && !anchors.isEmpty()?tr("rim"):
            hint>=0?Component.translatable(sheet.hints().get(hint).text()).getString():tr("controls");
        g.drawWordWrap(getFont(),Component.literal(note),x+LEFT,y+NOTE_TOP,2*COLUMN_STEP+BUTTON_WIDTH,0xff514332);
    }
    private void mark(GuiGraphics g,int x,int y,P p,int color) {
        int xx=x+px(p.x()),yy=y+py(p.y());
        g.renderOutline(xx-2,yy-2,5,5,color);
    }
}
