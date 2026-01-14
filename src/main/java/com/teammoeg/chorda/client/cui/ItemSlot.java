package com.teammoeg.chorda.client.cui;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.client.ui.Colors;
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

@Setter
@Getter
public class ItemSlot extends UIElement {
    public static final int ITEM_WIDTH = 16;
    public static final int ITEM_HEIGHT = 16;
    private static final ItemStack[] EMPTY=new ItemStack[0];
    protected ItemStack[] item;
    protected float scale;
    protected boolean hoverOverlayEnabled = true;
    private int order=0;
 
    private int countOverride=0;
    public ItemSlot(UIElement parent) {
        this(parent,EMPTY);
        setScale(1);
    }
    
    public ItemSlot(UIElement parent, ItemStack item) {
        this(parent);
        this.setItem(item);
        
    }

    public ItemSlot(UIElement parent, Ingredient item) {
        this(parent);
        this.setItem(item);
    }
    public ItemSlot(UIElement parent, ItemStack[] item) {
        super(parent);
        this.setItem(item);
        
    }
    public void setItem(Ingredient item) {
    	this.setItem(item.getItems());
    }
    public void setItem(ItemStack item) {
    	this.setItem(new ItemStack[] {item});
    }
    public void setItem(ItemStack[] item) {
    	this.item=item;
    }
    @Override
    public void render(GuiGraphics graphics, int x, int y, int w, int h) {
    	
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
    public boolean onClicked(MouseButton button) {
    	return false;
    }
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

    public void setScale(float scale) {
        this.scale = scale;
        setSize((int) (ITEM_WIDTH * scale), (int) (ITEM_HEIGHT * scale));
    }

    @Override
    public void getTooltip(TooltipBuilder tooltip) {
    	if(item.length>0)
    		item[order].getTooltipLines(ClientUtils.getPlayer(), TooltipFlag.NORMAL).forEach(tooltip);
    }
    public void clear() {
    	this.setItem(EMPTY);
    }
}
