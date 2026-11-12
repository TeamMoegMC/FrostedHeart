package com.teammoeg.frostedresearch.knowledge.client.experiment;
import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MenuPrimaryLayer;
import com.teammoeg.frostedresearch.knowledge.client.ui.*;
import com.teammoeg.frostedresearch.knowledge.furnace.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import java.util.*;

/** Current temperature is latched; adjustment applies to the next batch. */
public final class ControlledFurnaceScreen extends MenuPrimaryLayer<ControlledFurnaceMenu>{
    private final JournalText text=new JournalText(this);private int signature;
    public ControlledFurnaceScreen(ControlledFurnaceMenu menu){super(menu);setTheme(KnowledgeUiTheme.INSTANCE);}
    static Component t(String k,Object... a){return Component.translatable("gui.frostedresearch.furnace."+k,a);}
    @Override public boolean onInit(){setSize(450,220);return super.onInit();}
    private void button(int x,int y,int width,String key,int action){
        JournalButton b=new JournalButton(this,t(key),false,()->Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId,action));
        b.setPosAndSize(x,y,width,20);add(b);
    }
    @Override public void addChildUIElements(){
        var f=menu.getBlock();button(150,30,100,f.trial()?"trial":"production",2);
        button(260,30,40,"minus",0);button(304,30,40,"plus",1);
        boolean displayedTrial=f.trial();var displayedTarget=f.selectedTarget();
        JournalButton start=new JournalButton(this,t("start"),false,()->com.teammoeg.frostedresearch.FRNetwork.INSTANCE.sendToServer(
            new ControlledFurnaceStartPacket(menu.containerId,f.getBlockPos(),displayedTrial,displayedTarget)));
        start.setPosAndSize(150,183,100,20);add(start);button(260,183,100,"cancel",4);
        List<Component> lines=new ArrayList<>(List.of(t("next",f.temperature()),t("process",f.lockedTemperature()),
            t("progress",Math.max(0,f.duration()-f.remaining()),f.duration()),t(f.message())));
        for(var recipe:f.recipes())lines.add(t("batch",recipe.input().getCount(),recipe.input().getHoverName(),recipe.fuel().getCount(),recipe.fuel().getHoverName(),recipe.duration()));
        int textTop=60;
        if(f.trial()) {
            if(f.targetCount()>1 && !f.displayedBusy()) {
                JournalButton target=new JournalButton(this,f.targetTitle().getString().isBlank()?t("choose_target"):f.targetTitle(),true,
                    ()->Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId,5));
                target.setPosAndSize(150,60,285,19);target.tooltip(f.targetTitle().getString().isBlank()?t("choose_target"):f.targetTitle());add(target);textTop=83;
            }else if(!f.targetTitle().getString().isBlank())lines.add(0,f.targetTitle());
        }
        lines.add(t("rules"));text.text(lines);
        text.setPosAndSize(150,textTop,285,175-textTop);add(text);
    }
    @Override public void alignWidgets(){}
    @Override public void tick(){var f=menu.getBlock();int now=Objects.hash(f.trial(),f.temperature(),f.lockedTemperature(),f.remaining()/20,f.message(),f.targetTitle(),f.targetCount(),f.displayedBusy());if(now!=signature){signature=now;refresh();}super.tick();}
    private static void drawSlot(GuiGraphics g,int x,int y){
        g.fill(x-1,y-1,x+17,y+17,KnowledgeUiTheme.MUTED);
        g.fill(x,y,x+16,y+16,KnowledgeUiTheme.PAPER_DARK);
    }
    @Override public void drawBackground(GuiGraphics g,int x,int y,int w,int h,RenderingHint hint){
        KnowledgeUiTheme.drawPaper(g,x,y,w,h);
        g.drawString(getFont(),Component.translatable("block.frostedresearch.controlled_furnace"),x+150,y+12,KnowledgeUiTheme.INK,false);
        String[] labels={"input","fuel","output"};for(int i=0;i<3;i++){int[] p=ControlledFurnaceMenu.POSITIONS[i];g.drawString(getFont(),t(labels[i]),x+p[0]-5,y+18,KnowledgeUiTheme.INK,false);drawSlot(g,x+p[0],y+p[1]);}
        for(int i=0;i<36;i++){int p=i<9?i+27:i-9;int sx=x+10+(p%6)*17,sy=y+93+(p/6)*17;drawSlot(g,sx,sy);}
    }
}
