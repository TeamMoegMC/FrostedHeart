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

package com.teammoeg.frostedheart.content.wheelmenu;

import java.util.function.Predicate;

import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.client.icon.FlatIcon;
import com.teammoeg.chorda.client.ui.Colors;

import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
@OnlyIn(Dist.CLIENT)
public class Selection {

	public static final Predicate<Selection> ALWAYS_VISIBLE = s -> true;
	public static final Action NO_ACTION = Action.NoAction.INSTANCE;

	protected final Predicate<Selection> visibility;
	protected final Action selectAction;
	protected final Action hoverAction;
	@Getter
	public final CIcon icon;
	@Getter
	protected Component message;
	@Getter
	protected boolean visible;
	@Getter
	protected boolean hovered;
	protected boolean autoAdded=true;
	public int color;
	public Selection(UserSelection sel) {
		this(sel.getParsedMessage(),sel.icon(),sel.selectAction()==null?Selection.NO_ACTION:sel.selectAction());
	}
	/**
	 * @param icon        {@link ItemStack}, {@link FlatIcon},
	 *                    {@link Component}, {@code null}
	 * @param selectAction 选择后的行动 (选中 -> 松开Tab)
	 */
	Selection(Component message, CIcon icon, Action selectAction) {
		this(ALWAYS_VISIBLE, selectAction, NO_ACTION,icon,message, true, Colors.CYAN);
	}

	
	Selection(Predicate<Selection> visibility, Action selectAction, Action hoverAction, CIcon icon, Component message, boolean autoAdded, int color) {
		super();
		this.visibility = visibility;
		this.selectAction = selectAction;
		this.hoverAction = hoverAction;
		this.icon = icon;
		this.message = message;
		this.autoAdded = autoAdded;
		this.color = color;
	}
	protected void render(ForgeGui gui, GuiGraphics graphics, float partialTick,int x,int y, int width, int height) {
		if (!visible)
			return;
		renderSelection(gui, graphics, partialTick,x,y, width, height);
		if (hovered) {
			renderWhenHovered(gui, graphics, partialTick,x,y, width, height);
		}
	}

	@SuppressWarnings("unused")
	protected void renderSelection(ForgeGui gui, GuiGraphics graphics, float partialTick,int x,int y,  int width, int height) {
		icon.draw(graphics, x-(width/2), y-(height/2), width, height);
	}

	/**
	 * 选中选项时渲染
	 */
	@SuppressWarnings("unused")
	protected void renderWhenHovered(ForgeGui gui, GuiGraphics graphics, float partialTick,int x,int y,  int width, int height) {
	}

	protected void tick() {
		hovered = WheelMenuRenderer.hoveredSelection == this;
		validateVisibility();
	}
	public void validateVisibility() {
		visible = visibility.test(this);
	}

	/**
	 * Return true if this option should be added to selection list when became visible
	 * Disable this makes this selection not added to selection list by default
	 * */
	public boolean isAutoAddable() {
		return autoAdded;
	}

}