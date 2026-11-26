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
    private enum Tool { SEGMENT, LINE, RAY, CIRCLE, SPAN, ANGLE, BOX, POINT, DISTANCE, ERASE, REMOVE }
    private static final int CX=96,CY=99,HALF=82, LEFT=191, ROW=14;
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
        if(tool==Tool.CIRCLE) next.add(Curve.arc(p,radius,0,360));
        else if(tool==Tool.POINT) next.add(Curve.line(p,p));
        else if(anchors.isEmpty()) { anchors.add(p); return; }
        else {
            P a=anchors.get(0);
            if(p.dist(a)<EPS) return;
            switch(tool) {
                case SEGMENT,LINE,RAY -> next.add(ruled(a,p));
                case DISTANCE -> feedback=String.format(Locale.ROOT,"%.2f mm",p.dist(a));
                case SPAN -> { radius=p.dist(a); tool=Tool.CIRCLE; }
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
        adjust(scroll>0?1:-1); return true;
    }
    @Override public boolean onMousePressed(MouseButton button) {
        if(!isMouseOver()) return false;
        if(button==MouseButton.RIGHT) { anchors.clear(); feedback=""; return true; }
        if(button!=MouseButton.LEFT) return false;
        int x=(int)getMouseX(), y=(int)getMouseY();
        if(x>=LEFT+120 && x<LEFT+178 && y>=174 && y<186) { desk.closeDeskGame(); return true; }
        if(sheet==null || done || pending!=null) return true;
        if(Math.abs(x-CX)<=HALF && Math.abs(y-CY)<=HALF) { drawAt(mouse()); return true; }
        if(x<LEFT || x>=LEFT+180 || y<20) return true;
        int col=(x-LEFT)/60,row=(y-20)/ROW,index=row*3+col;
        if(index<Tool.values().length) { tool=Tool.values()[index]; anchors.clear(); feedback=""; return true; }
        if(row==4) { adjust(col==0?-1:col==2?1:0); return true; }
        if(row==5) {
            if(col==0 && !undo.isEmpty()) { redo.push(ink); update(undo.pop()); }
            if(col==1 && !redo.isEmpty()) { undo.push(ink); update(redo.pop()); }
            if(col==2) showAux=!showAux;
        }
        if(row==6) {
            if(col==0 && !sheet.hints().isEmpty()) { hint=(hint+1)%sheet.hints().size(); feedback=""; }
            if(col==1) { checked=true;send(Action.CHECK_DRAFT,""); }
            if(col==2) send(Action.TIMER_DRAFT,"");
        }
        return true;
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
    private void label(GuiGraphics g,int x,int y,int row,int col,String text,boolean selected) {
        int xx=x+LEFT+col*60, yy=y+20+row*ROW;
        g.fill(xx,yy,xx+58,yy+12,selected?0xff245966:0xffd7cbb5);
        g.drawCenteredString(getFont(),text,xx+29,yy+2,selected?0xffffffff:0xff393026);
    }
    @Override public void render(GuiGraphics g,int x,int y,int w,int h,RenderingHint rh) {
        g.fill(x,y,x+w,y+h,0xfff2e9d6);
        label(g,x,y,11,2,tr("back"),false);
        if(sheet==null) { g.drawString(getFont(),feedback,x+9,y+8,0xff453a31,false); return; }
        g.drawString(getFont(),Component.translatable(sheet.title()),x+9,y+4,0xff342d27,false);
        g.drawString(getFont(),sheet.dimensions(),x+LEFT,y+5,0xff493c30,false);
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
            if(tool==Tool.CIRCLE) plot(g,x,y,Curve.arc(hover,radius,0,360),0x665c86a1,false);
            if(!anchors.isEmpty()) plot(g,x,y,Curve.line(anchors.get(0),hover),0xff7898a4,false);
            mark(g,x,y,hover,0xff32718a);
        }
    }
    private void drawControls(GuiGraphics g,int x,int y) {
        for(Tool t:Tool.values()) label(g,x,y,t.ordinal()/3,t.ordinal()%3,tr(t.name().toLowerCase(Locale.ROOT)),t==tool);
        label(g,x,y,4,0,"−",false); label(g,x,y,4,1,String.format(Locale.ROOT,tool==Tool.ANGLE?"%.1f°":"R%.1f",tool==Tool.ANGLE?degrees:radius),false);
        label(g,x,y,4,2,"+",false); label(g,x,y,5,0,tr("undo"),false); label(g,x,y,5,1,tr("redo"),false);
        label(g,x,y,5,2,tr("aux"),showAux); label(g,x,y,6,0,tr("hint"),hint>=0);
        label(g,x,y,6,1,tr("check"),checked); label(g,x,y,6,2,remaining>=0?tr("cancel")+(remaining+19)/20:tr("auto")+sheet.seconds()+"s",remaining>=0);
        String note=done?tr("won"):!feedback.isEmpty()?feedback:checked && !gaps.isEmpty()?tr("gaps"):
            hint>=0?Component.translatable(sheet.hints().get(hint).text()).getString():tr("controls");
        g.drawWordWrap(getFont(),Component.literal(note),x+LEFT,y+123,178,0xff514332);
    }
    private void mark(GuiGraphics g,int x,int y,P p,int color) {
        int xx=x+px(p.x()),yy=y+py(p.y());
        g.renderOutline(xx-2,yy-2,5,5,color);
    }
}
