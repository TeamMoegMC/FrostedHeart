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
import com.teammoeg.chorda.client.cui.editor.EditUtils;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.client.ui.ColorHelper;
import com.teammoeg.chorda.client.ui.Point;
import com.teammoeg.chorda.client.widget.IconButton;
import com.teammoeg.chorda.math.CircleDimension;
import com.teammoeg.chorda.math.Dimension2D;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.client.FHKeyMappings;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.teammoeg.frostedheart.compat.CompatModule;
import com.teammoeg.frostedheart.content.climate.network.C2SOpenClothesScreenMessage;
import com.teammoeg.frostedheart.content.health.network.C2SOpenNutritionScreenMessage;
import com.teammoeg.frostedheart.content.health.screen.NutritionScreen;
import com.teammoeg.frostedheart.content.tips.client.gui.DebugScreen;
import com.teammoeg.frostedheart.content.wheelmenu.Selection.UserSelection;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.util.client.FGuis;
import dev.ftb.mods.ftbquests.client.FTBQuestsClient;
import dev.ftb.mods.ftbquests.item.FTBQuestsItems;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class WheelMenuRenderer {
	public static final IGuiOverlay OVERLAY = WheelMenuRenderer::render;

	protected static float wheelRadius = 60;
	protected static float ringWidth = 30;

	protected static final Set<Selection> selections = new TreeSet<>(
			Comparator.comparingInt(Selection::getPriority));
	protected static final List<Selection> visibleSelections = new ArrayList<>();
	public static final List<UserSelection> userConfiguredSelections=new ArrayList<>();

	@Override
	public String toString() {
		return "WheelMenuRenderer{}";
	}

	private static final List<Point> positions = new ArrayList<>();
	private static final List<Float> degrees = new ArrayList<>();
	@Getter
	protected static Selection hoveredSelection;
	protected static boolean mouseMoved = false;
	// create a virtual screen to track mouse movement
	protected static Dimension2D virtualScreen = new CircleDimension(wheelRadius * 2);
	@Getter
	public static boolean isOpened;
	public static boolean isClosing;
	static float openingStatus;

	public static void render(ForgeGui gui, GuiGraphics graphics, float partialTicks, int width, int height) {
		if (!isOpened || visibleSelections.isEmpty())
			return;
		float p;
		if (isClosing)
			p = Mth.clamp((openingStatus + (1 - partialTicks) * 2) / 3f, 0, 1);
		else
			p = Mth.clamp((openingStatus + partialTicks) / 3f, 0, 1);
		int size = visibleSelections.size();
		int cw = ClientUtils.screenCenterX();
		int ch = ClientUtils.screenCenterY();
		var font = gui.getFont();
		var pose = graphics.pose();
		float innerRadius = wheelRadius - ringWidth;
		virtualScreen.addPos(MouseCaptureUtil.getAndResetCapturedDeltaX(),
				MouseCaptureUtil.getAndResetCapturedDeltaY());

		pose.pushPose();
		pose.translate(cw, ch, 0);
		pose.scale(p, p, p);

		// 背景圆环
		FGuis.drawRing(graphics, 0, 0, innerRadius, wheelRadius, 0, 360,
				ColorHelper.setAlpha(ColorHelper.BLACK, 0.5F * p));
		FGuis.drawRing(graphics, 0, 0, innerRadius - 4, innerRadius - 2, 0, 360,
				ColorHelper.setAlpha(ColorHelper.BLACK, 0.5F * p));

		float halfSliceSize = 360F / (size * 2);

		if (mouseMoved) {
			double radian = Math.atan2(virtualScreen.getX(), -(virtualScreen.getY()));
			double degree = Math.toDegrees(radian);
			if (degree < 0)
				degree += 360;
			int selectedIndex = findIndex(degree + halfSliceSize, size);
			Selection lastHovered = hoveredSelection;
			hoveredSelection = visibleSelections.get(selectedIndex);
			if (hoveredSelection != lastHovered) {
				hoveredSelection.hoverAction.execute(lastHovered);
			}
			// 跟随鼠标移动的细圆环
			pose.pushPose();
			pose.rotateAround(new Quaternionf().rotateZ((float) radian), 0, 0, 0);
			FGuis.drawRing(graphics, 0, 0, innerRadius - 4, innerRadius - 2, -halfSliceSize, halfSliceSize,
					ColorHelper.setAlpha(ColorHelper.CYAN, p));
			pose.popPose();

			// 当前选择的选项的圆环
			pose.pushPose();
			pose.rotateAround(new Quaternionf().rotateZ((float) Math.toRadians(degrees.get(selectedIndex))), 0, 0, 0);
			FGuis.drawRing(graphics, 0, 0, innerRadius, wheelRadius, -halfSliceSize, halfSliceSize,
					ColorHelper.setAlpha(ColorHelper.CYAN, 0.25F * p));
			pose.popPose();
		} else {
			mouseMoved = !MouseHelper.isMouseIn(virtualScreen.getX(), virtualScreen.getY(), -50, -50, 100, 100);
			hoveredSelection = null;
		}
		// 渲染选项
		if (size == positions.size())
			for (int i = 0; i < size; i++) {
				visibleSelections.get(i).render(gui, graphics, partialTicks,positions.get(i).getX(), positions.get(i).getY(), 16, 16);
			}

		// 渲染“鼠标”
		FGuis.drawRing(graphics, (int) virtualScreen.getX()/2, (int) virtualScreen.getY()/2, 3, 6, 0, 360,
				ColorHelper.setAlpha(ColorHelper.CYAN, p));

		// 渲染选项标题
		var message = hoveredSelection != null ? hoveredSelection.getMessage() : Component.translatable("gui.frostedheart.wheel_menu.message",
				FHKeyMappings.key_openWheelMenu.get().getKey().getDisplayName());
		var lines = font.split(message, (int) (innerRadius * 2 - 16));
		CGuiHelper.drawCenteredStrings(graphics, font, lines, 0, -lines.size() * 5, ColorHelper.CYAN, 10, true, true);
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

	protected static boolean init() {
		selections.clear();
		visibleSelections.clear();
		positions.clear();
		degrees.clear();
		wheelRadius = FHConfig.CLIENT.wheelMenuRadius.get();
		ringWidth = 30 * Math.max(1, wheelRadius / FHConfig.CLIENT.wheelMenuRadius.getDefault());
		((CircleDimension)virtualScreen).setRadius(wheelRadius * 2);

		boolean rslt=!MinecraftForge.EVENT_BUS.post(new WheelMenuInitEvent(WheelMenuRenderer::addSelection));
		if(rslt) {
			// 在此处添加轮盘选项
			addSelection(new Selection(Component.translatable("gui.close"), IconButton.Icon.CROSS.toCIcon(), 0, Selection.NO_ACTION));
			addSelection(new Selection(Component.translatable("gui.wheel_menu.editor.edit"), IconButton.Icon.LIST.toCIcon(), -1, s->{
				EditUtils.edit(WheelMenuEditors.SELECTION_LIST_EDITOR, Component.translatable("gui.wheel_menu.editor.edit"), userConfiguredSelections, t->{
					userConfiguredSelections.clear();
					userConfiguredSelections.addAll(t);
				});
			}));
			if (CompatModule.isFTBQLoaded()) {
				addSelection(new Selection(Component.translatable("key.ftbquests.quests"), CIcons.getIcon(FTBQuestsItems.BOOK.get()), 10,
						s -> FTBQuestsClient.openGui()));
			}
			addSelection(new Selection("key.curios.open.desc",CIcons.getIcon(FHItems.heater_vest), 50));
			
			

			addSelection(new Selection(Component.translatable("gui.frostedheart.wheel_menu.selection.debug"),
					CIcons.getIcon(FHItems.debug_item), ColorHelper.CYAN, 20,
					s -> ClientUtils.getPlayer().isCreative(), s -> DebugScreen.openDebugScreen(), Selection.NO_ACTION));

			addSelection(new Selection(Component.translatable("gui.frostedheart.wheel_menu.selection.nutrition"),
				CIcons.getIcon(NutritionScreen.fat_icon), 30, s -> FHNetwork.sendToServer(new C2SOpenNutritionScreenMessage())));

			addSelection(new Selection(Component.translatable("gui.frostedheart.wheel_menu.selection.clothing"),
				CIcons.getIcon(FHItems.gambeson), 40,
					s -> FHNetwork.sendToServer(new C2SOpenClothesScreenMessage())));

			int order=0;
			for(UserSelection i:userConfiguredSelections) {
				addSelection(i.createSelection(10000+(order++)));
			}
		}
		return rslt;
	}

	private static void update() {
		positions.clear();
		int size = visibleSelections.size();
		double averageRadius = (wheelRadius + wheelRadius - ringWidth) / 2.0;
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

	public static void open() {
		if (init() && !selections.isEmpty()) {
			MouseCaptureUtil.startMouseCapture();
			isOpened = true;
		} 
	}

	public static void tick() {
		if (ClientUtils.getPlayer() == null) {// not in world
			openingStatus = 0;
			onClose();
		}
		if (isOpened) {
			if (FHKeyMappings.key_openWheelMenu.get().isDown()) {
				isClosing = false;
				if (openingStatus < 6)
					openingStatus++;
			} else {
				isClosing = true;
				openingStatus -= 2;
				if (openingStatus <= 0) {
					openingStatus = 0;
					isClosing = false;
					onClose();
					return;
				}
				
			}
			int prevsize=visibleSelections.size();
			visibleSelections.clear();
			for (Selection selection : selections) {
				selection.tick();
				if (selection.visible) {
					visibleSelections.add(selection);
				}
			}
			if (prevsize!=visibleSelections.size())
				update();
		}
	}

	public static void onClose() {
		MouseCaptureUtil.stopMouseCapture();
		virtualScreen.reset();
		isOpened = false;
		mouseMoved = false;
		if (ClientUtils.getPlayer() != null && hoveredSelection != null) {
			hoveredSelection.selectAction.execute(hoveredSelection);
		}
	}

	private static void addSelection(Selection selection) {
		selections.add(selection);
	}
}
