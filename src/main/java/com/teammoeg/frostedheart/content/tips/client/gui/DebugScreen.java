package com.teammoeg.frostedheart.content.tips.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.content.tips.TipDisplayManager;
import com.teammoeg.frostedheart.content.tips.TipLockManager;
import com.teammoeg.frostedheart.content.tips.client.gui.widget.IconButton;
import com.teammoeg.frostedheart.util.TranslateUtils;
import com.teammoeg.frostedheart.util.client.FHColorHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;

public class DebugScreen extends Screen {
    public DebugScreen() {
        super(new StringTextComponent(""));
    }

    @Override
    public void init() {
//        Entity entity = Minecraft.getInstance().pointedEntity;
//        if (entity != null) {
//            EntityWaypoint w = new EntityWaypoint(entity, FHColorHelper.CYAN, FHColorHelper.RED);
//            WaypointManager.putWaypoint(w);
//        }

        this.addButton(new IconButton((int) (this.width*0.5-65), (int) (this.height*0.4), IconButton.ICON_TRASH_CAN, FHColorHelper.CYAN, TranslateUtils.translateGui("debug.clear_cache"), (b) -> {
            TipDisplayManager.clearCache();
        }));
        this.addButton(new IconButton((int) (this.width*0.5-45), (int) (this.height*0.4), IconButton.ICON_CROSS, FHColorHelper.CYAN, TranslateUtils.translateGui("debug.clear_queue"), (b) -> {
            TipDisplayManager.clearRenderQueue();
        }));
        this.addButton(new IconButton((int) (this.width*0.5-25), (int) (this.height*0.4), IconButton.ICON_HISTORY, FHColorHelper.RED, TranslateUtils.translateGui("debug.reset_unlock"), (b) -> {
            TipLockManager.manager.createFile();
        }));
//        this.addButton(new IconButton((int) (this.width*0.5-5), (int) (this.height*0.4), IconButton.ICON_BOX_ON, FHColorHelper.CYAN, TranslateUtils.translateGui("debug.random_waypoint"), (b) -> {
//            Random random = new Random();
//            String uuid = UUID.randomUUID().toString();
//            waypoint waypoint = new waypoint(new Vector3f((random.nextFloat()-0.5F)*256, (random.nextFloat()-0.5F)*128+128, (random.nextFloat()-0.5F)*256), random.nextInt(), uuid);
//            WaypointManager.getManager()
//        }));
//        this.addButton(new IconButton((int) (this.width*0.5+15), (int) (this.height*0.4), IconButton.ICON_BOX, FHColorHelper.RED, TranslateUtils.translateGui("debug.remove_all_waypoint"), (b) -> {
//            WaypointManager.removeAll();
//        }));
//        this.addButton(new IconButton((int) (this.width*0.5+35), (int) (this.height*0.4), IconButton.ICON_BOX_ON, 0xFFFFDA64, TranslateUtils.translateWaypoint("sun_station"), (b) -> {
//            WaypointManager.putWaypoint(new SunStationWaypoint());
//        }));
//        this.addButton(new IconButton((int) (this.width*0.5+55), (int) (this.height*0.4), IconButton.ICON_SIGHT, FHColorHelper.CYAN, TranslateUtils.translateGui("debug.entity_target"), (b) -> {
//            Entity entity2 = Minecraft.getInstance().pointedEntity;
//            if (entity2 != null) {
//                EntityWaypoint w = new EntityWaypoint(entity2, FHColorHelper.CYAN, FHColorHelper.RED);
//                WaypointManager.putWaypoint(w);
//            }
//        }));
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
        fill(ms, (int) (this.width*0.5-50), (int) (this.height*0.4-5), (int) (this.width*0.5+50), (int) (this.height*0.4+15), 0x80000000);
        super.render(ms, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
