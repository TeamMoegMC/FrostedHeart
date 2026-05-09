package com.teammoeg.frostedheart.compat.create;

import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.frostedheart.compat.create.IHaveOutlines.ShapeInfo;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;

import java.util.HashMap;
import java.util.Map;

public class GoggleInfoOutlineHandler {
    static Map<String, VisibleShape> shapes = new HashMap<>();

    public static void tick() {
        var mc = ClientUtils.getMc();
        var result = mc.hitResult;
        var player = mc.player;
        var level = mc.level;
        if (level == null || player == null || mc.options.hideGui || mc.gameMode.getPlayerMode() == GameType.SPECTATOR || !GogglesItem.isWearingGoggles(mc.player))
            return;

        // 更新当前注视方块提供的形状
        if (result instanceof BlockHitResult bhr) {
            var pos = bhr.getBlockPos();
            var be = level.getBlockEntity(pos);
            if (be instanceof IHaveOutlines info) {
                for (ShapeInfo vs : info.getOutlineShapes()) {
                    shapes.put(vs.name(), new VisibleShape(vs));
                }
            }
        }

        // 更新渲染及倒计时
        var allOutlines = CreateClient.OUTLINER.getOutlines();
        var iter = shapes.entrySet().iterator();
        while (iter.hasNext()) {
            var shape = iter.next().getValue();
            var info = shape.info;
            var bound = info.shape();

            if (allOutlines.containsKey(info.name())) {
                CreateClient.OUTLINER.chaseAABB(info.name(), bound);
            } else {
                CreateClient.OUTLINER.showAABB(info.name(), bound)
                        .colored(info.color())
                        .withFaceTexture(AllSpecialTextures.CHECKERED)
                        .lineWidth(1 / 16F)
                        .disableCull();
            }

            shape.tick();
            if (shape.remainTick <= 0) {
                iter.remove();
                // CreateClient.OUTLINER.remove(info.name());
            }
        }
    }

    static class VisibleShape {
        ShapeInfo info;
        int remainTick;

        public VisibleShape(ShapeInfo info) {
            this.info = info;
            this.remainTick = info.displayTick();
        }

        public void tick() {
            remainTick--;
        }
    }
}
