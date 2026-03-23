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

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.math.Colors;
import com.teammoeg.frostedheart.compat.jei.JEICompat;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;

import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/**
 * 物品槽控件。用于显示物品堆的UI元素，支持缩放、悬停高亮和JEI集成。
 * 当存在多个物品时，会按秒轮播展示。
 * <p>
 * Item slot widget. A UI element for displaying item stacks, supporting scaling,
 * hover overlay, and JEI integration. When multiple items are present, they cycle
 * through at one-second intervals.
 */
@Setter
@Getter
public class ItemSlot extends UIElement {
    /** 物品默认宽度（像素） / Default item width in pixels */
    public static final int ITEM_WIDTH = 16;
    /** 物品默认高度（像素） / Default item height in pixels */
    public static final int ITEM_HEIGHT = 16;
    /** 空物品数组常量 / Empty item array constant */
    private static final ItemStack[] EMPTY=new ItemStack[0];
    /** 当前显示的物品数组 / Currently displayed item array */
    protected ItemStack[] item;
    /** 物品渲染缩放比例 / Item rendering scale */
    protected float scale;
    /** 是否启用悬停高亮覆盖层 / Whether hover overlay is enabled */
    protected boolean hoverOverlayEnabled = true;
    /** 当前轮播序号 / Current cycling index */
    private int order=0;
    /** 覆盖显示的物品数量，0表示使用实际数量 / Override item count display, 0 means use actual count */
    private int countOverride=0;
    /**
     * 创建空物品槽。
     * <p>
     * Creates an empty item slot.
     *
     * @param parent 父级UI元素 / Parent UI element
     */
    public ItemSlot(UIElement parent) {
        this(parent,EMPTY);
        setScale(1);
    }

    /**
     * 创建包含单个物品堆的物品槽。
     * <p>
     * Creates an item slot with a single item stack.
     *
     * @param parent 父级UI元素 / Parent UI element
     * @param item 要显示的物品堆 / Item stack to display
     */
    public ItemSlot(UIElement parent, ItemStack item) {
        this(parent);
        this.setItem(item);

    }

    /**
     * 创建包含配方原料的物品槽，会轮播显示所有匹配物品。
     * <p>
     * Creates an item slot with an ingredient, cycling through all matching items.
     *
     * @param parent 父级UI元素 / Parent UI element
     * @param item 配方原料 / Recipe ingredient
     */
    public ItemSlot(UIElement parent, Ingredient item) {
        this(parent);
        this.setItem(item);
    }

    /**
     * 创建包含物品堆数组的物品槽。
     * <p>
     * Creates an item slot with an array of item stacks.
     *
     * @param parent 父级UI元素 / Parent UI element
     * @param item 物品堆数组 / Array of item stacks
     */
    public ItemSlot(UIElement parent, ItemStack[] item) {
        super(parent);
        this.setItem(item);

    }

    /**
     * 设置显示的配方原料。
     * <p>
     * Sets the ingredient to display.
     *
     * @param item 配方原料 / Recipe ingredient
     */
    public void setItem(Ingredient item) {
    	this.setItem(item.getItems());
    }

    /**
     * 设置显示的单个物品堆。
     * <p>
     * Sets a single item stack to display.
     *
     * @param item 物品堆 / Item stack
     */
    public void setItem(ItemStack item) {
    	this.setItem(new ItemStack[] {item});
    }

    /**
     * 设置显示的物品堆数组。
     * <p>
     * Sets the array of item stacks to display.
     *
     * @param item 物品堆数组 / Array of item stacks
     */
    public void setItem(ItemStack[] item) {
    	this.item=item;
    }
    /** {@inheritDoc} */
    @Override
    public void render(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
    	renderBackground(graphics,x,y,w,h,hint);
    	if(item.length>1)
    		order=(int) ((System.currentTimeMillis() / 1000) % item.length);
    	else
    		order=0;
    	if(item.length>0) {
	        if (scale != 1) {
	            CGuiHelper.drawItem(graphics, item[order], x, y, 100, scale, scale, true, countOverride == 0 ? null : String.valueOf(countOverride));
	        } else {
	        	CGuiHelper.drawItem(graphics, item[order], x, y, 100, true, countOverride == 0 ? null : String.valueOf(countOverride));
		    }
    	}
        if (isHoverOverlayEnabled() && isMouseOver()) {
            graphics.fill(x, y, x+w, y+h, 151, Colors.setAlpha(Colors.WHITE, 0.25F));
        }
    }
    /**
     * 绘制物品槽背景。子类可重写以自定义背景绘制。
     * <p>
     * Draws the item slot background. Subclasses can override for custom background rendering.
     *
     * @param graphics 图形上下文 / Graphics context
     * @param x X坐标 / X coordinate
     * @param y Y坐标 / Y coordinate
     * @param w 宽度 / Width
     * @param h 高度 / Height
     */
    public void renderBackground(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
    	
    	
    }
    

    /** {@inheritDoc} */
    @Override
    public boolean onMousePressed(MouseButton button) {
        if (!isMouseOver()) return false;
        if(onClicked(button)) return true;
        if(item.length>0)
	        switch (button) {
	            case LEFT -> JEICompat.showJEIFor(item[order]);
	            case RIGHT -> JEICompat.showJEIUsageFor(item[order]);
	            default -> {
	                return false;
	            }
	        }
        return true;
    }
    /**
     * 物品槽被点击时的回调。子类可重写以实现自定义点击行为。
     * <p>
     * Callback when the item slot is clicked. Subclasses can override for custom click behavior.
     *
     * @param button 被点击的鼠标按键 / The mouse button that was clicked
     * @return 是否已处理点击事件 / Whether the click event was handled
     */
    public boolean onClicked(MouseButton button) {
    	return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean onKeyPressed(int keyCode, int scanCode, int modifier) {
        if (!isMouseOver()) return false;
        if(item.length>0)
	        switch (keyCode) {
	            case GLFW.GLFW_KEY_R -> {
	                if (modifier == GLFW.GLFW_MOD_SHIFT) {
	                    JEICompat.showJEIUsageFor(item[order]);
	                } else {
	                    JEICompat.showJEIFor(item[order]);
	                }
	            }
	            case GLFW.GLFW_KEY_U -> JEICompat.showJEIUsageFor(item[order]);
	            default -> {
	                return false;
	            }
	        }
        return true;
    }

    /**
     * 设置物品渲染缩放比例，并自动调整控件尺寸。
     * <p>
     * Sets the item rendering scale and auto-adjusts widget size.
     *
     * @param scale 缩放比例 / Scale factor
     */
    public void setScale(float scale) {
        this.scale = scale;
        setSize((int) (ITEM_WIDTH * scale), (int) (ITEM_HEIGHT * scale));
    }

    /** {@inheritDoc} */
    @Override
    public void getTooltip(TooltipBuilder tooltip) {
    	if(item.length>0)
    		item[order].getTooltipLines(ClientUtils.getPlayer(), TooltipFlag.NORMAL).forEach(tooltip);
    }
    /**
     * 清空物品槽中的物品。
     * <p>
     * Clears all items from the slot.
     */
    public void clear() {
    	this.setItem(EMPTY);
    }
}
