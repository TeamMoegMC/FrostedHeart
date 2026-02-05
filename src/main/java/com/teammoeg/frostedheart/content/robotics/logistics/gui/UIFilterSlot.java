/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.robotics.logistics.gui;

import com.teammoeg.chorda.client.CInputHelper;
import com.teammoeg.chorda.client.ScrollTracker;
import com.teammoeg.chorda.client.cui.MouseButton;
import com.teammoeg.chorda.client.cui.TooltipBuilder;
import com.teammoeg.chorda.client.cui.UIElement;
import com.teammoeg.chorda.client.icon.FlatIcon;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.client.ui.Colors;
import com.teammoeg.chorda.menu.CCustomMenuSlot.CDataSlot;
import com.teammoeg.chorda.text.CFormatHelper;
import com.teammoeg.frostedheart.content.robotics.logistics.Filter;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class UIFilterSlot extends UIElement {
	RequesterChestMenu menu;
	final int index;
	ScrollTracker tracker=new ScrollTracker();
	ItemStack displayStack=ItemStack.EMPTY;
	FilterLayer layer;
	public UIFilterSlot(FilterLayer parent, RequesterChestMenu menu, int index) {
		super(parent);
		this.menu = menu;
		this.index = index;
		this.layer=parent;
		CDataSlot<Filter> slot=menu.list.get(index);
		slot.bind(c->{
			if(c==null)
				displayStack=ItemStack.EMPTY;
			else
				displayStack=c.getDisplayItem();
		});
		if(slot.getValue()!=null)
			displayStack=slot.getValue().getDisplayItem();
		else
			displayStack=ItemStack.EMPTY;
	}
	@Override
	public void refresh() {

	}
	@Override
	public void render(GuiGraphics graphics, int x, int y, int w, int h) {
		if(!displayStack.isEmpty()&&getFilter()!=null) {
			CGuiHelper.drawItem(graphics,displayStack, x, y, 0, true , null);
			
			int size=getFilter().getSize();
	         if (size != 1) {
	        	 
	             String s = CFormatHelper.toReadableItemStackUnit(size);
	             Font pFont=getFont();
	             int width=pFont.width(s);
	             graphics.pose().pushPose();
	             if(size>99) {
		             width=(int) (width*0.8);
		             
		             graphics.pose().translate(x+18-width, y+11, 350.0F);
		             graphics.pose().scale(0.8f, 0.8f,1f);
		             graphics.drawString(pFont, s, 0, 0, 16777215, true);
		             
	             }else {
	            	 graphics.pose().translate(0, 0, 350.0F);
		             graphics.drawString(pFont, s, x+18-width, y+9, 16777215, true);
	             }
	             graphics.pose().popPose();
	             
	          }
			
		}
		super.render(graphics, x, y, w, h);
		if (isMouseOver()) {
			graphics.fill(x, y, x + w, y + h, 300, Colors.setAlpha(Colors.WHITE, 0.25F));
		}
	}

	@Override
	public void getTooltip(TooltipBuilder tooltip) {
		if (!displayStack.isEmpty()) {
			tooltip.add(Component.empty().append(displayStack.getHoverName()).append(" x" + getFilter().getSize()));
			tooltip.add(Component.empty()
					.append(FlatIcon.INFO.toCTextIcon())
					.append(" ")
					.append(Component.translatable("gui.frostedheart.scroll_to_adjust"))
					.append(Component.literal("±" + getAdjustIncrement()))
					.withStyle(ChatFormatting.GRAY));
			tooltip.add(Component.translatable("gui.frostedheart.adjust_increment").withStyle(ChatFormatting.DARK_GRAY));
		}
		super.getTooltip(tooltip);
	}

	private int getAdjustIncrement() {
		boolean shift = CInputHelper.isShiftKeyDown();
		boolean ctrl = CInputHelper.isCtrlKeyDown();
		return shift&&ctrl?576:(shift?16:(ctrl?64:1));
	}

	@Override
	public boolean onMousePressed(MouseButton button) {
		if(!isMouseOver())return super.onMousePressed(button);
		if (button==MouseButton.RIGHT && getFilter() == null) {
			menu.setFilterItem(index);
			menu.setFilterSize(index, 1);
			return true;
		} else if(button==MouseButton.RIGHT) {
			menu.unsetFilterItem(index);
			return true;
		}else if(button==MouseButton.LEFT) {
			if(!menu.getCarried().isEmpty()) {
				menu.setFilterItem(index);
				
			}else if(getFilter() != null){
				layer.layer.openFilterLayer(index);
			}
			return true;
		}
		return super.onMousePressed(button);
	}
	public Filter getFilter() {
		return menu.list.get(index).getValue();
	}
	@Override
	public boolean onMouseScrolled(double scroll) {
		if(!isMouseOver())return super.onMouseScrolled(scroll);
		Filter filter=getFilter();
		if(filter==null)
			return super.onMouseScrolled(scroll);
		tracker.addScroll(scroll);
		int val=tracker.getScroll();
		if(val!=0) {
			int oldsize = (filter.getSize() == 1 && (CInputHelper.isShiftKeyDown() || CInputHelper.isCtrlKeyDown())) ? 0 : filter.getSize();
			int newsize = Mth.clamp(oldsize + (val*getAdjustIncrement()), 1, 1728);
			menu.setFilterSize(index, newsize);
		}
		return true;
	}


}
