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

import com.teammoeg.chorda.client.AnimationUtil;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.MouseHelper;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.client.ui.ColorHelper;
import com.teammoeg.chorda.client.ui.Point;
import com.teammoeg.chorda.client.widget.IconButton;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.client.FHKeyMappings;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.teammoeg.frostedheart.content.climate.network.C2SOpenClothesScreenMessage;
import com.teammoeg.frostedheart.content.health.network.C2SOpenNutritionScreenMessage;
import com.teammoeg.frostedheart.content.health.screen.NutritionScreen;
import com.teammoeg.frostedheart.content.tips.Popup;
import com.teammoeg.frostedheart.util.client.FGuis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class WheelMenuScreen extends Screen {
    private static final String OPEN_ANIM_NAME = WheelMenuScreen.class.getName() + "opening";
    public static final float WHEEL_OUTER_RADIUS = 100;
    public static final float WHEEL_INNER_RADIUS = 70;
    private final List<Selection> selections = new ArrayList<>();
    private final List<Selection> visibleSelections = new ArrayList<>();
    private final List<Point> positions = new ArrayList<>();
    private final List<Float> degrees = new ArrayList<>();
    public boolean showing = false;
    private Selection hovered;
    private Selection lastHovered;

    public WheelMenuScreen() {
        super(Component.empty());
//        if (Popup.isEmpty()) {
//            Popup.put("Release Tab to select");
//        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        showing = true;
        float p = AnimationUtil.fadeIn(300, OPEN_ANIM_NAME, false);
        int size = visibleSelections.size();
        int cw = ClientUtils.screenCenterX();
        int ch = ClientUtils.screenCenterY();
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(cw, ch, 0);
        pose.scale(p, p, p);

        // 背景圆环
        FGuis.drawRing(graphics,
                0,
                0,
                WHEEL_INNER_RADIUS,
                WHEEL_OUTER_RADIUS,
                0,
                360,
                ColorHelper.setAlpha(ColorHelper.BLACK, 0.5F * p));
        FGuis.drawRing(graphics,
                0,
                0,
                WHEEL_INNER_RADIUS - 4,
                WHEEL_INNER_RADIUS - 2,
                0,
                360,
                ColorHelper.setAlpha(ColorHelper.BLACK, 0.5F * p));

        if (visibleSelections.isEmpty()) return;

        float halfSliceSize = 360F / (size * 2);
        double radian = Math.atan2(mouseX-cw, -(mouseY-ch));
        double degree = Math.toDegrees(radian);
        if (degree < 0) degree += 360;
        int selectedIndex = findBlockIndex(degree + halfSliceSize, size);
        if (!MouseHelper.isMouseIn(mouseX, mouseY, cw-8, ch-8, 16, 16)) {
            lastHovered = hovered;
            hovered = visibleSelections.get(selectedIndex);
            if (hovered != lastHovered) {
                hovered.hoverAction.onPress(lastHovered);
            }
        } else {
            hovered = null;
            lastHovered = null;
        }
        // 跟随鼠标移动的细圆环
        pose.pushPose();
        pose.rotateAround(new Quaternionf().rotateZ((float)radian), 0, 0, 0);
        FGuis.drawRing(graphics,
                0,
                0,
                WHEEL_INNER_RADIUS - 4,
                WHEEL_INNER_RADIUS - 2,
                -halfSliceSize,
                halfSliceSize,
                ColorHelper.setAlpha(ColorHelper.CYAN, p));
        pose.popPose();

        // 选择选项的圆环
        pose.pushPose();
        pose.rotateAround(new Quaternionf().rotateZ((float)Math.toRadians(degrees.get(selectedIndex))), 0, 0, 0);
        FGuis.drawRing(graphics,
                0,
                0,
                WHEEL_INNER_RADIUS,
                WHEEL_OUTER_RADIUS,
                -halfSliceSize,
                halfSliceSize,
                ColorHelper.setAlpha(ColorHelper.CYAN, 0.25F * p));
        pose.popPose();

        // 渲染选项
        if (size == positions.size())
            for (int i = 0; i < size; i++) {
                visibleSelections.get(i).setPosition(positions.get(i).getX(), positions.get(i).getY());
                visibleSelections.get(i).renderSelection(graphics, mouseX, mouseY, partialTicks);
            }

        // 渲染选项标题
        if (hovered != null) {
            CGuiHelper.drawCenteredStrings(graphics, font, font.split(hovered.getMessage(), 60), 0, -4, hovered.color, 10, true ,true);
        }
        pose.popPose();
    }

    public static int findBlockIndex(double degrees, int size) {
        float sliceSize = 360F / size;
        for (int i = 1; i <= size; i++) {
            if (degrees <= sliceSize * i) {
                return Math.max(i - 1, 0);
            }
        }
        return 0;
    }

    @Override
    protected void init() {
        // 在此处添加轮盘选项
        addSelection(new Selection(Component.literal("Test0"), FHItems.debug_item.get().getDefaultInstance(), s -> Popup.put("Test")));
        addSelection(new Selection(Component.literal("Test1"), IconButton.Icon.LEAVE, ColorHelper.CYAN, Selection.ALWAYS_VISIBLE, Selection.NO_ACTION, s -> Popup.put("Test1 Hovered")));
        addSelection(new Selection(Component.literal("Test2"), FHItems.debug_item.get().getDefaultInstance(), Selection.NO_ACTION) {
            @Override
            void renderWhenHovered(@NotNull GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
                graphics.drawString(font, getMessage(), getX()+20, getY(), color);
            }
        });
        addSelection(new Selection(Component.literal("Nutrition"), NutritionScreen.fat_icon, s -> FHNetwork.sendToServer(new C2SOpenNutritionScreenMessage())));
        addSelection(new Selection(Component.literal("Clothes"), FHItems.straw_lining.get().getDefaultInstance(), s -> FHNetwork.sendToServer(new C2SOpenClothesScreenMessage())));
        tick();
    }

    private void update() {
        positions.clear();
        int size = visibleSelections.size();
        double averageRadius = (WHEEL_INNER_RADIUS + WHEEL_OUTER_RADIUS) / 2.0;
        double angleStep = 2 * Math.PI / size;
        for (int i = 0; i < size; i++) {
            double theta = Math.PI / 2 - i * angleStep;
            double x = averageRadius * Math.cos(theta);
            double y = averageRadius * Math.sin(theta);
            positions.add(new Point((int)x, (int)-y));
        }

        degrees.clear();
        float sliceSize = 360F / size;
        for (int i = 0; i < size; i++) {
            float angle = i * sliceSize;
            degrees.add(angle);
        }
    }

    @Override
    public void tick() {
        for (Selection selection : selections) {
            selection.tick();
            if (!selection.visible) {
                visibleSelections.remove(selection);
                update();
            } else if (!visibleSelections.contains(selection)) {
                visibleSelections.add(selection);
                update();
            }
        }
    }

    @Override
    public void onClose() {
        if (hovered != null) {
            hovered.onPress();
        }
        hovered = null;
        lastHovered = null;
        showing = false;
        AnimationUtil.remove(OPEN_ANIM_NAME);
        super.onClose();
    }

    private void addSelection(Selection selection) {
        selections.add(selection);
    }

    @Override
    public boolean keyReleased(int pKeyCode, int pScanCode, int pModifiers) {
        boolean flag = super.keyReleased(pKeyCode, pScanCode, pModifiers);
        if (flag) return true;

        if (pKeyCode == FHKeyMappings.key_openWheelMenu.get().getKey().getValue()) {
            onClose();
            return true;
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private class Selection extends Button {
        static final Predicate<Selection> ALWAYS_VISIBLE = s -> true;
        static final OnPress NO_ACTION = s -> {};

        int color;
        boolean visible;
        final IconType iconType;
        final Object icon;
        final Predicate<Selection> visibility;
        final OnPress hoverAction;

        /**
         * @param icon {@link ItemStack}, {@link IconButton.Icon}, {@link Component}, {@code null}
         */
        Selection(Component message, Object icon, OnPress pressAction) {
            this(message, icon, ColorHelper.CYAN, ALWAYS_VISIBLE, pressAction, NO_ACTION);
        }

        /**
         * @param icon {@link ItemStack}, {@link IconButton.Icon}, {@link Component}, {@code null}
         */
        Selection(Component message, Object icon, int color, Predicate<Selection> visibility, OnPress pressAction, OnPress hoverAction) {
            super(0, 0, 0, 0, message, pressAction, Button.DEFAULT_NARRATION);
            this.hoverAction = hoverAction;
            this.iconType = getIconType(icon);
            this.icon = icon;
            this.color = color;
            this.visibility = visibility;
        }

        void renderSelection(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (!visible) return;
            renderWidget(graphics, mouseX, mouseY, partialTick);
            if (isHovered) {
                renderWhenHovered(graphics, mouseX, mouseY, partialTick);
            }
        }

        void renderWhenHovered(@NotNull GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        }

        @Override
        protected void renderWidget(@NotNull GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
            switch (iconType) {
                case ITEM -> graphics.renderItem((ItemStack)icon, getX()-8, getY()-8);
                case ICON -> {
                    var flatIcon = (IconButton.Icon)icon;
                    int x = getX() - flatIcon.size.width/2;
                    int y = getY() - flatIcon.size.height/2;
                    flatIcon.render(graphics.pose(), x, y, color);
                }
                case COMPONENT -> {
                    int width = font.width((Component)icon) / 2;
                    graphics.drawString(font, (Component)icon, getX()-width, getY()-4, color);
                }
            }
        }

        void tick() {
            isHovered = hovered == this;
            visible = visibility.test(this);
        }

        static IconType getIconType(Object icon) {
            if (icon instanceof ItemStack) return IconType.ITEM;
            if (icon instanceof IconButton.Icon) return IconType.ICON;
            if (icon instanceof Component) return IconType.COMPONENT;
            return IconType.EMPTY;
        }

        enum IconType {
            EMPTY,
            ITEM,
            ICON,
            COMPONENT,

        }
    }
}
