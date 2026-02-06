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

package com.teammoeg.frostedheart.content.robotics.logistics.gui;

import com.teammoeg.chorda.client.ScrollTracker;
import com.teammoeg.chorda.client.cui.CheckBox;
import com.teammoeg.chorda.client.cui.ImageButton;
import com.teammoeg.chorda.client.cui.MouseButton;
import com.teammoeg.chorda.client.cui.TextBoxNoBackground;
import com.teammoeg.chorda.client.cui.UIElement;
import com.teammoeg.chorda.client.cui.UILayer;
import com.teammoeg.chorda.client.cui.editor.Verifier;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.text.Components;
import com.teammoeg.frostedheart.content.robotics.logistics.Filter;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public class DockFilterDialog extends UILayer {
	CheckBox check;
	ImageButton back;
	RequesterChestMenu menu;
	TextBoxNoBackground numberBox;
	final int index;
	ScrollTracker tracker=new ScrollTracker();
	ItemStack displayStack;
	public DockFilterDialog(RequesterChestScreen panel,RequesterChestMenu menu, int index) {
		super(panel);
		this.index=index;
		this.menu=menu;
		displayStack=getFilter().createDisplayStack();
		this.check=new CheckBox(this,CIcons.nop(),LogisticIcons.BUTTON_CHECK,getFilter().isIgnoreNbt());
		check.setPosAndSize(72,17,8, 8);
		
		this.back=new ImageButton(this,CIcons.nop(),LogisticIcons.BUTTON_BACK_ON) {

			@Override
			public void onClicked(MouseButton button) {
				panel.closeFilterLayer();
			}
			
		};
		back.setPosAndSize(92,3, 12, 12);
		numberBox=new TextBoxNoBackground(this);
		numberBox.setFilter(b->{
			try {
				int val=Integer.parseInt(b);
				if(val>1728||val<1)
					return Verifier.error(Components.literal("1~1728"));
			}catch(Exception ex) {
				return Verifier.error(Components.literal("Number only"));
			}
			return Verifier.SUCCESS;
			
		});
		numberBox.setMaxLength(4);
		numberBox.setPosAndSize(35, 18, 24, 7);
		numberBox.setText(String.valueOf(getFilter().getSize()));
		numberBox.setRightAlign();
		setSize(108,50);
		
	}
	public void updateFilterSetting() {
		if(numberBox.isTextValid())
			menu.setFilterSize(index, Integer.valueOf(numberBox.getText()));
		menu.setFilterIgnoreNbt(index, check.isChecked());
	};
	@Override
	public void render(GuiGraphics graphics, int x, int y, int w, int h) {
		LogisticIcons.FILTER_BACK.draw(graphics, x, y, w, h);
		super.render(graphics, x, y, w, h);
		CGuiHelper.drawItem(graphics,displayStack, x+9, y+9, 0, true, "");
	}
	@Override
	public void addUIElements() {
		add(check);
		add(back);
		add(numberBox);
		setSize(108,50);
		
	}
	public Filter getFilter() {
		return menu.list.get(index).getValue();
	}
	@Override
	public void alignWidgets() {

	}

}
