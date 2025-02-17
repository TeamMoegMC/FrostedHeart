package com.teammoeg.frostedheart.content.wheelmenu;

import java.util.function.Predicate;

import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.client.ui.ColorHelper;
import com.teammoeg.chorda.client.widget.IconButton;

import lombok.Getter;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;

public class Selection {
	public static final Predicate<Selection> ALWAYS_VISIBLE = s -> true;
	public static final Selection.Action NO_ACTION = s -> {};

	protected final Predicate<Selection> visibility;
	protected final Selection.Action selectAction;
	protected final Selection.Action hoverAction;
	public final CIcon icon;
	@Getter
	protected Component message;
	@Getter
	protected boolean visible;
	@Getter
	protected boolean hovered;
	public int color;
	@Getter
	private final int priority;

	/**
	 * @param icon        {@link ItemStack}, {@link IconButton.Icon},
	 *                    {@link Component}, {@code null}
	 * @param selectAction 选择后的行动 (选中 -> 松开Tab)
	 */
	public Selection(Component message, CIcon icon,int priority, Selection.Action selectAction) {
		this(message, icon, ColorHelper.CYAN,priority, ALWAYS_VISIBLE, selectAction, NO_ACTION);
	}

	/**
	 * @param icon        {@link ItemStack}, {@link IconButton.Icon},
	 *                    {@link Component}, {@code null}
	 * @param color       图标为 {@link IconButton.Icon} 时的颜色
	 * @param visibility  选项在什么情况下可见，每tick更新
	 * @param selectAction 选择后的行动 (选中 -> 松开Tab)
	 * @param hoverAction 选中选项后的行动
	 */
	public Selection(Component message, CIcon icon, int color,int priority, Predicate<Selection> visibility, Selection.Action selectAction,
			Selection.Action hoverAction) {
		this.message = message;
		this.icon = icon;
		this.color = color;
		this.visibility = visibility;
		this.selectAction = selectAction;
		this.hoverAction = hoverAction;
		this.priority=priority;
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
		visible = visibility.test(this);
	}


	@OnlyIn(Dist.CLIENT)
	public interface Action {
		void execute(Selection selection);
	}
	public static class KeyMappingTriggerAction implements Action{
		private KeyMapping km;
		public KeyMappingTriggerAction(String name) {
			super();
		}
		@Override
		public void execute(Selection selection) {
			
			
		}
		
	}
}