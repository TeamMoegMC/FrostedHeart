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

package com.teammoeg.chorda.client.cui.widgets;

import java.util.function.Consumer;

import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 带覆盖层图标的物品槽控件。在物品上方绘制一个额外的覆盖层图标，并支持自定义提示信息。
 * <p>
 * Item slot widget with overlay icon. Draws an additional overlay icon on top of the item,
 * and supports custom tooltip information.
 */
public class OverlayItemSlot extends ItemSlot {
	/** 覆盖层图标 / Overlay icon */
	@Getter
	@Setter
	protected CIcon overlay;
	/** 覆盖层宽度 / Overlay width */
	@Getter
	@Setter
	protected int overlayWidth;
	/** 覆盖层高度 / Overlay height */
	@Getter
	@Setter
	protected int overlayHeight;
	/** 自定义提示信息回调 / Custom tooltip callback */
	@Getter
	@Setter
	Consumer<TooltipBuilder> tooltips;

	/**
	 * 创建空的覆盖层物品槽。
	 * <p>
	 * Creates an empty overlay item slot.
	 *
	 * @param parent 父级UI元素 / Parent UI element
	 */
	public OverlayItemSlot(UIElement parent) {
		super(parent);
	}

	/**
	 * 创建包含单个物品的覆盖层物品槽。
	 * <p>
	 * Creates an overlay item slot with a single item stack.
	 *
	 * @param parent 父级UI元素 / Parent UI element
	 * @param item 物品堆 / Item stack
	 */
	public OverlayItemSlot(UIElement parent, ItemStack item) {
		super(parent, item);
	}

	/**
	 * 创建包含配方原料的覆盖层物品槽。
	 * <p>
	 * Creates an overlay item slot with an ingredient.
	 *
	 * @param parent 父级UI元素 / Parent UI element
	 * @param item 配方原料 / Recipe ingredient
	 */
	public OverlayItemSlot(UIElement parent, Ingredient item) {
		super(parent, item);
	}

	/**
	 * 创建包含物品堆数组的覆盖层物品槽。
	 * <p>
	 * Creates an overlay item slot with an array of item stacks.
	 *
	 * @param parent 父级UI元素 / Parent UI element
	 * @param item 物品堆数组 / Array of item stacks
	 */
	public OverlayItemSlot(UIElement parent, ItemStack[] item) {
		super(parent, item);
	}

	/** {@inheritDoc} */
	@Override
	public void render(GuiGraphics graphics, int x, int y, int w, int h) {

		super.render(graphics, x, y, w, h);

        if (overlay != null) {
        	graphics.pose().pushPose();
        	graphics.pose().translate(0, 0, 200);
            overlay.draw(graphics, x, y, overlayWidth, overlayHeight);
            graphics.pose().popPose();
        }

	}

    /**
     * 设置覆盖层图标及其尺寸。
     * <p>
     * Sets the overlay icon and its dimensions.
     *
     * @param overlay 覆盖层图标 / Overlay icon
     * @param height 覆盖层高度 / Overlay height
     * @param width 覆盖层宽度 / Overlay width
     */
    public void setOverlay(CIcon overlay, int height, int width) {
        this.overlay = overlay;
        this.overlayHeight = height;
        this.overlayWidth = width;
    }

    /**
     * 重置（移除）覆盖层图标。
     * <p>
     * Resets (removes) the overlay icon.
     */
    public void resetOverlay() {
        this.overlay = null;
    }

	/** {@inheritDoc} */
	@Override
	public void getTooltip(TooltipBuilder tooltip) {
		super.getTooltip(tooltip);
		if(tooltips!=null)
			tooltips.accept(tooltip);
	}

	/** {@inheritDoc} */
	@Override
	public void clear() {
		resetOverlay();
		super.clear();
	}
    
}
