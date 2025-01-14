package com.teammoeg.frostedheart.content.tips.client.gui;

import com.teammoeg.frostedheart.FrostedHud;
import com.teammoeg.frostedheart.base.client.gui.widget.ColorEditbox;
import com.teammoeg.frostedheart.content.tips.Tip;
import com.teammoeg.frostedheart.content.tips.TipManager;
import com.teammoeg.frostedheart.base.client.gui.widget.IconButton;
import com.teammoeg.frostedheart.content.waypoint.ClientWaypointManager;
import com.teammoeg.frostedheart.content.waypoint.waypoints.ColumbiatWaypoint;
import com.teammoeg.frostedheart.content.waypoint.waypoints.SunStationWaypoint;
import com.teammoeg.frostedheart.content.waypoint.waypoints.Waypoint;
import com.teammoeg.frostedheart.util.client.ClientUtils;
import com.teammoeg.frostedheart.util.client.FHColorHelper;
import com.teammoeg.frostedheart.util.lang.Lang;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
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

    @Override
    public void init() {
        super.init();
        buttons.clear();

        addButton(IconButton.Icon.CROSS, FHColorHelper.CYAN, "Clear Tip Render Queue", (b) ->
            TipManager.INSTANCE.display().clearRenderQueue()
        );
        addButton(IconButton.Icon.HISTORY, FHColorHelper.RED, "Reset State For All Tips", (b) ->
            TipManager.INSTANCE.state().resetAll()
        );
        addButton(IconButton.Icon.HISTORY, FHColorHelper.CYAN, "Reload All Tips", (b) ->
            TipManager.INSTANCE.loadFromFile()
        );
        addButton(IconButton.Icon.WRENCH, FHColorHelper.CYAN, "Open Tip Editor UI", (b) ->
            ClientUtils.mc().setScreen(new TipEditorScreen())
        );
        addButton(IconButton.Icon.BOX_ON, FHColorHelper.CYAN, "Create a Random Waypoint", (b) -> {
            Random random = new Random();
            String uuid = UUID.randomUUID().toString();
            Waypoint waypoint = new Waypoint(new Vec3((random.nextFloat()-0.5F)*1280, Math.abs(random.nextFloat())*256, (random.nextFloat()-0.5F)*1280), uuid, FHColorHelper.setAlpha(random.nextInt(), 1F));
            waypoint.focus = random.nextBoolean();
            ClientWaypointManager.putWaypoint(waypoint);
        });
        addButton(IconButton.Icon.BOX_ON, 0xFFFFDA64, "Create Sun Station Waypoint", (b) ->
            ClientWaypointManager.putWaypoint(new SunStationWaypoint())
        );
        addButton(IconButton.Icon.BOX_ON, 0xFFF6F1D5, "Create Columbiat Waypoint", (b) ->
            ClientWaypointManager.putWaypoint(new ColumbiatWaypoint())
        );
        addButton(IconButton.Icon.BOX, FHColorHelper.RED, "Remove The Waypoint You Are Looking At", (b) ->
            ClientWaypointManager.getHovered().ifPresent((hovered) -> ClientWaypointManager.removeWaypoint(hovered.getId()))
        );
        addButton(IconButton.Icon.SIGHT, FHColorHelper.CYAN, "Create a Waypoint From The Block You Are Looking At", (b) -> {
            HitResult block = ClientUtils.getPlayer().pick(128, ClientUtils.partialTicks(), false);
            if (block.getType() == HitResult.Type.BLOCK) {
                Waypoint waypoint = new Waypoint(((BlockHitResult)block).getBlockPos(), "picked_block", FHColorHelper.CYAN);
                waypoint.focus = true;
                waypoint.displayName = ClientUtils.getWorld().getBlockState(((BlockHitResult)block).getBlockPos()).getBlock().getName();
                ClientWaypointManager.putWaypoint(waypoint);
            }
        });
        addButton(IconButton.Icon.TRADE, FHColorHelper.CYAN, "Toggle Debug Overlay", (b) ->
            FrostedHud.renderDebugOverlay = !FrostedHud.renderDebugOverlay
        );
        addButton(IconButton.Icon.LEAVE, FHColorHelper.CYAN, "Do Something", (b) -> {
            String message = debug();
            ClientUtils.getPlayer().sendSystemMessage(Lang.str(message));
        });
        var colorPicker = new ColorEditbox(font, 50, 50, 64, 20, Lang.str(""),false, FHColorHelper.CYAN);
        addRenderableWidget(colorPicker);
//        addRenderableWidget(new TextLabelWidget(10, 10, 50, 50, Component.literal("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"), ClientUtils.font()));
    }

    // 方便热重载debug
    private String debug() {
        buttons.get(0).setScale(2);
        Tip tip = Tip.builder("test").line(Lang.str("test")).line(Lang.str("aaaaaaaaaaaaa")).nextTip("default").build();
        TipManager.INSTANCE.display().general(tip);
        return tip.getNextTip();
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
