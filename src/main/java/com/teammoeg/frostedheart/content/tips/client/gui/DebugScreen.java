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

package com.teammoeg.frostedheart.content.tips.client.gui;

import com.simibubi.create.foundation.config.ui.BaseConfigScreen;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.MouseHelper;
import com.teammoeg.chorda.client.icon.FlatIcon;
import com.teammoeg.chorda.client.ui.Colors;
import com.teammoeg.chorda.client.widget.IconButton;
import com.teammoeg.chorda.text.Components;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FrostedHud;
import com.teammoeg.frostedheart.content.tips.Popup;
import com.teammoeg.frostedheart.content.tips.TipManager;
import com.teammoeg.frostedheart.content.waypoint.ClientWaypointManager;
import com.teammoeg.frostedheart.content.waypoint.waypoints.ColumbiatWaypoint;
import com.teammoeg.frostedheart.content.waypoint.waypoints.SunStationWaypoint;
import com.teammoeg.frostedheart.content.waypoint.waypoints.Waypoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.DebugEntityNameGenerator;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class DebugScreen extends Screen {
    public List<IconButton> buttons = new ArrayList<>();
    public static String message = "";

    public DebugScreen() {
        super(Component.literal(""));
    }

    public static void openDebugScreen() {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().setScreen(new DebugScreen());
        }
    }

    @Override
    public void init() {
        super.init();
        buttons.clear();
        var input = addRenderableWidget(new EditBox(ClientUtils.font(), ClientUtils.screenCenterX() - 60, ClientUtils.screenCenterY() + 20, 120, 12, Component.literal("input")));
        input.setMaxLength(1024);

        addButton(FlatIcon.CROSS, Colors.themeColor(), "Clear Tip Render Queue", (b) ->
            TipManager.INSTANCE.display().clearRenderQueue()
        );
        addButton(FlatIcon.HISTORY, Colors.RED, "Reset State For All Tips", (b) ->
            TipManager.INSTANCE.state().resetAll()
        );
        addButton(FlatIcon.HISTORY, Colors.themeColor(), "Reload All Tips", (b) ->
            TipManager.INSTANCE.loadFromFile()
        );
        addButton(FlatIcon.WRENCH, Colors.themeColor(), "Open Tip Editor UI", (b) ->
            ClientUtils.getMc().setScreen(new TipEditorScreen())
        );
        addButton(FlatIcon.BOX_ON, Colors.themeColor(), "Create a Random Waypoint", (b) -> {
            Random random = new Random();
            String id = DebugEntityNameGenerator.getEntityName(UUID.randomUUID());
            Waypoint waypoint = new Waypoint(new Vec3((random.nextFloat()-0.5F)*1280, Math.abs(random.nextFloat())*256, (random.nextFloat()-0.5F)*1280), id, Colors.setAlpha(random.nextInt(), 1F));
            waypoint.setFocused(random.nextBoolean());
            ClientWaypointManager.putWaypoint(waypoint);
        });
        addButton(FlatIcon.BOX_ON, 0xFFFFDA64, "Create Sun Station Waypoint", (b) ->
            ClientWaypointManager.putWaypoint(new SunStationWaypoint())
        );
        addButton(FlatIcon.BOX_ON, 0xFFF6F1D5, "Create Columbiat Waypoint", (b) ->
            ClientWaypointManager.putWaypoint(new ColumbiatWaypoint())
        );
        addButton(FlatIcon.BOX, Colors.RED, "Remove The Waypoint You Are Looking At", (b) ->
            ClientWaypointManager.getSelected().ifPresent((hovered) -> ClientWaypointManager.removeWaypoint(hovered.getId()))
        );
        addButton(FlatIcon.SIGHT, Colors.themeColor(), "Create a Waypoint From The Block You Are Looking At", (b) -> {
            ClientWaypointManager.fromPickedBlock();
        });
        addButton(FlatIcon.TRADE, Colors.themeColor(), "Toggle Debug Overlay", (b) ->
            FrostedHud.renderDebugOverlay = !FrostedHud.renderDebugOverlay
        );
        addButton(FlatIcon.LIST, Colors.themeColor(), "Create Pop-up message", (b) ->
            Popup.put(input.getValue())
        );
        addButton(FlatIcon.LIST, Colors.themeColor(), "Unlock All Tips", (b) ->
            TipManager.INSTANCE.state().unlockAll()
        );
        addButton(FlatIcon.LEAVE, Colors.themeColor(), "Do Something", (b) -> {
            String message = debug();
            ClientUtils.getPlayer().sendSystemMessage(Components.str(message));
        });
    }

    // 方便热重载debug
    private String debug() {
        if (this.minecraft != null) {
            var config = new BaseConfigScreen(this, FHMain.MODID);
            this.minecraft.setScreen(config);
        }
        Popup.clear();
        return "opened";
    }

    public void addButton(FlatIcon icon, int color, String message, Button.OnPress onPress) {
        IconButton button = new IconButton(0, 0, icon, color, Component.literal(message), onPress);
        buttons.add(button);
        this.addRenderableWidget(button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int size = this.buttons.size();
        int centerX = ClientUtils.screenCenterX();
        int centerY = ClientUtils.screenCenterY();
        var font = ClientUtils.font();

        graphics.fill(centerX-((size+(size%2))/2*16)+4, centerY-36, centerX+(size/2*16)+14, centerY-14, 0x80000000);
        for (int i = 0; i < size; i++) {
            IconButton button = this.buttons.get(i);
            if (i == 0) {
                button.setPosition(centerX-4, centerY-30);
            } else if (i % 2 == 0) {
                button.setPosition(centerX-5-(i*8), centerY-30);
            } else {
                button.setPosition(centerX+5+(i*8), centerY-30);
            }
            button.render(graphics, mouseX, mouseY, partialTicks);
        }
        buttons.get(size-1).render(graphics, mouseX, mouseY, partialTicks);

        if (children().get(0) instanceof EditBox editBox) {
            try {
                String s = "|";
//                int charW = font.width(s);
                int charW = 1;
                int input = Math.min(Integer.parseInt(editBox.getValue()), ClientUtils.screenWidth()/charW);
                var text = Component.empty();
                for (int i = 0; i < input; i++) {
                    int color = Color.HSBtoRGB(i / (float)input, 1, 1);
                    int x = centerX - (input*charW)/2;
//                    graphics.fillGradient(x+(i*charW), centerY+40, x+(i*charW)+charW, centerY+40+(input*charW), color, Colors.BLACK);
                    graphics.fill(x+(i*charW), centerY+40, x+(i*charW)+charW, centerY+49, color);
                    var c = Integer.toHexString(color).toUpperCase();
                    var style = Style.EMPTY
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Components.withColor(Component.literal(c), color)))
                            .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, c));
                    text.append(Components.withColor(Components.str(s), color).withStyle(style));
                }
//                graphics.drawCenteredString(font, text, centerX, centerY+40, -1);

//                int textW = font.width(text);
                int textW = input*charW;
                if (MouseHelper.isMouseIn(mouseX, mouseY, centerX-textW/2, centerY+40, textW, font.lineHeight)) {
                    int x = mouseX - (centerX-textW/2);
                    hoveredStyle = text.getSiblings().get(x/charW).getStyle();
                } else {
                    hoveredStyle = null;
                }
                graphics.renderComponentHoverEffect(font, hoveredStyle, mouseX, mouseY);
            } catch (Exception ignored) {}
        }

        var others = new ArrayList<>(this.renderables);
        others.removeAll(buttons);
        for(Renderable renderable : others) {
            renderable.render(graphics, mouseX, mouseY, partialTicks);
        }
    }

    Style hoveredStyle = null;
    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (hoveredStyle != null) {
            handleComponentClicked(hoveredStyle);
            return true;
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
