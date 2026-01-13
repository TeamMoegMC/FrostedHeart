package com.teammoeg.chorda.client.cui;

import java.util.function.Consumer;

import com.teammoeg.chorda.client.icon.CIcons.CIcon;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public class OverlayItemSlot extends ItemSlot {
	@Getter
	@Setter
	protected CIcon overlay;
	@Getter
	@Setter
	protected int overlayWidth;
	@Getter
	@Setter
	protected int overlayHeight;
	@Getter
	@Setter
	Consumer<Consumer<Component>> tooltips;
	public OverlayItemSlot(UIElement parent) {
		super(parent);
	}
	
	public OverlayItemSlot(UIElement parent, ItemStack item) {
		super(parent, item);
	}

	public OverlayItemSlot(UIElement parent, Ingredient item) {
		super(parent, item);
	}

	public OverlayItemSlot(UIElement parent, ItemStack[] item) {
		super(parent, item);
	}

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
    public void setOverlay(CIcon overlay, int height, int width) {
        this.overlay = overlay;
        this.overlayHeight = height;
        this.overlayWidth = width;
    }
    public void resetOverlay() {
        this.overlay = null;
    }

	@Override
	public void getTooltip(Consumer<Component> tooltip) {
		super.getTooltip(tooltip);
		if(tooltips!=null)
			tooltips.accept(tooltip);
	}
    
}
