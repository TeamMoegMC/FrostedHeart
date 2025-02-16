package com.teammoeg.frostedheart.content.wheelmenu;

import java.util.function.Predicate;

import com.teammoeg.chorda.client.ui.ColorHelper;
import com.teammoeg.chorda.client.widget.IconButton;

import lombok.Getter;
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
	public final Object icon;
	public final Selection.IconType iconType;
	@Getter
	protected Component message;
	@Getter
	protected boolean visible;
	@Getter
	protected boolean hovered;
	public int color;
	public int x, y;
	@Getter
	private final int priority;

	/**
	 * @param icon        {@link ItemStack}, {@link IconButton.Icon},
	 *                    {@link Component}, {@code null}
	 * @param selectAction 选择后的行动 (选中 -> 松开Tab)
	 */
	public Selection(Component message, Object icon,int priority, Selection.Action selectAction) {
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
	public Selection(Component message, Object icon, int color,int priority, Predicate<Selection> visibility, Selection.Action selectAction,
			Selection.Action hoverAction) {
		this.message = message;
		this.iconType = getIconType(icon);
		this.icon = icon;
		this.color = color;
		this.visibility = visibility;
		this.selectAction = selectAction;
		this.hoverAction = hoverAction;
		this.priority=priority;
	}

	protected void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
		if (!visible)
			return;
		renderSelection(gui, graphics, partialTick, width, height);
		if (hovered) {
			renderWhenHovered(gui, graphics, partialTick, width, height);
		}
	}

	@SuppressWarnings("unused")
	protected void renderSelection(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
		switch (iconType) {
		case ITEM -> graphics.renderItem((ItemStack) icon, x - 8, y - 8);
		case ICON -> {
			var flatIcon = (IconButton.Icon) icon;
			int x1 = x - flatIcon.size.width / 2;
			int y1 = y - flatIcon.size.height / 2;
			flatIcon.render(graphics.pose(), x1, y1, color);
		}
		case COMPONENT -> {
			var font = gui.getFont();
			int textWidth = font.width((Component) icon) / 2;
			if (textWidth <= 4) {
				var pose = graphics.pose();
				pose.pushPose();
				pose.translate(x, y, 0);
				pose.scale(2, 2, 2);
				graphics.drawString(font, (Component) icon, -textWidth, -4, color, false);
				pose.popPose();
			} else {
				graphics.drawString(font, (Component) icon, x - textWidth, y - 4, color, false);
			}
		}
		}
	}

	/**
	 * 选中选项时渲染
	 */
	@SuppressWarnings("unused")
	protected void renderWhenHovered(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
	}

	protected void tick() {
		hovered = WheelMenuRenderer.hoveredSelection == this;
		visible = visibility.test(this);
	}

	public void setPosition(int x, int y) {
		this.x = x;
		this.y = y;
	}
	public static Selection.IconType getIconType(Object icon) {
		if (icon instanceof ItemStack)
			return IconType.ITEM;
		if (icon instanceof IconButton.Icon)
			return IconType.ICON;
		if (icon instanceof Component)
			return IconType.COMPONENT;
		return IconType.EMPTY;
	}

	public enum IconType {
		EMPTY, ITEM, ICON, COMPONENT,
	}

	@OnlyIn(Dist.CLIENT)
	public interface Action {
		void execute(Selection selection);
	}
}