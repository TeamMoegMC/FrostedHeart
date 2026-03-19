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

package com.teammoeg.frostedheart.content.archive;

import com.teammoeg.chorda.client.AnimationUtil;
import com.teammoeg.chorda.client.CSSStylingUtil;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.MouseHelper;
import com.teammoeg.chorda.client.cui.base.PrimaryLayer;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.contentpanel.ArchiveTheme;
import com.teammoeg.chorda.client.cui.contentpanel.ContentPanel;
import com.teammoeg.chorda.client.cui.screenadapter.CUIScreenWrapper;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.archive.ArchiveCategory.ArchiveEntry;
import com.teammoeg.frostedheart.content.tips.ClickActions;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class ArchiveScreen extends PrimaryLayer {
    public static String path;
    public final ContentPanel contentPanelOut;
    public final ContentPanel contentPanel;
    public final ArchiveCategory category;
    private Function<UIElement,List<? extends UIElement>> lastEntry;
    private Function<UIElement,List<? extends UIElement>> currentEntry;
    private Function<UIElement,List<? extends UIElement>> buffedEntry;
    static {
        ClickActions.register(FHMain.rl("view_in_archive"), "tips.frostedheart.click_action.open_archive", ArchiveScreen::open);
    }

    public boolean flipAnimationEnabled = false;

    public ArchiveScreen() {
        setTheme(ArchiveTheme.INSTANCE);
        this.contentPanel = new ContentPanel(this) {
            @Override
            public void resize() {
                int h = (int)(ClientUtils.screenHeight() * 0.8F);
                int w = (int)(h * 1.3333F); // 4:3
                setPosAndSize(120, 0, w, h);
                super.resize();
            }
        };
        this.contentPanelOut = new ContentPanel(this) {
            @Override
            public void resize() {
                int h = (int)(ClientUtils.screenHeight() * 0.8F);
                int w = (int)(h * 1.3333F); // 4:3
                setPosAndSize(90, -25, w, h);
                super.resize();
            }
        	public boolean isEnabled() {
        		return false;
        	}

        };
        contentPanelOut.setVisible(false);
        this.category = new ArchiveCategory(this);
    }
    public void swapPanels() {
    	contentPanel.fillContent(elements);
//    	contentPanel.setPos(120, 0);
    }
    public ArchiveScreen(String path) {
        this();
        if (path != null) {
            ArchiveCategory.currentPath = path;
        }
    }
    
    @Override
	public void updateRenderInfo(double mx, double my, float pt) {
    	if(transformation!=null) {
    		double scx=MouseHelper.getScaledX();
    		double scy=MouseHelper.getScaledY();
    		Vector3f v2f=unprojectScreenToPlane(scx,scy,transformation);
    		if(v2f!=null) {
    			double nmx=v2f.x-scx+mx;
    			double nmy=v2f.y-scy+my;
	    		super.updateRenderInfo(nmx, nmy, pt);
	    		return;
    		}
    	}
		super.updateRenderInfo(mx, my, pt);
	}
    private static Matrix4f skewm2=CSSStylingUtil.skewY(-.5f);
    private Matrix4f transformation;
	public Vector3f unprojectScreenToPlane(double screenX, double screenY,
            Matrix4f mvp) {
		Vector3f start=new Vector3f((float)screenX,(float)screenY,1f);
		mvp.transformPosition(start);
		return start;
	}
    @Override
	public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h) {
        if (!flipAnimationEnabled) {
            if (buffedEntry != null) {
                currentEntry=buffedEntry;
                contentPanel.fillContent(buffedEntry.apply(contentPanel));
                buffedEntry=null;
            }
            super.drawBackground(graphics, x, y, w, h);
            return;
        }

        if(currentEntry!=null) {
            float value=AnimationUtil.fadeIn(750, "archive", false);
            if(lastEntry==null)value=1;
            int maxh=ClientUtils.screenHeight();
            if(value<.66666f) {
                float ratio=value*3/2;
                contentPanelOut.setPos((int) (ratio*30+120),(int) (ratio*25));
                contentPanel.setPos((int) (ratio*30+90),(int) (ratio*25-25));
            }else {
                float ratio=value*3-2;
                contentPanelOut.setPos(150, (int) (25+ratio*maxh));
            }
            if(value>=1) {
                AnimationUtil.remove("archive");
                contentPanelOut.setVisible(false);
                contentPanelOut.fillContent(currentEntry.apply(contentPanelOut));
                lastEntry=currentEntry;
                currentEntry=null;
            }
        }else if(buffedEntry!=null) {
            AnimationUtil.remove("archive");
            currentEntry=buffedEntry;
            buffedEntry=null;
            contentPanelOut.setVisible(true);
            contentPanel.setPos(90, -25);
            contentPanelOut.setPos(120,0);
            contentPanel.fillContent(currentEntry.apply(contentPanel));

        }

        super.drawBackground(graphics, x, y, w, h);
    }


	@Override
	public void beforeDrawElements(GuiGraphics graphics, int parX, int parY, int x, int y, int w, int h) {
		Matrix4f before=new Matrix4f(graphics.pose().last().pose());
		graphics.pose().translate(-x-w/2, -y-h/2, 50);
		graphics.pose().mulPoseMatrix(skewm2);
		graphics.pose().translate(x+w/2, y+h/2, 0);
		graphics.fill(x+Mth.floor(getMouseX())-2,y+Mth.floor(getMouseY())-2, x+Mth.floor(getMouseX())+2, y+Mth.floor(getMouseY())+2, 0xffffffff);
		graphics.pose().translate(0,0,-50);
		Matrix4f after=new Matrix4f(graphics.pose().last().pose());
		after.mul(before.invert());
		after.invert();
		transformation=after;
		super.beforeDrawElements(graphics, parX, parY, x, y, w, h);
	}
	@Override
    public void addUIElements() {
        add(contentPanel);
        if (flipAnimationEnabled) {
            add(contentPanelOut);
        }
        add(category);
        add(contentPanel.scrollBar);
        add(category.scrollBar);
    }

    @Override
    public void getTooltip(TooltipBuilder list) {
        super.getTooltip(list);
        list.translateZ(300);
    }

    public static void open(@Nullable String path) {
        var layer = new ArchiveScreen(path);
        ClientUtils.getMc().setScreen(new CUIScreenWrapper(layer));
    }
	public void select(ArchiveEntry ae) {
		buffedEntry=panel->{
			List<UIElement> contents = new ArrayList<>(ae.getContents(panel));
	        contents.addAll(ae.getExtraElements(panel));
	        return contents;
		};
		
	}

	public void setContent(Function<UIElement,List<? extends UIElement>> contents) {
		buffedEntry=contents;
	}
}
