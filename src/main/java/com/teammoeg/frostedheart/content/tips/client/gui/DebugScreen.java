package com.teammoeg.frostedheart.content.tips.client.gui;

import com.teammoeg.frostedheart.content.tips.TipDisplayManager;
import com.teammoeg.frostedheart.content.tips.TipLockManager;
import com.teammoeg.frostedheart.content.tips.client.gui.widget.IconButton;
import com.teammoeg.frostedheart.content.waypoint.ClientWaypointManager;
import com.teammoeg.frostedheart.content.waypoint.waypoints.ColumbiatWaypoint;
import com.teammoeg.frostedheart.content.waypoint.waypoints.SunStationWaypoint;
import com.teammoeg.frostedheart.content.waypoint.waypoints.Waypoint;
import com.teammoeg.frostedheart.util.lang.Lang;
import com.teammoeg.frostedheart.util.client.ClientUtils;
import com.teammoeg.frostedheart.util.client.FHColorHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Random;
import java.util.UUID;

public class DebugScreen extends Screen {
    public DebugScreen() {
        super(Lang.str(""));
    }

    @Override
    public void init() {
        addButton(IconButton.Icon.TRASH_CAN, FHColorHelper.CYAN, "Clear Tip Cache", (b) -> {
            TipDisplayManager.clearCache();
        });
        addButton(IconButton.Icon.CROSS, FHColorHelper.CYAN, "Clear Tip Render Queue", (b) -> {
            TipDisplayManager.clearRenderQueue();
        });
        addButton(IconButton.Icon.HISTORY, FHColorHelper.RED, "Reset Lock State For All Tips", (b) -> {
            TipLockManager.manager.createFile();
        });
        addButton(IconButton.Icon.BOX_ON, FHColorHelper.CYAN, "Create a Random Waypoint", (b) -> {
            Random random = new Random();
            String uuid = UUID.randomUUID().toString();
            Waypoint waypoint = new Waypoint(new Vec3((random.nextFloat()-0.5F)*1280, Math.abs(random.nextFloat())*256, (random.nextFloat()-0.5F)*1280), uuid, FHColorHelper.setAlpha(random.nextInt(), 1F));
            waypoint.focus = random.nextBoolean();
            ClientWaypointManager.putWaypoint(waypoint);
        });
        addButton(IconButton.Icon.BOX_ON, 0xFFFFDA64, "Create Sun Station Waypoint", (b) -> {
            ClientWaypointManager.putWaypoint(new SunStationWaypoint());
        });
        addButton(IconButton.Icon.BOX_ON, 0xF6F1D5, "Create Columbiat Waypoint", (b) -> {
            ClientWaypointManager.putWaypoint(new ColumbiatWaypoint());
        });
        addButton(IconButton.Icon.BOX, FHColorHelper.CYAN, "Remove The Waypoint You Are Looking At", (b) -> {
            if (ClientWaypointManager.hoveredWaypoint != null) {
                ClientWaypointManager.removeWaypoint(ClientWaypointManager.hoveredWaypoint.getID());
            }
        });
        addButton(IconButton.Icon.SIGHT, FHColorHelper.CYAN, "Create a Waypoint From The Block You Are Looking At", (b) -> {
            HitResult block = ClientUtils.getPlayer().pick(128, ClientUtils.partialTicks(), false);
            if (block.getType() == HitResult.Type.BLOCK) {
                Waypoint waypoint = new Waypoint(((BlockHitResult)block).getBlockPos(), "picked_block", FHColorHelper.CYAN);
                waypoint.focus = true;
                waypoint.displayName = ClientUtils.getWorld().getBlockState(((BlockHitResult)block).getBlockPos()).getBlock().getName();
                ClientWaypointManager.putWaypoint(waypoint);
            }
        });
    }

    public void addButton(IconButton.Icon icon, int color, String message, Button.OnPress onPress) {
        IconButton button = new IconButton(0, 0, icon, color, Component.literal(message), onPress);
        this.addRenderableWidget(button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int size = this.renderables.size();
        int centerX = ClientUtils.screenWidth() / 2;
        int centerY = ClientUtils.screenHeight() / 2;

        graphics.fill(centerX-((size+(size%2))/2*16)+4, centerY-36, centerX+(size/2*16)+14, centerY-14, 0x80000000);
        for (int i = 0; i < size; i++) {
            IconButton button = (IconButton)this.renderables.get(i);
            if (button instanceof IconButton) {
                if (i == 0) {
                    button.setXY(centerX-5, centerY-30);
                } else if (i % 2 == 0) {
                    button.setXY(centerX-5-(i*8), centerY-30);
                } else {
                    button.setXY(centerX+5+(i*8), centerY-30);
                }
                button.render(graphics, mouseX, mouseY, partialTicks);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
