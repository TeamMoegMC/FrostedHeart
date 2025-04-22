package com.teammoeg.frostedresearch.gui;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.PartialTickTracker;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.client.ui.ColorHelper;
import com.teammoeg.chorda.client.ui.Point;
import com.teammoeg.frostedheart.util.client.FGuis;
import com.teammoeg.frostedresearch.FRMain;
import com.teammoeg.frostedresearch.api.ClientResearchDataAPI;
import com.teammoeg.frostedresearch.data.TeamResearchData;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class InsightOverlay implements IGuiOverlay{
	public static InsightOverlay INSTANCE;
	private static CIcon[] INSIGHT_ICONS=new CIcon[6];
	static {
		for(int i=0;i<6;i++) {
			INSIGHT_ICONS[i]=CIcons.getIcon(FRMain.rl("textures/gui/insight/insight_"+i+".png"));
		}
	}
	private static final int LEVEL_SCROLL_TICKS=10;//ticks for animation when level changes
	// at least enough for viewing after exiting quest book
	private static final int LINGERING_TICKS=200;//ticks the overlay stay on screen after all animation ends
	private static final int COLOR=0xff8cffd6;//color style 
	private static final float MAX_PROGRESS_PRE_TICK=0.05f;//max progress added per tick
	private static final float MIN_PROGRESS_PRE_TICK=0.01f;//min progress added per tick
	private static final int CENTER_Y=35;//Y-POS for the whole ui
	int renderingInsightLevel;
	int renderingNextInsightLevel;
	float renderingInsightProgress;
	float renderingInsightNextProgress;
	int textScrollTicks;
	boolean isShown;
	int hiddenTicks;
	@Override
	public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
		if(!isShown)return;
		if(ClientUtils.mc().isPaused())return;
		//partialTick=PartialTickTracker.getTickAlignedPartialTicks();
		//FGuis.drawRing(guiGraphics, screenWidth/2, 25, 11, 14, -60,30, 0xff8cffd6);
		float angleProgress=0;
		//render animated progress bar if needed
		if(renderingInsightNextProgress!=renderingInsightProgress)
			angleProgress=Mth.clamp((renderingInsightNextProgress-renderingInsightProgress)*partialTick+renderingInsightProgress, 0, 1);
		else
			angleProgress=renderingInsightProgress;
		INSIGHT_ICONS[Mth.clamp((int)(angleProgress*6), 0, 5)].draw(guiGraphics, screenWidth/2-12, CENTER_Y-14, 24, 24);
		angleProgress=angleProgress*280-140;
		
		FGuis.drawRing(guiGraphics, screenWidth/2, CENTER_Y, 14, 19, -140,angleProgress, COLOR);//exp bar
		//FGuis.drawRing(guiGraphics, screenWidth/2, 51, 2, 6, 0,360, 0xff8cffd6);//num bar
		
		float direction=Math.signum(renderingInsightLevel-renderingNextInsightLevel);
		
		int textHeight=ClientUtils.font().lineHeight;
		if(direction!=0) {
			guiGraphics.enableScissor(0, CENTER_Y+12, screenWidth, CENTER_Y+12+textHeight);
			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(0, direction*((LEVEL_SCROLL_TICKS-textScrollTicks)*1f/LEVEL_SCROLL_TICKS)*textHeight, 0);
			guiGraphics.drawCenteredString(ClientUtils.font(), ""+renderingInsightLevel, screenWidth/2, CENTER_Y+12, COLOR);
			guiGraphics.drawCenteredString(ClientUtils.font(), ""+renderingNextInsightLevel, screenWidth/2, (int) (CENTER_Y+12-(direction*textHeight)), COLOR);
			guiGraphics.pose().popPose();
			guiGraphics.disableScissor();
		}else {
			guiGraphics.drawCenteredString(ClientUtils.font(), ""+renderingInsightLevel, screenWidth/2, CENTER_Y+12, COLOR);
		}
	}
	public void init() {
		TeamResearchData rd=ClientResearchDataAPI.getData().get();
		renderingNextInsightLevel=renderingInsightLevel=rd.getInsightLevel();
		renderingInsightNextProgress=renderingInsightProgress=rd.getInsightProgress();
		textScrollTicks=0;
		isShown=false;
		hiddenTicks=0;
	}
	public void tick() {
		if(ClientUtils.mc().isPaused())return;
		if(textScrollTicks>0) {//text is scrolling, wait until text scroll finished
			textScrollTicks--;
			renderingInsightProgress=renderingInsightNextProgress;
			if(textScrollTicks<=0) {//text scroll finish, maintain progress bar to next/prev level
				textScrollTicks=0;
				
				if(renderingInsightLevel>renderingNextInsightLevel) {
					renderingInsightNextProgress=1;
				}else if(renderingInsightLevel<renderingNextInsightLevel) {
					renderingInsightNextProgress=0;
				}
				renderingInsightLevel=renderingNextInsightLevel;
			}else
				return;
		}
		TeamResearchData rd=ClientResearchDataAPI.getData().get();
		int curInsightLevel=rd.getInsightLevel();
		float curInsightProgress=rd.getInsightProgress();
		renderingInsightProgress=renderingInsightNextProgress;
		float delta=(curInsightLevel-renderingInsightLevel)+curInsightProgress-renderingInsightProgress;
		if(delta!=0) {
			//display overlay when animation needed
			isShown=true;
			hiddenTicks=LINGERING_TICKS;
			float direction=Math.signum(delta);
			float value=Math.abs(delta);
			//calculate smoothed value
			float smoothedvalue=Mth.clamp(value/5F, MIN_PROGRESS_PRE_TICK, MAX_PROGRESS_PRE_TICK);
			
			//do not obey min-progress if smoothed value is larger than value itself
			value=Math.min(value, smoothedvalue);
			//calculate animated progress for next tick, and smooth the animation with partialticks
			renderingInsightNextProgress+=value*direction;
			//progress over 1 or lower than 0, the level should be modified
			if(renderingInsightNextProgress<0) {
				renderingInsightNextProgress=0;
				renderingNextInsightLevel=renderingInsightLevel-1;
				textScrollTicks=LEVEL_SCROLL_TICKS;
			}else if(renderingInsightNextProgress>=1) {
				renderingInsightNextProgress=1;
				renderingNextInsightLevel=renderingInsightLevel+1;
				textScrollTicks=LEVEL_SCROLL_TICKS;
			}
		}else if(isShown) {//all animation has done, wait for several ticks before hidding this overlay
			hiddenTicks--;
			if(hiddenTicks<=0) {
				isShown=false;
			}
		}
	}
	public InsightOverlay() {
		super();
		INSTANCE=this;
		//init();
	}
	public static void initOverlay() {
		if(INSTANCE!=null)
			INSTANCE.init();
	}
}
