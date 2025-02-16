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

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.MouseCaptureUtil;
import com.teammoeg.chorda.client.MouseHelper;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.client.ui.ColorHelper;
import com.teammoeg.chorda.client.ui.Point;
import com.teammoeg.chorda.client.widget.IconButton;
import com.teammoeg.chorda.math.PlaneWorld;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.client.FHKeyMappings;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.teammoeg.frostedheart.compat.CompatModule;
import com.teammoeg.frostedheart.content.climate.network.C2SOpenClothesScreenMessage;
import com.teammoeg.frostedheart.content.health.network.C2SOpenNutritionScreenMessage;
import com.teammoeg.frostedheart.content.health.screen.NutritionScreen;
import com.teammoeg.frostedheart.content.tips.client.gui.DebugScreen;
import com.teammoeg.frostedheart.util.client.FGuis;
import dev.ftb.mods.ftbquests.client.FTBQuestsClient;
import dev.ftb.mods.ftbquests.item.FTBQuestsItems;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class WheelMenuRenderer {
	public static final IGuiOverlay OVERLAY = WheelMenuRenderer::render;

	public static final float WHEEL_OUTER_RADIUS = 100;
	public static final float WHEEL_INNER_RADIUS = 70;

	protected static final List<Selection> selections = new ArrayList<>();
	protected static final List<Selection> visibleSelections = new ArrayList<>();
	private static final List<Point> positions = new ArrayList<>();
	private static final List<Float> degrees = new ArrayList<>();
	@Getter
	protected static Selection hoveredSelection;
	protected static boolean mouseMoved = false;
	//create a virtual screen to track mouse movement
	protected static PlaneWorld virtualScreen=new PlaneWorld(-250,-250,250,250);
	@Getter
	public static boolean isOpened;
	public static boolean isClosing;
	static float openningStatus;
	public static void render(ForgeGui gui, GuiGraphics graphics, float partialTicks, int width, int height) {
		if(!isOpened)return;
		if (visibleSelections.isEmpty())
			return;
		float p;
		if(isClosing)
			p=Mth.clamp((openningStatus+(1-partialTicks)*2)/6f, 0, 1);
		else
			p=Mth.clamp((openningStatus+(partialTicks))/6f, 0, 1);
		int size = visibleSelections.size();
		int cw = ClientUtils.screenCenterX();
		int ch = ClientUtils.screenCenterY();
		var font = gui.getFont();
		var pose = graphics.pose();

		pose.pushPose();
		pose.translate(cw, ch, 0);
		pose.scale(p, p, p);

		// 背景圆环
		FGuis.drawRing(graphics, 0, 0, WHEEL_INNER_RADIUS, WHEEL_OUTER_RADIUS, 0, 360,
				ColorHelper.setAlpha(ColorHelper.BLACK, 0.5F * p));
		FGuis.drawRing(graphics, 0, 0, WHEEL_INNER_RADIUS - 4, WHEEL_INNER_RADIUS - 2, 0, 360,
				ColorHelper.setAlpha(ColorHelper.BLACK, 0.5F * p));


		
		float halfSliceSize = 360F / (size * 2);
		double radian = Math.atan2(virtualScreen.getX() , -(virtualScreen.getY()));
		double degree = Math.toDegrees(radian);
		if (degree < 0)
			degree += 360;
		int selectedIndex = findIndex(degree + halfSliceSize, size);
		if (mouseMoved) {
			Selection lastHovered = hoveredSelection;
			hoveredSelection = visibleSelections.get(selectedIndex);
			if (hoveredSelection != lastHovered) {
				hoveredSelection.hoverAction.execute(lastHovered);
			}
		} else {
			mouseMoved = !MouseHelper.isMouseIn(virtualScreen.getX(), virtualScreen.getY(), -25, -25, 50,50);
			hoveredSelection = null;
		}
		// 跟随鼠标移动的细圆环
		pose.pushPose();
		pose.rotateAround(new Quaternionf().rotateZ((float) radian), 0, 0, 0);
		FGuis.drawRing(graphics, 0, 0, WHEEL_INNER_RADIUS - 4, WHEEL_INNER_RADIUS - 2, -halfSliceSize, halfSliceSize,
				ColorHelper.setAlpha(ColorHelper.CYAN, p));
		pose.popPose();

		// 选择选项的圆环
		pose.pushPose();
		pose.rotateAround(new Quaternionf().rotateZ((float) Math.toRadians(degrees.get(selectedIndex))), 0, 0, 0);
		FGuis.drawRing(graphics, 0, 0, WHEEL_INNER_RADIUS, WHEEL_OUTER_RADIUS, -halfSliceSize, halfSliceSize,
				ColorHelper.setAlpha(ColorHelper.CYAN, 0.25F * p));
		pose.popPose();

		// 渲染选项
		if (size == positions.size())
			for (int i = 0; i < size; i++) {
				visibleSelections.get(i).setPosition(positions.get(i).getX(), positions.get(i).getY());
				visibleSelections.get(i).render(gui, graphics, partialTicks, width, height);
			}

		// 渲染选项标题
		if (hoveredSelection != null) {
			CGuiHelper.drawCenteredStrings(graphics, font,
					font.split(hoveredSelection.getMessage(), (int) (WHEEL_INNER_RADIUS * 2 - 16)), 0, -4,
					hoveredSelection.color, 10, true, true);
		} else {
			Component c = Component.translatable("gui.frostedheart.wheel_menu.message",
					FHKeyMappings.key_openWheelMenu.get().getKey().getDisplayName().getString());
			var texts = font.split(c, (int) (WHEEL_INNER_RADIUS * 2 - 16));
			CGuiHelper.drawCenteredStrings(graphics, font, texts, 0, -texts.size() * 5, ColorHelper.CYAN, 10, true,
					true);
		}
		pose.popPose();
	}

	private static int findIndex(double degrees, int size) {
		float sliceSize = 360F / size;
		for (int i = 1; i <= size; i++) {
			if (degrees <= sliceSize * i) {
				return Math.max(i - 1, 0);
			}
		}
		return 0;
	}

	protected static void init() {
		selections.clear();
		visibleSelections.clear();
		positions.clear();
		degrees.clear();
		// 在此处添加轮盘选项
		if (CompatModule.isFTBQLoaded()) {
			addSelection(new Selection(Component.translatable("key.ftbquests.quests"), FTBQuestsItems.BOOK,
					s -> FTBQuestsClient.openGui()));
		} else {
			
			addSelection(new Selection(Component.literal("Open Quests"), Items.BOOK.getDefaultInstance(),
					Selection.NO_ACTION));
		}
		addSelection(new Selection(Component.translatable("gui.frostedheart.wheel_menu.selection.debug"),
				FHItems.debug_item.get().getDefaultInstance(), ColorHelper.CYAN,
				s -> ClientUtils.getPlayer().isCreative(), s -> DebugScreen.openDebugScreen(), Selection.NO_ACTION));
		addSelection(new Selection(Component.translatable("gui.frostedheart.wheel_menu.selection.nutrition"),
				NutritionScreen.fat_icon, s -> FHNetwork.sendToServer(new C2SOpenNutritionScreenMessage())));
		addSelection(new Selection(Component.translatable("gui.frostedheart.wheel_menu.selection.clothing"),
				FHItems.gambeson.get().getDefaultInstance(),
				s -> FHNetwork.sendToServer(new C2SOpenClothesScreenMessage())));
	}

	private static void update() {
		positions.clear();
		int size = visibleSelections.size();
		double averageRadius = (WHEEL_INNER_RADIUS + WHEEL_OUTER_RADIUS) / 2.0;
		double angleStep = 2 * Math.PI / size;
		for (int i = 0; i < size; i++) {
			double theta = Math.PI / 2 - i * angleStep;
			double x = averageRadius * Math.cos(theta);
			double y = averageRadius * Math.sin(theta);
			positions.add(new Point((int) x, (int) -y));
		}

		degrees.clear();
		float sliceSize = 360F / size;
		for (int i = 0; i < size; i++) {
			float angle = i * sliceSize;
			degrees.add(angle);
		}
	}

	public static void tick() {
		if(ClientUtils.getPlayer()==null) {//not in world
			openningStatus=0;
			onClose();
		}
		if (FHKeyMappings.key_openWheelMenu.get().isDown()) {
			if(!isOpened) {
				MouseCaptureUtil.setCaptureMouse(true);
				init();
				isOpened=true;
			}else {
				virtualScreen.addPos(MouseCaptureUtil.getAndResetCapturedX(),MouseCaptureUtil.getAndResetCapturedY());
				
				if(openningStatus<6)
				openningStatus++;
			}
		} else {
			if(openningStatus>0) {
				isClosing=true;
				openningStatus-=2;
				if (openningStatus <= 0) {
					openningStatus=0;
					isClosing=false;
					onClose();
				}
			}
		}
		if(isOpened) {
			boolean shouldUpdate = false;
			for (Selection selection : selections) {
				selection.tick();
				if (!selection.visible) {
					visibleSelections.remove(selection);
					shouldUpdate = true;
				} else if (!visibleSelections.contains(selection)) {
					visibleSelections.add(selection);
					shouldUpdate = true;
				}
			}
			if (shouldUpdate)
				update();
		}
	}

	public static void onClose() {
		
		MouseCaptureUtil.setCaptureMouse(false);
		virtualScreen.reset();
		isOpened=false;
		if(ClientUtils.getPlayer()!=null&&hoveredSelection!=null) {
			hoveredSelection.selectAction.execute(hoveredSelection);
		}
	}

	private static void addSelection(Selection selection) {
		selections.add(selection);
	}

	public static class Selection {
		public static final Predicate<Selection> ALWAYS_VISIBLE = s -> true;
		public static final Action NO_ACTION = s -> {
		};

		protected final Predicate<Selection> visibility;
		protected final Action selectAction;
		protected final Action hoverAction;
		public final Object icon;
		public final IconType iconType;
		@Getter
		protected Component message;
		@Getter
		protected boolean visible;
		@Getter
		protected boolean hovered;
		public int color;
		public int x, y;

		/**
		 * @param icon        {@link ItemStack}, {@link IconButton.Icon},
		 *                    {@link Component}, {@code null}
		 * @param pressAction 选择后的行动
		 */
		public Selection(Component message, Object icon, Action pressAction) {
			this(message, icon, ColorHelper.CYAN, ALWAYS_VISIBLE, pressAction, NO_ACTION);
		}

		/**
		 * @param icon        {@link ItemStack}, {@link IconButton.Icon},
		 *                    {@link Component}, {@code null}
		 * @param color       图标为 {@link IconButton.Icon} 时的颜色
		 * @param visibility  选项在什么情况下可见，每tick更新
		 * @param pressAction 选择后的行动 (选中 -> 松开Tab)
		 * @param hoverAction 选中选项后的行动
		 */
		public Selection(Component message, Object icon, int color, Predicate<Selection> visibility, Action pressAction,
				Action hoverAction) {
			this.message = message;
			this.iconType = getIconType(icon);
			this.icon = icon;
			this.color = color;
			this.visibility = visibility;
			this.selectAction = pressAction;
			this.hoverAction = hoverAction;
		}

		protected void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
			if (!visible)
				return;
			renderSelection(gui, graphics, partialTick, width, height);
			if (hovered) {
				renderWhenHovered(gui, graphics, partialTick, width, height);
			}
		}

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
			hovered = hoveredSelection == this;
			visible = visibility.test(this);
		}

		public void setPosition(int x, int y) {
			this.x = x;
			this.y = y;
		}

		public static IconType getIconType(Object icon) {
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
}
