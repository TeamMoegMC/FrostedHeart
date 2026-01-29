package com.teammoeg.frostedheart.content.robotics.logistics.gui;

import com.teammoeg.chorda.client.ScrollTracker;
import com.teammoeg.chorda.client.cui.MouseButton;
import com.teammoeg.chorda.client.cui.UIElement;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.client.ui.Colors;
import com.teammoeg.chorda.menu.CCustomMenuSlot.CDataSlot;
import com.teammoeg.frostedheart.content.robotics.logistics.Filter;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public class UIFilterSlot extends UIElement {
	RequesterChestMenu menu;
	final int index;
	ScrollTracker tracker=new ScrollTracker();
	ItemStack displayStack=ItemStack.EMPTY;
	public UIFilterSlot(UIElement parent, RequesterChestMenu menu, int index) {
		super(parent);
		this.menu = menu;
		this.index = index;
	}
	@Override
	public void refresh() {
		CDataSlot<Filter> slot=menu.list.get(index);
		slot.bind(c->{
			if(c==null)
				displayStack=ItemStack.EMPTY;
			else
				displayStack=c.createDisplayStack();
		});
	}
	@Override
	public void render(GuiGraphics graphics, int x, int y, int w, int h) {
		if(!displayStack.isEmpty())
			CGuiHelper.drawItem(graphics,displayStack, x, y, 0, true , null);
		super.render(graphics, x, y, w, h);
		if (isMouseOver()) {
			graphics.fill(x, y, x + w, y + h, 300, Colors.setAlpha(Colors.WHITE, 0.25F));
		}
	}
	@Override
	public boolean onMousePressed(MouseButton button) {
		if(!isMouseOver())return super.onMousePressed(button);
		if(button==MouseButton.RIGHT) {
			menu.unsetFilterItem(index);
			return true;
		}else if(button==MouseButton.LEFT) {
			if(!menu.getCarried().isEmpty()) {
				menu.setFilterItem(index);
				
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
			int oldsize=filter.getSize();
			int newsize=(oldsize+val-1)%1728+1;
			if(newsize<=0)
				newsize+=1728;
			menu.setFilterSize(index, newsize);
		}
		return true;
	}


}
