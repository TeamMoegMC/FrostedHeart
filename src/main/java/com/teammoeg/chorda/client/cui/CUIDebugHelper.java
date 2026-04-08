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

package com.teammoeg.chorda.client.cui;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.cui.screenadapter.CUIScreen;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.math.Colors;
import com.teammoeg.chorda.math.Rect;
import com.teammoeg.frostedheart.FrostedHud;
import com.teammoeg.frostedheart.util.Lang;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

import java.awt.*;

/**
 * CUI调试辅助工具类，控制CUI框架的调试模式开关。
 * 调试模式仅在原版F3调试界面开启时可用，启用后会在UI元素周围显示边框等调试信息。
 * <p>
 * CUI debug helper utility controlling the debug mode toggle for the CUI framework.
 * Debug mode is only available when the vanilla F3 debug screen is enabled;
 * when active, it displays debug information such as borders around UI elements.
 */
public class CUIDebugHelper {
	private static boolean isDebugEnabled;
	private CUIDebugHelper() {

	}
	public static boolean isDebugEnabled() {
		return ClientUtils.getMc().options.renderDebug&&isDebugEnabled;
		
	}
	public static void toggleDebug() {
		if(ClientUtils.getMc().options.renderDebug)
			isDebugEnabled=!isDebugEnabled;
	}

	public static void renderDebug(GuiGraphics graphics, int x, int y, RenderingHint hint, CUIScreen manager) {
		if (!isDebugEnabled() || Screen.hasAltDown() || !shouldRender(manager)) return;
		// debug
		var ele = UILayer.hoveredEle;
		graphics.pose().pushPose();
		graphics.pose().translate(0, 0, 1200);
		assert ele != null;
		boolean shift = Screen.hasShiftDown();
		int depth = 0;
		var parent = ele;
		while (parent != null) {
			parent = parent.getParent();
			depth++;
		}
		// 子元素
		if (ele instanceof UILayer layer) {
			for (UIElement cele : layer.getElements()) {
				if (cele.isVisible() || shift) {
					CGuiHelper.drawBox(graphics, cele.getScreenX()+ x, cele.getScreenY()+ y, cele.getWidth(), cele.getHeight(), Color.HSBtoRGB((depth+2) / 6F, 1, 1), false);
				}
			}
		}
		// 选中的元素
		int color = getDepthColor(depth);
		var bound = new Rect(ele.getScreenX()+ x, ele.getScreenY()+ y, ele.getWidth(), ele.getHeight());
		ele.renderDebug(graphics, x, y, ele.getX(), ele.getHeight(), hint, depth+1);
		CGuiHelper.drawRect(graphics, bound, Colors.setAlpha(color, 0.1F));
		CGuiHelper.drawBox(graphics, bound, color, false);
		graphics.pose().popPose();
	}

	public static int getDepthColor(int depth) {
		return Color.HSBtoRGB((depth + 1) / 6F, 1, 1);
	}

	public static void getDebugTooltip(TooltipBuilder list, Font font, CUIScreen manager) {
		if (!Screen.hasControlDown() || !shouldRender(manager)) {
			UILayer.hoveredEle = null;
			return;
		}
		// debug
		list.translateZ(1200);
		if (!list.isEmpty())
			list.add(net.minecraft.network.chat.Component.literal(" "));
		var ele = UILayer.hoveredEle;
		var b = ele.getBounds();
		var gray = ChatFormatting.GRAY;
		var color = ChatFormatting.GOLD;
		// 标题
		if (!ele.getTitle().getString().isBlank()) {
			var title = net.minecraft.network.chat.Component.literal("T: ").withStyle(gray);
			if (font.width(ele.getTitle()) > 200) {
				var t = FormattedText.composite(font.substrByWidth(ele.getTitle(), 200), CommonComponents.ELLIPSIS).getString();
				title.append(net.minecraft.network.chat.Component.literal(t).withStyle(color));
			} else {
				title.append(net.minecraft.network.chat.Component.empty().append(ele.getTitle()).withStyle(color));
			}
			list.add(title);
		}
		// 类名
		String clazz = ele.getClass().getSimpleName();
		clazz = clazz.isBlank() ? "extends " + ele.getClass().getSuperclass().getSimpleName() : clazz;
		list.add(net.minecraft.network.chat.Component.literal("C: ")
				.withStyle(gray)
				.append(net.minecraft.network.chat.Component.literal(clazz).withStyle(color)));
		// 父元素类名
		if (ele.getParent() != null) {
			list.add(Lang.builder().style(gray)
					.text("P: ")
					.add(net.minecraft.network.chat.Component.literal(FrostedHud.tryGetTitle(ele.getParent())).withStyle(color))
					.component());
		}
		// X 和 Y
		list.add(Lang.builder().style(gray)
				.text("L: x=")
				.add(net.minecraft.network.chat.Component.literal(String.valueOf(ele.getX())).withStyle(color))
				.text(", y=")
				.add(net.minecraft.network.chat.Component.literal(String.valueOf(ele.getY())).withStyle(color))
				.component());
		// 在屏幕中的 X 和 Y
		list.add(Lang.builder().style(gray)
				.text("S: x=")
				.add(net.minecraft.network.chat.Component.literal(String.valueOf(b.getX())).withStyle(color))
				.text(", y=")
				.add(net.minecraft.network.chat.Component.literal(String.valueOf(b.getY())).withStyle(color))
				.text(", w=")
				.add(net.minecraft.network.chat.Component.literal(String.valueOf(b.getW())).withStyle(color))
				.text(", h=")
				.add(net.minecraft.network.chat.Component.literal(String.valueOf(b.getH())).withStyle(color))
				.component());
		// 开启和可见
		list.add(net.minecraft.network.chat.Component.empty()
				.append(net.minecraft.network.chat.Component.literal("Enable" ).withStyle(ele.isEnabled() ? ChatFormatting.GREEN : ChatFormatting.RED))
				.append(net.minecraft.network.chat.Component.literal(" | "))
				.append(Component.literal("Visible").withStyle(ele.isVisible() ? ChatFormatting.GREEN : ChatFormatting.RED)));
		UILayer.hoveredEle = null;
	}

	public static boolean shouldRender(CUIScreen manager) {
		if (!isDebugEnabled() || ClientUtils.getMc().screen == null) return false;

		var ele = UILayer.hoveredEle;
		if (ele == null) return false;

		var p = ele;
		while (p != null) {
			if (p.getParent() == manager.getPrimaryLayer()) {
				return true;
			}
			p = p.getParent();
		}
		return false;
	}

}
