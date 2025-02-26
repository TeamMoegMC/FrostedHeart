package com.teammoeg.frostedheart.content.wheelmenu;

import java.util.function.Predicate;

import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.client.ui.ColorHelper;
import com.teammoeg.chorda.client.widget.IconButton;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.frostedheart.content.wheelmenu.useractions.KeyMappingTriggerAction;

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
	public int color;
	public Selection(UserSelection sel) {
		this(sel.getParsedMessage(),sel.icon(),sel.selectAction()==null?Selection.NO_ACTION:sel.selectAction());
	}
	/**
	 * @param icon        {@link ItemStack}, {@link IconButton.Icon},
	 *                    {@link Component}, {@code null}
	 * @param selectAction 选择后的行动 (选中 -> 松开Tab)
	 */
	public Selection(Component message, CIcon icon, Action selectAction) {
		this(message, icon, ColorHelper.CYAN, ALWAYS_VISIBLE, selectAction, NO_ACTION);
	}

	/**
	 * @param icon        {@link ItemStack}, {@link IconButton.Icon},
	 *                    {@link Component}, {@code null}
	 * @param color       图标为 {@link IconButton.Icon} 时的颜色
	 * @param visibility  选项在什么情况下可见，每tick更新
	 * @param selectAction 选择后的行动 (选中 -> 松开Tab)
	 * @param hoverAction 选中选项后的行动
	 */
	public Selection(Component message, CIcon icon, int color, Predicate<Selection> visibility, Action selectAction,
			Action hoverAction) {
		this.message = message;
		this.icon = icon;
		this.color = color;
		this.visibility = visibility;
		this.selectAction = selectAction;
		this.hoverAction = hoverAction;
	}
	public Selection(String keyDesc, CIcon icon) {
		this(Components.translatable(keyDesc), icon, ColorHelper.CYAN, ALWAYS_VISIBLE, new KeyMappingTriggerAction(keyDesc), NO_ACTION);
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
	 * Disable this makes this selection could only activate by program
	 * */
	public boolean isAutoAddable() {
		return true;
	}
}