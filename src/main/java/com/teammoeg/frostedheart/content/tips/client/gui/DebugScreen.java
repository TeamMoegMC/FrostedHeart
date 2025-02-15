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

package com.teammoeg.frostedheart.content.tips.client.gui;

import com.teammoeg.frostedheart.FrostedHud;
import com.teammoeg.frostedheart.content.tips.Popup;
import com.teammoeg.frostedheart.content.tips.TipManager;
import com.teammoeg.frostedheart.content.waypoint.ClientWaypointManager;
import com.teammoeg.frostedheart.content.waypoint.waypoints.ColumbiatWaypoint;
import com.teammoeg.frostedheart.content.waypoint.waypoints.SunStationWaypoint;
import com.teammoeg.frostedheart.content.waypoint.waypoints.Waypoint;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.ui.ColorHelper;
import com.teammoeg.chorda.client.widget.IconButton;
import com.teammoeg.chorda.lang.Components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class DebugScreen extends Screen {
    public List<IconButton> buttons = new ArrayList<>();

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
        var input = addRenderableWidget(new EditBox(ClientUtils.font(), 10, 50, 80, 12, Component.literal("input")));
        input.setMaxLength(1024);

        addButton(IconButton.Icon.CROSS, ColorHelper.CYAN, "Clear Tip Render Queue", (b) ->
            TipManager.INSTANCE.display().clearRenderQueue()
        );
        addButton(IconButton.Icon.HISTORY, ColorHelper.RED, "Reset State For All Tips", (b) ->
            TipManager.INSTANCE.state().resetAll()
        );
        addButton(IconButton.Icon.HISTORY, ColorHelper.CYAN, "Reload All Tips", (b) ->
            TipManager.INSTANCE.loadFromFile()
        );
        addButton(IconButton.Icon.WRENCH, ColorHelper.CYAN, "Open Tip Editor UI", (b) ->
            ClientUtils.mc().setScreen(new TipEditorScreen())
        );
        addButton(IconButton.Icon.BOX_ON, ColorHelper.CYAN, "Create a Random Waypoint", (b) -> {
            Random random = new Random();
            String uuid = UUID.randomUUID().toString();
            Waypoint waypoint = new Waypoint(new Vec3((random.nextFloat()-0.5F)*1280, Math.abs(random.nextFloat())*256, (random.nextFloat()-0.5F)*1280), uuid, ColorHelper.setAlpha(random.nextInt(), 1F));
            waypoint.focus = random.nextBoolean();
            ClientWaypointManager.putWaypoint(waypoint);
        });
        addButton(IconButton.Icon.BOX_ON, 0xFFFFDA64, "Create Sun Station Waypoint", (b) ->
            ClientWaypointManager.putWaypoint(new SunStationWaypoint())
        );
        addButton(IconButton.Icon.BOX_ON, 0xFFF6F1D5, "Create Columbiat Waypoint", (b) ->
            ClientWaypointManager.putWaypoint(new ColumbiatWaypoint())
        );
        addButton(IconButton.Icon.BOX, ColorHelper.RED, "Remove The Waypoint You Are Looking At", (b) ->
            ClientWaypointManager.getHovered().ifPresent((hovered) -> ClientWaypointManager.removeWaypoint(hovered.getId()))
        );
        addButton(IconButton.Icon.SIGHT, ColorHelper.CYAN, "Create a Waypoint From The Block You Are Looking At", (b) -> {
            HitResult block = ClientUtils.getPlayer().pick(128, ClientUtils.partialTicks(), false);
            if (block.getType() == HitResult.Type.BLOCK) {
                Waypoint waypoint = new Waypoint(((BlockHitResult)block).getBlockPos(), "picked_block", ColorHelper.CYAN);
                waypoint.focus = true;
                waypoint.displayName = ClientUtils.getWorld().getBlockState(((BlockHitResult)block).getBlockPos()).getBlock().getName();
                ClientWaypointManager.putWaypoint(waypoint);
            }
        });
        addButton(IconButton.Icon.TRADE, ColorHelper.CYAN, "Toggle Debug Overlay", (b) ->
            FrostedHud.renderDebugOverlay = !FrostedHud.renderDebugOverlay
        );
        addButton(IconButton.Icon.LIST, ColorHelper.CYAN, "Create Pop-up message", (b) ->
            Popup.put(input.getValue())
        );
        addButton(IconButton.Icon.LEAVE, ColorHelper.CYAN, "Do Something", (b) -> {
            String message = debug();
            ClientUtils.getPlayer().sendSystemMessage(Components.str(message));
        });
    }

    // 方便热重载debug
    private String debug() {
//        if (this.minecraft != null) {
////            var config = new BaseConfigScreen(this, FHMain.MODID);
//            this.minecraft.setScreen(new WheelMenuRenderer());
//        }
        Popup.clear();
        return "opened";
    }

    public void addButton(IconButton.Icon icon, int color, String message, Button.OnPress onPress) {
        IconButton button = new IconButton(0, 0, icon, color, Component.literal(message), onPress);
        buttons.add(button);
        this.addRenderableWidget(button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int size = this.buttons.size();
        int centerX = ClientUtils.screenWidth() / 2;
        int centerY = ClientUtils.screenHeight() / 2;

        graphics.fill(centerX-((size+(size%2))/2*16)+4, centerY-36, centerX+(size/2*16)+14, centerY-14, 0x80000000);
        for (int i = 0; i < size; i++) {
            IconButton button = this.buttons.get(i);
            if (i == 0) {
                button.setPosition(centerX-5, centerY-30);
            } else if (i % 2 == 0) {
                button.setPosition(centerX-5-(i*8), centerY-30);
            } else {
                button.setPosition(centerX+5+(i*8), centerY-30);
            }
            button.render(graphics, mouseX, mouseY, partialTicks);
        }
        buttons.get(size-1).render(graphics, mouseX, mouseY, partialTicks);


        var others = new ArrayList<>(this.renderables);
        others.removeAll(buttons);
        for(Renderable renderable : others) {
            renderable.render(graphics, mouseX, mouseY, partialTicks);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
